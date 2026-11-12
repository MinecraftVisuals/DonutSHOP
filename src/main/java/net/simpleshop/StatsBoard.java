package net.simpleshop;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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

import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Sidebar: name / shards / kills / deaths / playtime.
 * Uses built-in Minecraft symbols, so no resource pack is needed.
 */
public final class StatsBoard implements Listener {

    // Change these characters if one doesn't render on your client.
    private static final String ICON_SHARDS = "\u2605"; // star
    private static final String ICON_KILLS  = "\u2694"; // crossed swords
    private static final String ICON_DEATHS = "\u2620"; // skull
    private static final String ICON_TIME   = "\u231A"; // watch

    private static final TextColor COLOR_SHARDS = TextColor.color(0xA64DFF);
    private static final TextColor COLOR_KILLS  = TextColor.color(0xFF3C28);
    private static final TextColor COLOR_DEATHS = TextColor.color(0xFF961E);
    private static final TextColor COLOR_TIME   = TextColor.color(0xFFE128);

    private final JavaPlugin plugin;
    private final ToIntFunction<UUID> shards;

    public StatsBoard(JavaPlugin plugin, ToIntFunction<UUID> shards) {
        this.plugin = plugin;
        this.shards = shards;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player p : Bukkit.getOnlinePlayers()) {
            createBoard(p);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                update(p);
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        createBoard(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void createBoard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("stats", Criteria.DUMMY, Component.empty());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank()); // hide the red score numbers
        p.setScoreboard(board);
        update(p);
    }

    private void update(Player p) {
        Objective obj = p.getScoreboard().getObjective("stats");
        if (obj == null) {
            createBoard(p);
            return;
        }

        int kills = p.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = p.getStatistic(Statistic.DEATHS);
        long ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);

        setLine(obj, 4, "l0", Component.text(p.getName(), NamedTextColor.WHITE));
        setLine(obj, 3, "l1", row(ICON_SHARDS, COLOR_SHARDS, String.valueOf(shards.applyAsInt(p.getUniqueId()))));
        setLine(obj, 2, "l2", row(ICON_KILLS, COLOR_KILLS, String.valueOf(kills)));
        setLine(obj, 1, "l3", row(ICON_DEATHS, COLOR_DEATHS, String.valueOf(deaths)));
        setLine(obj, 0, "l4", row(ICON_TIME, COLOR_TIME, formatTime(ticks / 20L)));
    }

    private static void setLine(Objective obj, int score, String entry, Component text) {
        var s = obj.getScore(entry);
        s.setScore(score);
        s.customName(text);
    }

    private static Component row(String icon, TextColor color, String value) {
        return Component.text()
                .append(Component.text(icon, color))
                .append(Component.text(" " + value, NamedTextColor.WHITE))
                .build();
    }

    /** 10d 4h / 3h 12m / 25m */
    static String formatTime(long seconds) {
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }
}
