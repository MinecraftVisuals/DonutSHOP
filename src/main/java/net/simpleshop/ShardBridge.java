package net.simpleshop;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Talks to the ShardShop plugin without needing it at compile time.
 * ShardShop exposes shards() -> ShardManager, which has take(UUID, int) and save().
 */
final class ShardBridge {

    private ShardBridge() {}

    private static Plugin shardPlugin() {
        Plugin p = Bukkit.getPluginManager().getPlugin("ShardShop");
        return (p != null && p.isEnabled()) ? p : null;
    }

    static boolean isAvailable() {
        return shardPlugin() != null;
    }

    /** Removes the shards and saves, or returns false if the player can't afford it. */
    static boolean take(UUID id, int amount) {
        Plugin p = shardPlugin();
        if (p == null) return false;
        try {
            Object manager = p.getClass().getMethod("shards").invoke(p);
            Method take = manager.getClass().getMethod("take", UUID.class, int.class);
            boolean ok = (boolean) take.invoke(manager, id, amount);
            if (ok) {
                manager.getClass().getMethod("save").invoke(manager);
            }
            return ok;
        } catch (ReflectiveOperationException | ClassCastException e) {
            Bukkit.getLogger().severe("[SimpleShop] Could not reach ShardShop: " + e);
            return false;
        }
    }
}
