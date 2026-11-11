package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Catalog {

    public static final int SPAWNER_PRICE = 250; // paid in shards
    public static final int STACK_PRICE = 1;     // paid in XP levels

    // Shard Pickaxe: sits in the Shards menu directly under the middle spawner (slot 13 -> 22)
    public static final int PICKAXE_PRICE = 100; // paid in shards
    public static final int PICKAXE_SLOT = 22;
    private static final TextColor FUCHSIA = TextColor.color(0xFF00FF);

    public enum Currency { LEVELS, SHARDS }

    // give = what the player receives, icon = what is shown in the menu
    public record Entry(ItemStack give, ItemStack icon, Component name, int price, Currency currency) {
        // everything defaults to XP levels unless a currency is given
        public Entry(ItemStack give, ItemStack icon, Component name, int price) {
            this(give, icon, name, price, Currency.LEVELS);
        }
    }

    // Main menu: middle row is slots 11-15, BLOCKS sits directly under the Totem (slot 13 -> 22)
    public enum Category {
        END("End", Material.END_STONE, 11),
        NETHER("Nether", Material.NETHER_WART_BLOCK, 12),
        GEAR("Gear", Material.TOTEM_OF_UNDYING, 13),
        FOOD("Food", Material.COOKED_BEEF, 14),
        SHARDS("Shards", Material.AMETHYST_SHARD, 15),
        BLOCKS("Blocks", Material.COBBLESTONE, 22);

        public final String title;
        public final Material icon;
        public final int slot;

        Category(String title, Material icon, int slot) {
            this.title = title;
            this.icon = icon;
            this.slot = slot;
        }
    }

    private record Mob(EntityType type, String name) {}

    private Catalog() {}

    public static Map<Category, List<Entry>> build() {
        Map<Category, List<Entry>> map = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            map.put(c, new ArrayList<>());
        }

        // ---- End (9) ----
        addNamed(map.get(Category.END),
                "ENDER_CHEST", "ENDER_PEARL", "END_STONE", "DRAGON_BREATH", "END_ROD",
                "CHORUS_FRUIT", "PURPUR_PILLAR", "PURPUR_STAIRS", "PURPUR_BLOCK");

        // ---- Nether (8) ----
        addNamed(map.get(Category.NETHER),
                "BLAZE_ROD", "BLAZE_POWDER", "MAGMA_CREAM", "GHAST_TEAR",
                "NETHER_QUARTZ_ORE", "ANCIENT_DEBRIS", "MAGMA_BLOCK", "CRYING_OBSIDIAN");

        // ---- Gear (9) ----
        List<Entry> gear = map.get(Category.GEAR);
        addNamed(gear, "OBSIDIAN", "END_CRYSTAL", "RESPAWN_ANCHOR", "GLOWSTONE",
                "TOTEM_OF_UNDYING", "ENDER_PEARL", "GOLDEN_APPLE");
        addPotion(gear, PotionType.SWIFTNESS, "Potion of Swiftness"); // change this if you want another potion
        addNamed(gear, "ARROW");

        // ---- Food (9) ----
        addNamed(map.get(Category.FOOD),
                "BAKED_POTATO", "SWEET_BERRIES", "MELON_SLICE", "CARROT", "APPLE",
                "COOKED_CHICKEN", "COOKED_BEEF", "GOLDEN_CARROT", "GOLDEN_APPLE");

        // ---- Shards / Spawners (max 9) ----
        addSpawners(map.get(Category.SHARDS));

        // ---- Blocks (7) ----
        addNamed(map.get(Category.BLOCKS),
                "SPRUCE_LOG", "DIORITE", "OAK_LOG", "STONE_BRICKS",
                "BIRCH_LOG", "PALE_OAK_LOG", "SAND");

        return map;
    }

    /** The Shard Pickaxe (netherite, breaks a 3x3 area). Kept out of the spawner row on purpose. */
    public static Entry shardPickaxe(NamespacedKey toolKey) {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        Component name = Component.text("Shard Pickaxe", FUCHSIA).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);
        meta.lore(List.of(
                Component.text("Breaks a 3x3 area", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, "pickaxe");

        // ---- Enchantments (change levels or add/remove lines as you like) ----
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);

        item.setItemMeta(meta);
        return new Entry(item, item.clone(), name, PICKAXE_PRICE, Currency.SHARDS);
    }

    private static void addNamed(List<Entry> list, String... names) {
        for (String n : names) {
            addStack(list, Material.getMaterial(n));
        }
    }

    private static void addStack(List<Entry> list, Material m) {
        if (m == null) return;
        ItemStack give = new ItemStack(m, m.getMaxStackSize());
        list.add(new Entry(give, give.clone(), Component.text(pretty(m.name())), STACK_PRICE));
    }

    private static void addPotion(List<Entry> list, PotionType type, String displayName) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        list.add(new Entry(potion, potion.clone(), Component.text(displayName), STACK_PRICE));
    }

    private static void addSpawners(List<Entry> list) {
        Mob[] mobs = {
                new Mob(EntityType.SKELETON, "Skeleton"),
                new Mob(EntityType.PIG, "Pig"),
                new Mob(EntityType.CREEPER, "Creeper"),
                new Mob(EntityType.ZOMBIFIED_PIGLIN, "Zombie Piglin"),
                new Mob(EntityType.BLAZE, "Blaze")
                // add more here, up to 9 total, e.g.:
                // new Mob(EntityType.ZOMBIE, "Zombie"),
        };

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

            list.add(new Entry(spawner, spawner.clone(), name, SPAWNER_PRICE, Currency.SHARDS));
        }
    }

    static String pretty(String key) {
        StringBuilder sb = new StringBuilder();
        for (String part : key.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
