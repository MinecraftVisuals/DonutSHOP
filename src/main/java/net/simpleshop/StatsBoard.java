
package net.simpleshop;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HexFormat;
import java.util.UUID;
import java.util.function.ToIntFunction;

public final class StatsBoard implements Listener {

    // Your GitHub resource pack download link
    private static final String PACK_URL =
            "https://github.com/MinecraftVisuals/DonutSHOP/releases/download/r1313/StatsBoard-ResourcePack.zip";

    // SHA-1 hash of the resource pack
    private static final String PACK_SHA1 =
            "5af189f3e2252f06a521a05bbb7083d79f9477df";

    // Resource pack icons
    private static final String ICON_SHARDS = "\uE000";
    private static final String ICON_KILLS  = "\uE001";
    private static final String ICON_DEATHS = "\uE002";
    private static final String ICON_TIME   = "\uE003";

    private static final Key ICON_FONT = Key.key("simpleshop", "icons");

    private final JavaPlugin plugin;
    private final ToIntFunction<UUID> shards;

    public StatsBoard(JavaPlugin plugin, ToIntFunction<UUID> shards) {
        this.plugin = plugin;
        this.shards = shards;
    }

    // Start scoreboard system
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Player p : Bukkit.getOnlinePlayers()) {
            createBoard(p);
            sendResourcePack(p);
        }

        // Update scoreboard every second
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                update(p);
            }
        }, 20L, 20L);
    }

    // Player joins
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        createBoard(p);

        // Send resource pack shortly after joining
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                sendResourcePack(p);
            }
        }, 20L);
    }

    // Send resource pack
    private void sendResourcePack(Player p) {
        p.setResourcePack(
                PACK_URL,
                HexFormat.of().parseHex(PACK_SHA1),
                true
        );
    }

    // Player leaves
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.getPlayer().setScoreboard(
                Bukkit.getScoreboardManager().getMainScoreboard()
        );
    }

    // Create scoreboard
    private void createBoard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective(
                "stats",
                Criteria.DUMMY,
                Component.empty()
        );

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Hide scoreboard numbers
        obj.numberFormat(NumberFormat.blank());

        p.setScoreboard(board);

        update(p);
    }

    // Update scoreboard information
    private void update(Player p) {
        Objective obj = p.getScoreboard().getObjective("stats");

        if (obj == null) {
            createBoard(p);
            return;
        }

        int kills = p.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = p.getStatistic(Statistic.DEATHS);

        long ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);

        int playerShards = shards.applyAsInt(p.getUniqueId());

        setLine(
                obj,
                4,
                "l0",
                Component.text(p.getName(), NamedTextColor.WHITE)
        );

        setLine(
                obj,
                3,
                "l1",
                row(ICON_SHARDS, String.valueOf(playerShards))
        );

        setLine(
                obj,
                2,
                "l2",
                row(ICON_KILLS, String.valueOf(kills))
        );

        setLine(
                obj,
                1,
                "l3",
                row(ICON_DEATHS, String.valueOf(deaths))
        );

        setLine(
                obj,
                0,
                "l4",
                row(ICON_TIME, formatTime(ticks / 20L))
        );
    }

    // Set scoreboard line
    private static void setLine(
            Objective obj,
            int score,
            String entry,
            Component text
    ) {
        var s = obj.getScore(entry);
        s.setScore(score);
        s.customName(text);
    }

    // Create icon + value row
    private static Component row(String icon, String value) {
        return Component.text()
                .append(
                        Component.text(icon, NamedTextColor.WHITE)
                                .font(ICON_FONT)
                )
                .append(
                        Component.text(" " + value, NamedTextColor.WHITE)
                )
                .build();
    }

    // Format playtime
    // Examples: 10d 4h / 3h 12m / 25m
    static String formatTime(long seconds) {
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;

        if (d > 0) {
            return d + "d " + h + "h";
        }

        if (h > 0) {
            return h + "h " + m + "m";
        }

        return m + "m";
    }
}
