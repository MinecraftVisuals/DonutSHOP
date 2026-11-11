package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

public final class Catalog {

    public static final int SPAWNER_PRICE = 10;

    // give = the item the player receives (also used as the icon in the menus)
    public record Entry(ItemStack give, Component name, int price, int slot) {}

    private record Mob(EntityType type, String name, int slot) {}

    private Catalog() {}

    public static List<Entry> build() {
        // slots 11-15 = the five middle slots of the 3-row chest, same as the gif
        Mob[] mobs = {
                new Mob(EntityType.SKELETON, "Skeleton", 11),
                new Mob(EntityType.PIG, "Pig", 12),
                new Mob(EntityType.CREEPER, "Creeper", 13),
                new Mob(EntityType.ZOMBIFIED_PIGLIN, "Zombie Piglin", 14),
                new Mob(EntityType.BLAZE, "Blaze", 15)
        };

        List<Entry> list = new ArrayList<>();
        for (Mob mob : mobs) {
            Component name = Component.text(mob.name() + " Spawner", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);

            ItemStack spawner = new ItemStack(Material.SPAWNER);
            BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();
            CreatureSpawner state = (CreatureSpawner) meta.getBlockState();
            state.setSpawnedType(mob.type());
            meta.setBlockState(state);
            meta.displayName(name);
            spawner.setItemMeta(meta);

            list.add(new Entry(spawner, name, SPAWNER_PRICE, mob.slot()));
        }
        return list;
    }
}
