package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CombatTag implements Listener {

    // how long a player stays in combat after the last player hit
    private static final int COMBAT_SECONDS = 20;
    private static final TextColor FUCHSIA = TextColor.color(0xFF00FF);

    // player -> time (ms) when their combat tag runs out
    private final Map<UUID, Long> tagged = new HashMap<>();

    public CombatTag(JavaPlugin plugin) {
        // update the action bar every 10 ticks (twice a second)
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public boolean isInCombat(Player player) {
        Long end = tagged.get(player.getUniqueId());
        return end != null && end > System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        // the victim must be a player
        if (!(e.getEntity() instanceof Player victim)) return;

        // the attacker must be a player (directly, or by shooting a projectile)
        Player attacker = null;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        // mobs, fall damage, etc. never reach here; hitting yourself doesn't count either
        if (attacker == null || attacker.equals(victim)) return;

        tag(attacker);
        tag(victim);
    }

    private void tag(Player player) {
        tagged.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_SECONDS * 1000L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        tagged.remove(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        boolean wasInCombat = isInCombat(player);
        tagged.remove(player.getUniqueId());

        // don't kill anyone when the server itself is shutting down
        if (wasInCombat && !Bukkit.isStopping()) {
            Bukkit.broadcast(
                    Component.text(player.getName(), NamedTextColor.WHITE)
                            .append(Component.text(" logged out in combat and died.", NamedTextColor.RED))
            );
            player.setHealth(0.0); // drops their items and counts as a normal death
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = tagged.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            long left = entry.getValue() - now;
            if (left <= 0) {
                it.remove();
                player.sendActionBar(Component.text("You are no longer in combat", NamedTextColor.GREEN));
                continue;
            }

            long seconds = (left + 999) / 1000; // round up so it shows 20s first, 1s last
            player.sendActionBar(
                    Component.text("Combat: ", NamedTextColor.GRAY)
                            .append(Component.text(seconds + "s", FUCHSIA))
            );
        }
    }
}
