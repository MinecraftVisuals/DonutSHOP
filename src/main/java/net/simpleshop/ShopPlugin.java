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

    // category pages: 3-row chest, items in the top 2 rows, buttons in the bottom row
    private static final int PAGE_SIZE = 18;
    private static final int SLOT_PREV = 18;
    private static final int SLOT_BACK = 22;
    private static final int SLOT_NEXT = 26;

    // confirm menu: 3-row chest
    private static final int SLOT_CANCEL = 9;
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_CONFIRM = 17;

    private Map<Catalog.Category, List<Catalog.Entry>> catalog;
    private EnderChests enderChests;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        catalog = Catalog.build();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new SpawnProtection(this), this);

        enderChests = new EnderChests(this);
        getServer().getPluginManager().registerEvents(enderChests, this);

        PluginCommand cmd = getCommand("shop");
        if (cmd != null) {
            cmd.setExecutor(this::onShopCommand);
        }

        int total = 0;
        for (List<Catalog.Entry> list : catalog.values()) {
            total += list.size();
        }
        getLogger().info("SimpleShop loaded with " + total + " items.");
    }

    @Override
    public void onDisable() {
        if (enderChests != null) enderChests.saveAll();
    }

    private boolean onShopCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /shop.");
            return true;
        }
        openMain(player);
        return true;
    }

    // ---------- menus ----------

    private void openMain(Player player) {
        ShopHolder holder = new ShopHolder(null, 0);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("SHOP"));
        holder.setInventory(inv);

        for (Catalog.Category c : Catalog.Category.values()) {
            ItemStack item = new ItemStack(c.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(line(c.title, NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(line(catalog.get(c).size() + " items", NamedTextColor.GRAY));
            lore.add(line("Click to open", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(c.slot, item);
        }
        player.openInventory(inv);
    }

    private void openCategory(Player player, Catalog.Category category, int page) {
        List<Catalog.Entry> entries = catalog.get(category);
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));

        ShopHolder holder = new ShopHolder(category, page);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("SHOP - " + category.title + " (" + (page + 1) + "/" + pages + ")"));
        holder.setInventory(inv);

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < entries.size(); i++) {
            inv.setItem(i, display(entries.get(start + i)));
        }
        if (page > 0) inv.setItem(SLOT_PREV, button(Material.ARROW, "Previous page"));
        inv.setItem(SLOT_BACK, button(Material.BARRIER, "Back"));
        if (page < pages - 1) inv.setItem(SLOT_NEXT, button(Material.ARROW, "Next page"));

        player.openInventory(inv);
    }

    private void openConfirm(Player player, Catalog.Category category, int page, Catalog.Entry entry) {
        ShopHolder holder = new ShopHolder(category, page, entry);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Confirm purchase"));
        holder.setInventory(inv);

        // red = cancel
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(line("Cancel", NamedTextColor.RED));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(SLOT_CANCEL, cancel);

        // the item being bought
        ItemStack item = entry.icon().clone();
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> itemLore = new ArrayList<>();
        itemLore.add(line("You get: " + entry.give().getAmount() + "x", NamedTextColor.GRAY));
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

    private ItemStack display(Catalog.Entry entry) {
        ItemStack item = entry.icon().clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(line("Price: " + levels(entry.price()), NamedTextColor.GOLD));
        lore.add(line("You get: " + entry.give().getAmount() + "x", NamedTextColor.GRAY));
        lore.add(line("Click to buy", NamedTextColor.YELLOW));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name, NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
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
                openCategory(player, holder.category(), holder.page());
            } else if (slot == SLOT_CANCEL) {
                openCategory(player, holder.category(), holder.page());
            }
            return;
        }

        // main menu
        if (holder.category() == null) {
            for (Catalog.Category c : Catalog.Category.values()) {
                if (c.slot == slot) {
                    openCategory(player, c, 0);
                    return;
                }
            }
            return;
        }

        // category menu
        List<Catalog.Entry> entries = catalog.get(holder.category());
        if (slot == SLOT_BACK) {
            openMain(player);
        } else if (slot == SLOT_PREV) {
            if (holder.page() > 0) openCategory(player, holder.category(), holder.page() - 1);
        } else if (slot == SLOT_NEXT) {
            if ((holder.page() + 1) * PAGE_SIZE < entries.size()) {
                openCategory(player, holder.category(), holder.page() + 1);
            }
        } else if (slot >= 0 && slot < PAGE_SIZE) {
            int index = holder.page() * PAGE_SIZE + slot;
            if (index < entries.size()) {
                openConfirm(player, holder.category(), holder.page(), entries.get(index));
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

        player.sendMessage(Component.text("Bought " + entry.give().getAmount() + "x ", NamedTextColor.GREEN)
                .append(entry.name().color(NamedTextColor.WHITE))
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
