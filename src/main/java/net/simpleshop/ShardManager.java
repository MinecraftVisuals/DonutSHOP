package net.simpleshop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShardManager {

    /** Hard ceiling for a single balance. A balance can never go above this or below 0. */
    public static final int MAX_BALANCE = Integer.MAX_VALUE;

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
                UUID id = UUID.fromString(key);
                // read as long and clamp, so a corrupt / hand-edited value can never become a negative balance
                long raw = yml.getLong(key);
                long clamped = Math.max(0L, Math.min(raw, MAX_BALANCE));
                if (clamped != raw) {
                    plugin.getLogger().warning("Balance for " + key + " in shards.yml was out of range (" + raw
                            + "), using " + clamped + " instead.");
                }
                balances.put(id, (int) clamped);
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

    /**
     * Writes all balances to disk. The data goes to a temp file first and is then moved over
     * shards.yml, so a crash in the middle of a save can't leave a half-written file.
     *
     * @return true if the data was written successfully
     */
    public boolean save() {
        YamlConfiguration yml = new YamlConfiguration();
        balances.forEach((id, amount) -> yml.set(id.toString(), amount));

        File tmp = new File(plugin.getDataFolder(), "shards.yml.tmp");
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save shards.yml: " + e.getMessage());
            return false;
        }
    }

    public int get(UUID id) {
        return balances.getOrDefault(id, 0);
    }

    /**
     * Adds shards to a balance.
     *
     * @return true if the shards were added. Returns false (and changes nothing) if the amount is
     *         zero or negative, or if the new balance would go past {@link #MAX_BALANCE}.
     */
    public boolean add(UUID id, int amount) {
        if (amount <= 0) return false;

        long updated = (long) get(id) + amount; // long math: can't wrap around
        if (updated > MAX_BALANCE) return false;

        balances.put(id, (int) updated);
        return true;
    }

    /**
     * Returns true and removes the shards if the player has enough.
     * A zero or negative amount is always refused, so this can never be used to ADD shards.
     */
    public boolean take(UUID id, int amount) {
        if (amount <= 0) return false;

        int current = get(id);
        if (current < amount) return false;
        balances.put(id, current - amount);
        return true;
    }
}
