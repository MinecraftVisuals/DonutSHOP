package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnProtection implements Listener {

    private static final String BYPASS = "simpleshop.spawn.bypass";

    private final JavaPlugin plugin;

    public SpawnProtection(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isProtected(Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-protection.enabled", true)) return false;

        World world = loc.getWorld();
        if (world == null) return false;

        String name = plugin.getConfig().getString("spawn-protection.world", "");
        World main = (name == null || name.isEmpty()) ? Bukkit.getWorlds().get(0) : Bukkit.getWorld(name);
        if (main == null || !world.equals(main)) return false;

        int half = plugin.getConfig().getInt("spawn-protection.size", 332) / 2;
        Location spawn = world.getSpawnLocation();

        return Math.abs(loc.getBlockX() - spawn.getBlockX()) <= half
                && Math.abs(loc.getBlockZ() - spawn.getBlockZ()) <= half;
    }

    private boolean deny(Player player, Location loc) {
        if (player.hasPermission(BYPASS)) return false;
        if (!isProtected(loc)) return false;
        player.sendMessage(Component.text("You can't build or break blocks near spawn.", NamedTextColor.RED));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> isProtected(b.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> isProtected(b.getLocation()));
    }
}
