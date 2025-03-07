package ml.peya.plugins.Detect;

import ml.peya.plugins.DetectClasses.*;
import ml.peya.plugins.Enum.*;
import ml.peya.plugins.*;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;
import org.bukkit.scheduler.*;

import java.util.*;

import static ml.peya.plugins.Utils.MessageEngine.get;
import static ml.peya.plugins.Variables.cheatMeta;
import static ml.peya.plugins.Variables.config;

/**
 * NPCのTeleportを管理する。
 */
public class NPCTeleport
{
    /**
     * テレポートォ！
     *
     * @param player    プレイヤー。
     * @param target    ターゲット。
     * @param arm       防具。
     * @param tpCase    テレポートケース。
     * @param reachMode リーチモードかどうか。
     */
    public static void teleport(Player player, EntityPlayer target, ItemStack[] arm, DetectType tpCase, boolean reachMode)
    {
        switch (tpCase)
        {
            case AURA_BOT:
                auraBotTeleport(player, target, arm, reachMode);
                break;
            case AURA_PANIC:
                auraPanic_teleport(player, target, arm, tpCase.getPanicCount(), tpCase.getSender(), reachMode);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + tpCase);
        }
    }

    /**
     * Aurapanicのテレポート。
     *
     * @param player    プレイヤー。
     * @param target    ターゲット。
     * @param arm       装備。
     * @param count     回数。
     * @param sender    イベントsender。
     * @param reachMode リーチモードかどうか。
     */
    private static void auraPanic_teleport(Player player, EntityPlayer target, ItemStack[] arm, int count, CommandSender sender, boolean reachMode)
    {
        final double[] time = {0.0};
        final double radius = reachMode ? config.getDouble("npc.reachPanicRange"): config.getDouble("npc.panicRange");

        final int[] now = {0};
        BukkitRunnable r = new BukkitRunnable()
        {
            public void run()
            {
                now[0]++;

                double yValue = generateWaveValue(1.0, 2.0, 0.0, 0.01, count[0] < 20);
                Location center = player.getLocation();
                Location n = new Location(
                    center.getWorld(),
                    center.getX(),
                    center.getY() + yValue,
                    center.getZ(),
                    (float) generateWaveValue(10.0, 100.0, 10.0, 0.01, false),
                    (float) generateWaveValue(10.0, 100.0, 10.0, 0.01, false)
                );

                NPC.setLocation(n, target);
                ((CraftPlayer) player).getHandle().playerConnection
                    .sendPacket(new PacketPlayOutEntityTeleport(target));

                NPC.setArmor(player, target, arm);
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        Bukkit.getOnlinePlayers()
                            .parallelStream()
                            .filter(p -> p.hasPermission("psac.viewnpc"))
                            .forEachOrdered(p ->
                            {
                                ((CraftPlayer) p).getHandle().playerConnection
                                    .sendPacket(new PacketPlayOutEntityTeleport(target));
                                NPC.setArmor(p, target, arm);
                            });
                        this.cancel();
                    }
                }.runTask(PeyangSuperbAntiCheat.getPlugin());
                count[0]++;
                CheatDetectNowMeta meta = cheatMeta.getMetaByPlayerUUID(player.getUniqueId());
                if (meta == null) return;
                meta.setNpcLocation(n.toVector());
            }
        };
        r.runTaskTimer(PeyangSuperbAntiCheat.getPlugin(), 0, 1);

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                r.cancel();
                this.cancel();
            }
        }.runTaskLater(PeyangSuperbAntiCheat.getPlugin(), 20 * (config.getLong("npc.seconds")));
    }

    /**
     * Aurabotのテレポート。
     *
     * @param player    プレイヤー。
     * @param target    ターゲット。
     * @param arm       装備。
     * @param reachMode リーチモードかどうか。
     */
    private static void auraBotTeleport(Player player, EntityPlayer target, ItemStack[] arm, boolean reachMode)
    {
        final double[] time = {0.0};
        final double radius = reachMode ? config.getDouble("npc.reachRange"): config.getDoubleList("npc.range")
            .get(new Random().nextInt(config.getDoubleList("npc.range").size()));

        final int[] count = {0};
        BukkitRunnable r = new BukkitRunnable()
        {
            public void run()
            {
                double speed = 0.0;

                if (player.hasMetadata("speed"))
                    for (MetadataValue value : player.getMetadata("speed"))
                        if (value.getOwningPlugin().getName().equals(PeyangSuperbAntiCheat.getPlugin().getName()))
                            speed = value.asDouble() * 2.0;
                for (double i = 0; i < Math.PI * 2; i++)
                {
                    double rangeTmp = radius - 0.1 + generateWaveValue(radius - 0.1, radius, config.getDouble("npc.waveMin"), 0.01, true);

                    final Location center = player.getLocation();
                    final Location n = new Location(
                        center.getWorld(),
                        auraBotXPos(time[0], rangeTmp + speed) + center.getX(),
                        center.getY() + generateWaveValue(1.0, 2.0, 0.0, 0.01, count[0] < 20),
                        auraBotZPos(time[0], rangeTmp + speed) + center.getZ(),
                        (float) generateWaveValue(10.0, 100.0, 10.0, 0.01, false),
                        (float) generateWaveValue(10.0, 100.0, 10.0, 0.01, false)
                    );

                    NPC.setLocation(n, target);
                    ((CraftPlayer) player).getHandle().playerConnection
                        .sendPacket(new PacketPlayOutEntityTeleport(target));

                    NPC.setArmor(player, target, arm);
                    new BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            Bukkit.getOnlinePlayers()
                                .parallelStream()
                                .filter(p -> p.hasPermission("psac.viewnpc"))
                                .forEachOrdered(p ->
                                {
                                    ((CraftPlayer) p).getHandle().playerConnection
                                        .sendPacket(new PacketPlayOutEntityTeleport(target));
                                    NPC.setArmor(p, target, arm);
                                });
                            this.cancel();
                        }
                    }.runTask(PeyangSuperbAntiCheat.getPlugin());
                    count[0]++;
                    CheatDetectNowMeta meta = cheatMeta.getMetaByPlayerUUID(player.getUniqueId());
                    if (meta == null) return;
                    meta.setNpcLocation(n.toVector());
                }
                time[0] += config.getDouble("npc.time") + (config.getBoolean("npc.speed.wave")
                    ? generateWaveValue(0.0, config.getDouble("npc.speed.waveRange"), 0 - config.getDouble("npc.speed.waveRange"), 0.001, true)
                    : 0.0);
            }
        };
        r.runTaskTimer(PeyangSuperbAntiCheat.getPlugin(), 0, 1);

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                r.cancel();
                this.cancel();
            }
        }.runTaskLater(PeyangSuperbAntiCheat.getPlugin(), 20 * (config.getLong("npc.seconds")));
    }

    /**
     * Aurabotのz軸を算出する。
     *
     * @param time   時間。
     * @param radius 半径。
     * @return 位置。
     */
    private static double auraBotZPos(double time, double radius)
    {
        return Math.sin(time) * radius;
    }

    /**
     * Aurabotのx軸を算出する。
     *
     * @param time   時間。
     * @param radius 半径。
     * @return 位置。
     */
    private static double auraBotXPos(double time, double radius)
    {
        return Math.cos(time) * radius;
    }

    /**
     * Generate a wave-like value.
     *
     * @param min     Minimum value.
     * @param max     Maximum value.
     * @param offset  Offset value.
     * @param step    Step value.
     * @param isWave  Whether to generate a wave.
     * @return Wave-like value.
     */
    private static double generateWaveValue(double min, double max, double offset, double step, boolean isWave)
    {
        if (isWave)
        {
            double value = Math.sin(step) * (max - min) / 2 + (min + max) / 2 + offset;
            return value;
        }
        return (min + max) / 2 + offset;
    }
}