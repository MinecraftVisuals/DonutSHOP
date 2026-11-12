package net.simpleshop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Makes the Shard Pickaxe break a 3x3 area and play a sound. */
public final class PickaxeListener implements Listener {

    /** Key the old ShardShop plugin used, so pickaxes players already own keep working. */
    private static final NamespacedKey LEGACY_KEY = NamespacedKey.fromString("shardshop:tool");

    private final NamespacedKey toolKey;
    /** Players currently mid-break, so our own breakBlock calls don't trigger this again. */
    private final Set<UUID> working = new HashSet<>();

    public PickaxeListener(NamespacedKey toolKey) {
        this.toolKey = toolKey;
    }

    private boolean isShardPickaxe(ItemStack tool) {
        if (tool.getType() != Material.NETHERITE_PICKAXE || !tool.hasItemMeta()) return false;
        PersistentDataContainer pdc = tool.getItemMeta().getPersistentDataContainer();
        if ("pickaxe".equals(pdc.get(toolKey, PersistentDataType.STRING))) return true;
        return LEGACY_KEY != null && "pickaxe".equals(pdc.get(LEGACY_KEY, PersistentDataType.STRING));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (working.contains(player.getUniqueId())) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isShardPickaxe(tool)) return;

        // play once per swing (not once per block of the 3x3), for everyone nearby
        Block origin = event.getBlock();
        origin.getWorld().playSound(origin.getLocation().add(0.5, 0.5, 0.5),
                Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        breakArea(player, origin, tool);
    }

    private void breakArea(Player player, Block origin, ItemStack tool) {
        BlockFace face = faceOf(player, origin);

        List<Block> targets = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                Block target = switch (face) {
                    case UP, DOWN -> origin.getRelative(a, 0, b);
                    case NORTH, SOUTH -> origin.getRelative(a, b, 0);
                    default -> origin.getRelative(0, b, a);
                };
                targets.add(target);
            }
        }

        working.add(player.getUniqueId());
        try {
            for (Block target : targets) {
                if (player.getInventory().getItemInMainHand().getType() != Material.NETHERITE_PICKAXE) break;
                if (target.getType().isAir() || target.isLiquid()) continue;
                if (target.getType().getHardness() < 0) continue;
                if (!target.isPreferredTool(tool)) continue;
                player.breakBlock(target);
            }
        } finally {
            working.remove(player.getUniqueId());
        }
    }

    private BlockFace faceOf(Player player, Block origin) {
        RayTraceResult result = player.rayTraceBlocks(8.0);
        if (result != null && origin.equals(result.getHitBlock()) && result.getHitBlockFace() != null) {
            return result.getHitBlockFace();
        }
        float pitch = player.getLocation().getPitch();
        if (pitch > 45) return BlockFace.UP;
        if (pitch < -45) return BlockFace.DOWN;
        return player.getFacing().getOppositeFace();
    }
}
