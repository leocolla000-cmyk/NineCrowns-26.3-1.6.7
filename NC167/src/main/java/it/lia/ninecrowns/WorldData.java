package it.lia.ninecrowns;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.item.Item;

public final class WorldData {
    public final Roster roster = new Roster();

    private final Path path;

    private boolean swordCrafted;
    private boolean spearCrafted;
    private boolean crownCrafted;

    // There is only one craftable OP Spear, therefore the lightning cooldown
    // belongs to the legendary spear system globally, not to individual players.
    // Passing/stolen ownership cannot reset or bypass the 30-second cooldown.
    private long spearLightningNextUse;

    public WorldData(Path path) {
        this.path = path;
        if (!Files.exists(path)) return;

        try {
            var root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            // A save from a different roster size is intentionally not imported.
            // The first nine distinct players to join become the new registered roster.
            if (root.has("players")
                && root.has("rosterSize")
                && root.get("rosterSize").getAsInt() == Roster.SIZE) {
                var players = root.getAsJsonObject("players");
                for (var e : players.entrySet()) {
                    roster.register(UUID.fromString(e.getKey()), e.getValue().getAsString());
                }
            }

            if (root.has("spearLightningNextUse")) {
                spearLightningNextUse = Math.max(0L, root.get("spearLightningNextUse").getAsLong());
            }

            if (root.has("crafted") && root.get("crafted").isJsonObject()) {
                var crafted = root.getAsJsonObject("crafted");
                swordCrafted = crafted.has("sword") && crafted.get("sword").getAsBoolean();
                spearCrafted = crafted.has("spear") && crafted.get("spear").getAsBoolean();
                crownCrafted = crafted.has("crown") && crafted.get("crown").getAsBoolean();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nine Crowns world data error", e);
        }
    }

    public synchronized boolean isCrafted(Item item) {
        if (item == ModItems.SWORD) return swordCrafted;
        if (item == ModItems.SPEAR) return spearCrafted;
        if (item == ModItems.CROWN) return crownCrafted;
        return false;
    }

    /**
     * Atomically reserves the one allowed craft of a legendary item.
     * Returns false if another craft already claimed it.
     */
    public synchronized boolean tryMarkCrafted(Item item) {
        if (isCrafted(item)) return false;

        if (item == ModItems.SWORD) swordCrafted = true;
        else if (item == ModItems.SPEAR) spearCrafted = true;
        else if (item == ModItems.CROWN) crownCrafted = true;
        else return false;

        save();
        return true;
    }

    public synchronized String craftedStatus() {
        return "Spada=" + (swordCrafted ? "CRAFTATA" : "disponibile")
            + ", Lancia=" + (spearCrafted ? "CRAFTATA" : "disponibile")
            + ", Corona=" + (crownCrafted ? "CRAFTATA" : "disponibile");
    }

    public synchronized long lightningRemaining(long now) {
        return Math.max(0L, spearLightningNextUse - now);
    }

    /**
     * Atomically consumes the spear lightning use if ready.
     * Cooldown is global for the unique legendary spear.
     */
    public synchronized boolean consumeLightning(long now, long cooldownTicks) {
        if (now < spearLightningNextUse) return false;
        spearLightningNextUse = now + Math.max(1L, cooldownTicks);
        save();
        return true;
    }

    public synchronized void save() {
        try {
            var root = new JsonObject();
            var ps = new JsonObject();
            var crafted = new JsonObject();

            roster.players().forEach((id, name) -> ps.addProperty(id.toString(), name));

            crafted.addProperty("sword", swordCrafted);
            crafted.addProperty("spear", spearCrafted);
            crafted.addProperty("crown", crownCrafted);

            root.addProperty("rosterSize", Roster.SIZE);
            root.add("players", ps);
            root.add("crafted", crafted);
            root.addProperty("spearLightningNextUse", spearLightningNextUse);

            Files.createDirectories(path.getParent());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);

            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, json);

            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nine Crowns save error", e);
        }
    }
}
