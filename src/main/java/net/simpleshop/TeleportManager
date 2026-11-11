package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /tp <player>      -> GUI -> you ask to teleport TO them
 * /tphere <player>  -> GUI -> you ask them to teleport to YOU
 * /tpaccept [player], /tpdeny [player] -> the receiver answers (also clickable in chat)
 */
public final class TeleportManager implements Listener {

    private static final TextColor FUCHSIA = TextColor.color(0xFF00FF);
    private static final long EXPIRE_MS = 60_000L; // requests run out after 60 seconds

    // same layout as the shop's confirm menu: 3 rows, red | head | green on the middle row
    private static final int SLOT_CANCEL = 9;
    private static final int SLOT_HEAD = 13;
    private static final int SLOT_SEND = 17;

    private record Request(UUID sender, TpHolder.Type type, long created) {}

    // receiver -> (sender -> request)
    private final Map<UUID, Map<UUID, Request>> requests = new HashMap<>();

    public TeleportManager(JavaPlugin plugin) {
        bind(plugin, "tp", this::onTp);
        bind(plugin, "tphere", this::onTpHere);
        bind(plugin, "tpaccept", (s, c, l, a) -> respond(s, a, true));
        bind(plugin, "tpdeny", (s, c, l, a) -> respond(s, a, false));
    }

    private static void bind(JavaPlugin plugin, String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) cmd.setExecutor(executor);
    }

    // ---------- commands ----------

    private boolean onTp(CommandSender sender, Command command, String label, String[] args) {
        return openFromCommand(sender, args, TpHolder.Type.TP, "tp");
    }

    private boolean onTpHere(CommandSender sender, Command command, String label, String[] args) {
        return openFromCommand(sender, args, TpHolder.Type.TPHERE, "tphere");
    }

    private boolean openFromCommand(CommandSender sender, String[] args, TpHolder.Type type, String name) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /" + name + ".");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /" + name + " <player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You can't send a request to yourself.", NamedTextColor.RED));
            return true;
        }

        openGui(player, target, type);
        return true;
    }

    // /tpaccept and /tpdeny
    private boolean respond(CommandSender s, String[] args, boolean accept) {
        if (!(s instanceof Player receiver)) {
            s.sendMessage("Only players can use this command.");
            return true;
        }

        Map<UUID, Request> mine = requests.get(receiver.getUniqueId());
        long now = System.currentTimeMillis();
        if (mine != null) mine.values().removeIf(r -> now - r.created() > EXPIRE_MS);
        if (mine == null || mine.isEmpty()) {
            requests.remove(receiver.getUniqueId());
            receiver.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return true;
        }

        Request req;
        if (args.length >= 1) {
            Player named = Bukkit.getPlayerExact(args[0]);
            req = named == null ? null : mine.get(named.getUniqueId());
            if (req == null) {
                receiver.sendMessage(Component.text("You have no request from that player.", NamedTextColor.RED));
                return true;
            }
        } else {
            req = mine.values().stream().max(Comparator.comparingLong(Request::created)).orElseThrow();
        }

        mine.remove(req.sender());
        if (mine.isEmpty()) requests.remove(receiver.getUniqueId());

        Player sender = Bukkit.getPlayer(req.sender());
        if (sender == null) {
            receiver.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            return true;
        }

        if (!accept) {
            notify(receiver, Component.text("You denied ", NamedTextColor.RED)
                    .append(Component.text(sender.getName(), FUCHSIA))
                    .append(Component.text("'s request.", NamedTextColor.RED)));
            notify(sender, Component.text(receiver.getName(), FUCHSIA)
                    .append(Component.text(" denied your request.", NamedTextColor.RED)));
            return true;
        }

        if (req.type() == TpHolder.Type.TP) {
            sender.teleport(receiver.getLocation());   // sender goes to receiver
        } else {
            receiver.teleport(sender.getLocation());   // receiver goes to sender
        }

        notify(receiver, Component.text("You accepted ", NamedTextColor.GREEN)
                .append(Component.text(sender.getName(), FUCHSIA))
                .append(Component.text("'s request.", NamedTextColor.GREEN)));
        notify(sender, Component.text(receiver.getName(), FUCHSIA)
                .append(Component.text(" accepted your request.", NamedTextColor.GREEN)));
        return true;
    }

    // ---------- GUI ----------

    private void openGui(Player player, Player target, TpHolder.Type type) {
        TpHolder holder = new TpHolder(type, target.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(
                type == TpHolder.Type.TP ? "Teleport Request" : "Teleport Here Request"));
        holder.setInventory(inv);

        inv.setItem(SLOT_CANCEL, pane(Material.RED_STAINED_GLASS_PANE, "Cancel", NamedTextColor.RED));
        inv.setItem(SLOT_HEAD, head(target, type));
        inv.setItem(SLOT_SEND, pane(Material.LIME_STAINED_GLASS_PANE, "Send request", NamedTextColor.GREEN));

        player.openInventory(inv);
    }

    private ItemStack pane(Material material, String name, TextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name, color));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack head(Player target, TpHolder.Type type) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target); // shows the target's skin
        meta.displayName(line(target.getName(), NamedTextColor.WHITE)); // hover shows their name
        List<Component> lore = new ArrayList<>();
        lore.add(line(type == TpHolder.Type.TP ? "Teleport to this player" : "Teleport this player to you",
                NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof TpHolder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        int slot = e.getSlot();
        if (slot == SLOT_SEND) {
            player.closeInventory();
            send(player, holder);
        } else if (slot == SLOT_CANCEL) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof TpHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        requests.remove(id);
        for (Map<UUID, Request> m : requests.values()) m.remove(id);
        requests.values().removeIf(Map::isEmpty);
    }

    // ---------- sending ----------

    private void send(Player sender, TpHolder holder) {
        Player target = Bukkit.getPlayer(holder.target());
        if (target == null) {
            sender.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            return;
        }

        requests.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(sender.getUniqueId(), new Request(sender.getUniqueId(), holder.type(), System.currentTimeMillis()));

        String what = holder.type() == TpHolder.Type.TP ? "teleport request" : "teleport here request";

        // sender: chat + above the hotbar
        notify(sender, Component.text("You sent a " + what + " to ")
                .append(Component.text(target.getName(), FUCHSIA)));

        // receiver: chat (with clickable buttons) + above the hotbar
        Component text = Component.text(sender.getName(), FUCHSIA)
                .append(Component.text(" sent you a " + what));

        Component accept = Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept " + sender.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept", NamedTextColor.GREEN)));
        Component deny = Component.text("[DENY]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny " + sender.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to deny", NamedTextColor.RED)));

        target.sendMessage(text.append(Component.text("  ")).append(accept).append(Component.text(" ")).append(deny));
        target.sendActionBar(text);
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
    }

    // ---------- helpers ----------

    /** Shows a message in chat AND above the hotbar. */
    private static void notify(Player player, Component message) {
        player.sendMessage(message);
        player.sendActionBar(message);
    }

    private static Component line(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
