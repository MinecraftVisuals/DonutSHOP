package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    private static final TextColor FUCHSIA = TextColor.color(0xFF00FF);

    // category pages: 3-row chest, items centered in the MIDDLE row (slots 9-17)
    private static final int MIDDLE_ROW_START = 9;
    private static final int ROW_WIDTH = 9;
    private static final int SLOT_BACK = 18; // bottom-left, red

    // confirm menu: 3-row chest
    private static final int SLOT_CANCEL = 9;
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_CONFIRM = 17;

    private Map<Catalog.Category, List<Catalog.Entry>> catalog;
    private Catalog.Entry pickaxeEntry;
    private EnderChests enderChests;
    private ShardManager shardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        shardManager = new ShardManager(this);
        shardManager.load();
        new StatsBoard(this, shardManager::get).start();

        NamespacedKey toolKey = new NamespacedKey(this, "tool");
        catalog = Catalog.build();
        pickaxeEntry = Catalog.shardPickaxe(toolKey);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new SpawnProtection(this), this);
        getServer().getPluginManager().registerEvents(new CombatTag(this), this);
        getServer().getPluginManager().registerEvents(new TeleportManager(this), this);
        getServer().getPluginManager().registerEvents(new PickaxeListener(toolKey), this);

        enderChests = new EnderChests(this);
        getServer().getPluginManager().registerEvents(enderChests, this);

        PluginCommand cmd = getCommand("shop");
        if (cmd != null) {
            cmd.setExecutor(this::onShopCommand);
        }
        PluginCommand shardsCmd = getCommand("shards");
        if (shardsCmd != null) {
            shardsCmd.setExecutor(this::onShardsCommand);
        }
        PluginCommand giveCmd = getCommand("giveshards");
        if (giveCmd != null) {
            giveCmd.setExecutor(this::onGiveShardsCommand);
        }

        // Every 1 minute (1200 ticks): give every online player 1 shard
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                shardManager.add(player.getUniqueId(), 1);
                player.sendMessage(Component.text("You received ")
                        .append(Component.text("1 Shard", FUCHSIA)));
            }
            shardManager.save();
        }, 1200L, 1200L);

        int total = 0;
        for (List<Catalog.Entry> list : catalog.values()) {
            total += list.size();
        }
        getLogger().info("SimpleShop loaded with " + total + " items.");
    }

    @Override
    public void onDisable() {
        if (enderChests != null) enderChests.saveAll();
        if (shardManager != null) shardManager.save();
    }

    private boolean onShopCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /shop.");
            return true;
        }
        openMain(player);
        return true;
    }

    private boolean onShardsCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /shards.");
            return true;
        }
        player.sendMessage(Component.text("You have ")
                .append(Component.text(shardManager.get(player.getUniqueId()) + " Shards", FUCHSIA)));
        return true;
    }

    private boolean onGiveShardsCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /giveshards <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be above 0.", NamedTextColor.RED));
            return true;
        }

        shardManager.add(target.getUniqueId(), amount);
        shardManager.save();

        sender.sendMessage(Component.text("Gave ")
                .append(Component.text(amount + " Shards", FUCHSIA))
                .append(Component.text(" to " + target.getName() + ".")));
        target.sendMessage(Component.text("You received ")
                .append(Component.text(amount + " Shards", FUCHSIA)));
        return true;
    }

    // first slot of the middle row so that `count` items are centered
    private static int startSlot(int count) {
        int n = Math.min(count, ROW_WIDTH);
        return MIDDLE_ROW_START + (ROW_WIDTH - n) / 2;
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
            int count = catalog.get(c).size() + (c == Catalog.Category.SHARDS ? 1 : 0);
            lore.add(line(count + " items", NamedTextColor.GRAY));
            lore.add(line("Click to open", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(c.slot, item);
        }
        player.openInventory(inv);
    }

    private void openCategory(Player player, Catalog.Category category) {
        List<Catalog.Entry> entries = catalog.get(category);

        ShopHolder holder = new ShopHolder(category, 0);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("SHOP - " + category.title.toUpperCase()));
        holder.setInventory(inv);

        int start = startSlot(entries.size());
        for (int i = 0; i < entries.size() && i < ROW_WIDTH; i++) {
            inv.setItem(start + i, display(entries.get(i)));
        }

        // Shards menu: Shard Pickaxe directly under the middle spawner
        if (category == Catalog.Category.SHARDS) {
            inv.setItem(Catalog.PICKAXE_SLOT, display(pickaxeEntry));
        }

        inv.setItem(SLOT_BACK, backButton());

        player.openInventory(inv);
    }

    private void openConfirm(Player player, Catalog.Category category, Catalog.Entry entry) {
        ShopHolder holder = new ShopHolder(category, 0, entry);
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
        List<Component> baseLore = itemMeta.lore();
        if (baseLore != null) itemLore.addAll(baseLore);
        itemLore.add(line("You get: " + entry.give().getAmount() + "x", NamedTextColor.GRAY));
        itemLore.add(line("Price: " + priceText(entry), priceColor(entry)));
        itemMeta.lore(itemLore);
        item.setItemMeta(itemMeta);
        inv.setItem(SLOT_ITEM, item);

        // green = confirm
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(line("Confirm", NamedTextColor.GREEN));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(line("Pay " + priceText(entry), priceColor(entry)));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(SLOT_CONFIRM, confirm);

        player.openInventory(inv);
    }

    private ItemStack display(Catalog.Entry entry) {
        ItemStack item = entry.icon().clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        List<Component> baseLore = meta.lore();
        if (baseLore != null) lore.addAll(baseLore);
        lore.add(line("Price: " + priceText(entry), priceColor(entry)));
        lore.add(line("You get: " + entry.give().getAmount() + "x", NamedTextColor.GRAY));
        lore.add(line("Click to buy", NamedTextColor.YELLOW));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("Back", NamedTextColor.RED));
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
                openCategory(player, holder.category());
            } else if (slot == SLOT_CANCEL) {
                openCategory(player, holder.category());
            }
            return;
        }

        // main menu
        if (holder.category() == null) {
            for (Catalog.Category c : Catalog.Category.values()) {
                if (c.slot == slot) {
                    openCategory(player, c);
                    return;
                }
            }
            return;
        }

        // category menu
        List<Catalog.Entry> entries = catalog.get(holder.category());
        if (slot == SLOT_BACK) {
            openMain(player);
            return;
        }

        if (holder.category() == Catalog.Category.SHARDS && slot == Catalog.PICKAXE_SLOT) {
            openConfirm(player, holder.category(), pickaxeEntry);
            return;
        }

        int start = startSlot(entries.size());
        int index = slot - start;
        if (index >= 0 && index < entries.size() && index < ROW_WIDTH) {
            openConfirm(player, holder.category(), entries.get(index));
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

        if (entry.currency() == Catalog.Currency.SHARDS) {
            if (!shardManager.take(player.getUniqueId(), price)) {
                player.sendMessage(Component.text("You need ", NamedTextColor.RED)
                        .append(Component.text(shards(price), FUCHSIA))
                        .append(Component.text(" to buy that.", NamedTextColor.RED)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            shardManager.save();
        } else {
            if (player.getLevel() < price) {
                player.sendMessage(Component.text("You need " + levels(price) + " to buy that.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            player.setLevel(player.getLevel() - price);
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(entry.give().clone());
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }

        player.sendMessage(Component.text("Bought " + entry.give().getAmount() + "x ", NamedTextColor.GREEN)
                .append(entry.name().color(NamedTextColor.WHITE))
                .append(Component.text(" for " + priceText(entry) + ".", NamedTextColor.GREEN)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ---------- helpers ----------

    private static String levels(int n) {
        return n == 1 ? "1 level" : n + " levels";
    }

    private static String shards(int n) {
        return n == 1 ? "1 Shard" : n + " Shards";
    }

    private static String priceText(Catalog.Entry entry) {
        return entry.currency() == Catalog.Currency.SHARDS ? shards(entry.price()) : levels(entry.price());
    }

    private static TextColor priceColor(Catalog.Entry entry) {
        return entry.currency() == Catalog.Currency.SHARDS ? FUCHSIA : NamedTextColor.GOLD;
    }

    private static Component line(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
