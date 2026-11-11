package net.simpleshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EnderChests implements Listener {

    public static final class Holder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        Holder(UUID owner) {
            this.owner = owner;
        }

        UUID owner() {
            return owner;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, Inventory> open = new HashMap<>();

    public EnderChests(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "enderchests");
        this.folder.mkdirs();
    }

    private int size() {
        int rows = plugin.getConfig().getInt("enderchest.rows", 5);
        rows = Math.max(1, Math.min(6, rows));
        return rows * 9;
    }

    private File fileFor(UUID id) {
        return new File(folder, id + ".yml");
    }

    // ---------- events ----------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (!plugin.getConfig().getBoolean("enderchest.enabled", true)) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;

        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) return;

        Player player = e.getPlayer();
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().getType().isAir()) return;

        e.setCancelled(true);
        openFor(player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof Holder holder) {
            save(holder.owner(), inv);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        Inventory inv = open.remove(id);
        if (inv != null) {
            save(id, inv);
        }
    }

    // ---------- logic ----------

    public void openFor(Player player) {
        UUID id = player.getUniqueId();
        Inventory inv = open.get(id);
        if (inv == null) {
            Holder holder = new Holder(id);
            inv = Bukkit.createInventory(holder, size(), Component.text("Ender Chest"));
            holder.setInventory(inv);
            load(player, inv);
            open.put(id, inv);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1f);
        player.openInventory(inv);
    }

    private void load(Player player, Inventory inv) {
        File file = fileFor(player.getUniqueId());

        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = cfg.getItemStack("items." + i);
                if (item != null) inv.setItem(i, item);
            }
            return;
        }

        // first time: move the old vanilla ender chest contents over
        ItemStack[] old = player.getEnderChest().getContents();
        for (int i = 0; i < old.length && i < inv.getSize(); i++) {
            if (old[i] != null) inv.setItem(i, old[i]);
        }
        player.getEnderChest().clear();
        save(player.getUniqueId(), inv);
    }

    private void save(UUID id, Inventory inv) {
        File file = fileFor(id);
        // load the old file first so slots beyond the current size are never lost
        YamlConfiguration cfg = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        for (int i = 0; i < inv.getSize(); i++) {
            cfg.set("items." + i, inv.getItem(i));
        }
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save ender chest for " + id + ": " + ex.getMessage());
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Inventory> entry : open.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }
}
