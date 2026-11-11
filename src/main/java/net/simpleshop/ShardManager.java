package net.simpleshop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShardManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> balances = new HashMap<>();

    public ShardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shards.yml");
    }

    public void load() {
        balances.clear();

        // First start after merging: pull everyone's balance over from the old ShardShop plugin
        if (!file.exists()) importFromOldShardShop();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), yml.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void importFromOldShardShop() {
        File old = new File(plugin.getDataFolder().getParentFile(), "ShardShop/shards.yml");
        if (!old.exists()) return;
        try {
            plugin.getDataFolder().mkdirs();
            Files.copy(old.toPath(), file.toPath());
            plugin.getLogger().info("Imported shard balances from the old ShardShop plugin.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not import old shards.yml: " + e.getMessage());
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        balances.forEach((id, amount) -> yml.set(id.toString(), amount));
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save shards.yml: " + e.getMessage());
        }
    }

    public int get(UUID id) {
        return balances.getOrDefault(id, 0);
    }

    public void add(UUID id, int amount) {
        balances.merge(id, amount, Integer::sum);
    }

    /** Returns true and removes the shards if the player has enough. */
    public boolean take(UUID id, int amount) {
        int current = get(id);
        if (current < amount) return false;
        balances.put(id, current - amount);
        return true;
    }
}
