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

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;

    private Map<Catalog.Category, List<Catalog.Entry>> catalog;

    @Override
    public void onEnable() {
        catalog = Catalog.build();
        getServer().getPluginManager().registerEvents(this, this);

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
        Inventory inv = Bukkit.createInventory(holder, 54,
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
                buy(player, entries.get(index));
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
