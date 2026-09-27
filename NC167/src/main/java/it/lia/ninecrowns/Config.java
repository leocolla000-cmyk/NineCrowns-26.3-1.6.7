package it.lia.ninecrowns;

import com.google.gson.GsonBuilder;
import java.nio.file.*;

public final class Config {
    // Fixed gameplay values for Nine Crowns 1.6.4.
    // OP Sword: 14 total raw damage with Sharpness V.
    // Vanilla Netherite Sword + Sharpness V = 11, therefore +3 damage = +1.5 hearts.
    public double swordDamage = 14;

    // OP Spear jab: 10 total raw damage WITH Sharpness V = 5 hearts before armor/protection.
    public double spearMeleeDamage = 10;
    public int spearCooldownTicks = 20;

    public double poisonChance = .20;
    public double slownessChance = .10;
    public int effectTicks = 60;

    // One lightning attack every 30 seconds.
    // Several bolts are visual only; damage is applied ONCE as normal player damage.
    public int lightningRechargeSeconds = 30;
    public int lightningVisualBolts = 3;
    public double lightningRange = 24;
    public float lightningDamage = 12; // 6 hearts before armor/protection

    public static Config load(Path path) {
        try {
            var gson = new GsonBuilder().setPrettyPrinting().create();
            Config c = new Config();

            if (Files.exists(path)) {
                Config loaded = gson.fromJson(Files.readString(path), Config.class);
                if (loaded != null) c = loaded;
            }

            // Core balance values are intentionally enforced on every start.
            // This prevents an old config from a previous Nine Crowns version
            // (for example the old 2-charge spear) from silently changing 1.6.4.
            c.swordDamage = 14;
            c.spearMeleeDamage = 10;
            c.spearCooldownTicks = 20;
            c.poisonChance = .20;
            c.slownessChance = .10;
            c.effectTicks = 60;
            c.lightningRechargeSeconds = 30;
            c.lightningVisualBolts = 3;
            c.lightningRange = 24;
            c.lightningDamage = 12;

            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(c));
            return c;
        } catch (Exception e) {
            throw new IllegalStateException("Nine Crowns config error", e);
        }
    }
}
