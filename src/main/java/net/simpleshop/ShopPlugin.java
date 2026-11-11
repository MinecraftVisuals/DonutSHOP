package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopPlugin extends JavaPlugin implements Listener {

    private static final int SLOT_CANCEL = 9;
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_CONFIRM = 17;

    private List<Catalog.Entry> catalog;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        catalog = Catalog.build();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new SpawnProtection(this), this);

        PluginCommand cmd = getCommand("shop");
        if (cmd != null) {
            cmd.setExecutor(this::onShopCommand);
        }
        getLogger().info("SimpleShop loaded with " + catalog.size() + " items.");
    }

    private boolean onShopCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /shop.");
            return true;
        }
        openShop(player);
        return true;
    }

    // ---------- menus ----------

    private void openShop(Player player) {
        ShopHolder holder = new ShopHolder(null);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("SHOP"));
        holder.setInventory(inv);

        for (Catalog.Entry entry : catalog) {
            ItemStack item = entry.give().clone();
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(line("Price: " + levels(entry.price()), NamedTextColor.GOLD));
            lore.add(line("Click to buy", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(entry.slot(), item);
        }
        player.openInventory(inv);
    }

    private void openConfirm(Player player, Catalog.Entry entry) {
        ShopHolder holder = new ShopHolder(entry);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Confirm purchase"));
        holder.setInventory(inv);

        // red = cancel
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(line("Cancel", NamedTextColor.RED));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(SLOT_CANCEL, cancel);

        // the spawner being bought
        ItemStack item = entry.give().clone();
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> itemLore = new ArrayList<>();
        itemLore.add(line("Price: " + levels(entry.price()), NamedTextColor.GOLD));
        itemMeta.lore(itemLore);
        item.setItemMeta(itemMeta);
        inv.setItem(SLOT_ITEM, item);

        // green = confirm
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(line("Confirm", NamedTextColor.GREEN));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(line("Pay " + levels(entry.price()), NamedTextColor.GOLD));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(SLOT_CONFIRM, confirm);

        player.openInventory(inv);
    }

    // ---------- events ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ShopHolder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        ClickType click = e.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT
                && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) return;

        int slot = e.getSlot();

        // confirm menu
        if (holder.pending() != null) {
            if (slot == SLOT_CONFIRM) {
                buy(player, holder.pending());
                openShop(player);
            } else if (slot == SLOT_CANCEL) {
                openShop(player);
            }
            return;
        }

        // main shop
        for (Catalog.Entry entry : catalog) {
            if (entry.slot() == slot) {
                openConfirm(player, entry);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            e.setCancelled(true);
        }
    }

    // ---------- buying ----------

    private void buy(Player player, Catalog.Entry entry) {
        int price = entry.price();
        if (player.getLevel() < price) {
            player.sendMessage(Component.text("You need " + levels(price) + " to buy that.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        player.setLevel(player.getLevel() - price);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(entry.give().clone());
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }

        player.sendMessage(Component.text("Bought ", NamedTextColor.GREEN)
                .append(entry.name())
                .append(Component.text(" for " + levels(price) + ".", NamedTextColor.GREEN)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ---------- helpers ----------

    private static String levels(int n) {
        return n == 1 ? "1 level" : n + " levels";
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
