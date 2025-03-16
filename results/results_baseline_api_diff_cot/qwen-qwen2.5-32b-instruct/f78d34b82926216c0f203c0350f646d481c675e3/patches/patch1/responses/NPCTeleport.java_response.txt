package ml.peya.plugins.Detect;

import tokyo.peya.lib.WaveCreator; // Import the new WaveCreator class
import develop.p2p.lib.*;
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
            @Override
            public void run()
            {
                for (double i = 0; i < Math.PI * 2; i++)
                {
                    double rangeTmp = range;

                    if (config.getBoolean("npc.wave"))
                        rangeTmp = new WaveCreator(radius - 0.1, radius, 0 - config.getDouble("npc.waveRange")).get(0.01, true);

                    final Location center = player.getLocation();
                    final Location n = new Location(
                        center.getWorld(),
                        auraBotXPos(time[0], rangeTmp) + center.getX(),
                        center.getY() + new WaveCreator(1.0, 2.0, 0 - config.getDouble("npc.waveRange")).get(0.01, count[0] < 20),
                        auraBotZPos(time[0], rangeTmp) + center.getZ(),
                        (float) ypp.getStatic(),
                        (float) ypp.get(4.5, false)
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
                time[0] += config.getDouble("npc.time") + (config.getBoolean("npc.speed.wave")
                    ? new WaveCreator(0.0, config.getDouble("npc.speed.waveRange"), 0 - config.getDouble("npc.speed.waveRange")).get(0.001, true)
                    : 0.0);
            }
        }.runTaskTimer(PeyangSuperbAntiCheat.getPlugin(), 0, 1);

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