package it.lia.ninecrowns;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraft.commands.Commands;

public final class NineCrowns implements ModInitializer {
    public static Config config;
    public static WorldData data;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("ninecrowns", path);
    }

    @Override
    public void onInitialize() {
        config = Config.load(FabricLoader.getInstance().getConfigDir().resolve("ninecrowns.json"));
        ModItems.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
            data = new WorldData(server.getWorldPath(LevelResource.ROOT).resolve("data/ninecrowns.json"))
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            server.getPlayerList().getPlayers().forEach(CrownEffects::clear);
            if (data != null) data.save();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> data = null);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            if (data == null) return;
            boolean member = data.roster.register(player.getUUID(), player.getGameProfile().name());
            data.save();
            player.sendSystemMessage(Component.literal(
                member
                    ? "Nine Crowns: partecipanti registrati " + data.roster.players().size() + "/9."
                    : "Nine Crowns: elenco completo; questo account non e un partecipante."
            ));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CrownEffects.clear(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server ->
            server.getPlayerList().getPlayers().forEach(player -> {
                CrownEffects.update(player);

                // Keeps special items in the required state, including Creative or /give copies.
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ModItems.applyRequiredProperties(player.getInventory().getItem(slot), player.registryAccess());
                }
                ModItems.applyRequiredProperties(player.getMainHandItem(), player.registryAccess());
                ModItems.applyRequiredProperties(player.getOffhandItem(), player.registryAccess());
                ModItems.applyRequiredProperties(player.getItemBySlot(EquipmentSlot.HEAD), player.registryAccess());
            })
        );

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && data != null && data.roster.contains(player.getUUID())) {
                var head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
                head.set(DataComponents.CUSTOM_NAME, Component.literal("Testa di " + player.getGameProfile().name()));
                player.spawnAtLocation(player.level(), head);
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damageTaken, blocked) -> {
            if (blocked || damageTaken <= 0) return;
            if (source.getDirectEntity() instanceof ServerPlayer attacker && attacker.getMainHandItem().is(ModItems.SWORD)) {
                var random = attacker.getRandom();
                if (random.nextFloat() < config.poisonChance) {
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, config.effectTicks, 0), attacker);
                }
                if (random.nextFloat() < config.slownessChance) {
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, config.effectTicks, 0), attacker);
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("ninecrowns")
                .executes(context -> {
                    if (data == null) {
                        context.getSource().sendFailure(Component.literal("Nine Crowns: dati non ancora caricati."));
                        return 0;
                    }
                    String names = data.roster.players().isEmpty()
                        ? "nessuno"
                        : String.join(", ", data.roster.players().values());
                    context.getSource().sendSuccess(
                        () -> Component.literal(
                            "Nine Crowns (" + data.roster.players().size() + "/9): " + names
                            + " | " + data.craftedStatus()
                        ),
                        false
                    );
                    return data.roster.players().size();
                })
                .then(Commands.literal("recipes").executes(context -> {
                    context.getSource().sendSuccess(
                        () -> Component.literal("Ricette leggendarie: 8 teste degli ALTRI giocatori attorno all'oggetto centrale."),
                        false
                    );
                    context.getSource().sendSuccess(
                        () -> Component.literal("Spada OP: 8 teste + Netherite Sword al centro."),
                        false
                    );
                    context.getSource().sendSuccess(
                        () -> Component.literal("Spear OP: 8 teste + Netherite Spear al centro."),
                        false
                    );
                    context.getSource().sendSuccess(
                        () -> Component.literal("Emperor Crown: 8 teste + Empty Crown al centro."),
                        false
                    );
                    context.getSource().sendSuccess(
                        () -> Component.literal("Ogni leggendario puo essere craftato UNA SOLA VOLTA nel mondo."),
                        false
                    );
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    if (data == null) {
                        context.getSource().sendFailure(Component.literal("Nine Crowns: dati non ancora caricati."));
                        return 0;
                    }
                    context.getSource().sendSuccess(
                        () -> Component.literal(data.craftedStatus()),
                        false
                    );
                    return 1;
                }));

            dispatcher.register(root);
        });
    }

    public static void lightning(ServerPlayer player) {
        if (data == null || player.isSpectator()) return;

        long now = player.level().getServer().overworld().getGameTime();
        long rechargeTicks = Math.max(1, config.lightningRechargeSeconds) * 20L;
        long remaining = data.lightningRemaining(now);

        if (remaining > 0) {
            player.sendSystemMessage(Component.literal(
                "Fulmini non pronti. Attendi " + ((remaining + 19) / 20) + " s."
            ));
            return;
        }

        HitResult hit = ProjectileUtil.getHitResultOnViewVector(
            player,
            entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator(),
            config.lightningRange
        );

        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            player.sendSystemMessage(Component.literal(
                "Mira a un bersaglio entro " + (int) config.lightningRange + " blocchi."
            ));
            return;
        }

        if (target instanceof ServerPlayer other && !player.canHarmPlayer(other)) return;

        if (!data.consumeLightning(now, rechargeTicks)) return;

        var level = player.level();

        // Several visual bolts, but ONE normal damage event so armor and Protection
        // reduce the total 12 damage normally instead of each tiny bolt separately.
        int boltCount = Math.max(1, config.lightningVisualBolts);
        double radius = 0.65;
        for (int i = 0; i < boltCount; i++) {
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt == null) continue;

            double angle = (Math.PI * 2.0 * i) / boltCount;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            bolt.setPos(x, target.getY(), z);
            bolt.setVisualOnly(true);
            bolt.setCause(player);
            level.addFreshEntity(bolt);
        }

        target.hurtServer(
            level,
            level.damageSources().playerAttack(player),
            config.lightningDamage
        );
    }
}
