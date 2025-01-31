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
        final double range = reachMode ? config.getDouble("npc.reachPanicRange"): config.getDouble("npc.panicRange");
        final double[] clt = {0.0};
        final int[] now = {0};

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        int sec = config.getInt("npc.seconds");

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                now[0]++;

                connection.sendPacket(new PacketPlayOutAnimation(((CraftPlayer) player).getHandle(), 1));

                HashMap<String, Object> map = new HashMap<>();
                map.put("hit", now[0]);
                map.put("max", count);

                sender.sendMessage(get("message.auraCheck.panic.lynx", map));
                if (now[0] >= count)
                    this.cancel();
            }
        }.runTaskTimer(PeyangSuperbAntiCheat.getPlugin(), 0, (long) (10 * ((1.5 / count) * sec)));


        new BukkitRunnable()
        {
            public void run()
            {
                for (double i = 0; i < Math.PI * 2; i++)
                {
                    Location center = player.getLocation();

                    if (center.getPitch() <= 0.0f || center.getPitch() > 15.0f)
                        center.setPitch(0.0f);

                    Location n = center.add(center.getDirection().multiply(0 - range));

                    n.setY(center.getY() + range);

                    n.setPitch(50);

                    NPC.setLocation(n, target);
                    connection.sendPacket(new PacketPlayOutEntityTeleport(target));
                    connection.sendPacket(new PacketPlayOutEntityHeadRotation(target, (byte) ((CraftPlayer) player).getHandle().getHeadRotation() * 0.5f));

                    NPC.setArmor(player, target, arm);
                    float finalHead = ((CraftPlayer) player).getHandle().getHeadRotation() * 0.5f;
                    new BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            Bukkit.getOnlinePlayers().parallelStream().filter(p -> p.hasPermission("psac.viewnpc"))
                                .forEachOrdered(p ->
                                {
                                    ((CraftPlayer) p).getHandle().playerConnection
                                        .sendPacket(new PacketPlayOutEntityTeleport(target));
                                    ((CraftPlayer) p).getHandle().playerConnection
                                        .sendPacket(new PacketPlayOutEntityHeadRotation(target, (byte) finalHead));
                                    NPC.setArmor(p, target, arm);
                                });
                            this.cancel();
                        }
                    }.runTask(PeyangSuperbAntiCheat.getPlugin());
                }

                clt[0] += 0.035;
                if (clt[0] >= sec)
                {
                    Variables.logger.info("Finished");
                    this.cancel();
                }
            }
        }.runTaskTimer(PeyangSuperbAntiCheat.getPlugin(), 0, 1);
    }

    /**
     * AuraBotのテレポート。
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

        // Assuming WaveCreator is now in a different package or has been removed, we will replace it with a placeholder
        // You need to implement the logic of WaveCreator or find the new equivalent class
        final double waveMin = config.getDouble("npc.waveMin");
        final double waveMax = 100.0; // Placeholder value
        final double waveFrequency = 10.0; // Placeholder value

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
                    double rangeTmp = radius;

                    // Placeholder for wave effect logic
                    if (config.getBoolean("npc.wave"))
                        rangeTmp = radius; // Replace with actual wave calculation logic

                    final Location center = player.getLocation();
                    final Location n = new Location(
                        center.getWorld(),
                        auraBotXPos(time[0], rangeTmp + speed) + center.getX(),
                        center.getY() + 1.0, // Placeholder for wave effect
                        auraBotZPos(time[0], rangeTmp + speed) + center.getZ(),
                        (float) waveFrequency,
                        (float) waveMax
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
                    if (meta == null) continue;
                    meta.setNpcLocation(n.toVector());
                }
                time[0] += config.getDouble("npc.time"); // Placeholder for wave effect
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
        return Math.sin(time) * radius * Math.cos(Math.PI / 180 * 360.0);
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
}