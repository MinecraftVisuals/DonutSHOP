package net.simpleshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Catalog {

    public static final int SPAWNER_PRICE = 10;
    public static final int STACK_PRICE = 1;

    // Anything in here can never be sold.
    private static final Set<Material> EXCLUDED = Set.of(Material.EXPERIENCE_BOTTLE);

    public record Entry(ItemStack give, ItemStack icon, Component name, int price) {}

    public enum Category {
        END("End", Material.END_STONE, 11),
        NETHER("Nether", Material.NETHER_WART_BLOCK, 12),
        PVP("PvP", Material.TOTEM_OF_UNDYING, 13),
        FOOD("Food", Material.COOKED_BEEF, 14),
        SPAWNERS("Spawners", Material.SPAWNER, 15);

        public final String title;
        public final Material icon;
        public final int slot;

        Category(String title, Material icon, int slot) {
            this.title = title;
            this.icon = icon;
            this.slot = slot;
        }
    }

    private Catalog() {}

    public static Map<Category, List<Entry>> build() {
        Map<Category, List<Entry>> map = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            map.put(c, new ArrayList<>());
        }

        // ---- PvP ----
        addNamed(map.get(Category.PVP),
                "TOTEM_OF_UNDYING", "GLOWSTONE", "RESPAWN_ANCHOR", "END_CRYSTAL",
                "OBSIDIAN", "ENDER_PEARL", "GOLDEN_APPLE", "ARROW", "SPECTRAL_ARROW");

        // ---- Nether ----
        List<Entry> nether = map.get(Category.NETHER);
        addNamed(nether,
                "NETHERRACK", "CRIMSON_NYLIUM", "WARPED_NYLIUM",
                "CRIMSON_STEM", "WARPED_STEM", "STRIPPED_CRIMSON_STEM", "STRIPPED_WARPED_STEM",
                "CRIMSON_HYPHAE", "WARPED_HYPHAE", "CRIMSON_PLANKS", "WARPED_PLANKS",
                "NETHER_WART_BLOCK", "WARPED_WART_BLOCK", "SHROOMLIGHT",
                "CRIMSON_FUNGUS", "WARPED_FUNGUS", "CRIMSON_ROOTS", "WARPED_ROOTS",
                "NETHER_SPROUTS", "WEEPING_VINES", "TWISTING_VINES", "NETHER_WART",
                "SOUL_SAND", "SOUL_SOIL", "BASALT", "POLISHED_BASALT", "SMOOTH_BASALT",
                "BLACKSTONE", "GILDED_BLACKSTONE", "POLISHED_BLACKSTONE",
                "CHISELED_POLISHED_BLACKSTONE", "POLISHED_BLACKSTONE_BRICKS",
                "CRACKED_POLISHED_BLACKSTONE_BRICKS",
                "NETHER_BRICK", "NETHER_BRICKS", "CRACKED_NETHER_BRICKS",
                "CHISELED_NETHER_BRICKS", "RED_NETHER_BRICKS",
                "MAGMA_BLOCK", "GLOWSTONE", "GLOWSTONE_DUST",
                "QUARTZ", "QUARTZ_BLOCK", "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE",
                "ANCIENT_DEBRIS", "NETHERITE_SCRAP", "NETHERITE_INGOT",
                "CRYING_OBSIDIAN", "BLAZE_ROD", "BLAZE_POWDER", "GHAST_TEAR", "MAGMA_CREAM",
                "SOUL_TORCH", "SOUL_LANTERN", "SOUL_CAMPFIRE", "RESPAWN_ANCHOR",
                "BONE_BLOCK", "LODESTONE");

        // ---- End ----
        List<Entry> end = map.get(Category.END);
        addNamed(end,
                "END_STONE", "END_STONE_BRICKS", "END_STONE_BRICK_STAIRS",
                "END_STONE_BRICK_SLAB", "END_STONE_BRICK_WALL",
                "PURPUR_BLOCK", "PURPUR_PILLAR", "PURPUR_STAIRS", "PURPUR_SLAB",
                "CHORUS_FLOWER", "CHORUS_FRUIT", "POPPED_CHORUS_FRUIT", "END_ROD",
                "ENDER_PEARL", "ENDER_EYE", "END_CRYSTAL", "ENDER_CHEST",
                "SHULKER_SHELL", "ELYTRA", "DRAGON_BREATH", "DRAGON_HEAD", "DRAGON_EGG");
        for (Material m : Material.values()) {
            if (!m.isLegacy() && m.name().endsWith("SHULKER_BOX")) {
                addStack(end, m);
            }
        }

        // ---- Food (everything edible) ----
        List<Material> foods = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isLegacy() || !m.isItem() || !m.isEdible()) continue;
            foods.add(m);
        }
        foods.sort(Comparator.comparing(Material::name));
        for (Material m : foods) {
            addStack(map.get(Category.FOOD), m);
        }

        // ---- Spawners (every mob that has a spawn egg) ----
        addSpawners(map.get(Category.SPAWNERS));

        return map;
    }

    private static void addNamed(List<Entry> list, String... names) {
        for (String n : names) {
            addStack(list, Material.getMaterial(n));
        }
    }

    private static void addStack(List<Entry> list, Material m) {
        if (m == null || EXCLUDED.contains(m)) return;
        for (Entry e : list) {
            if (e.give().getType() == m) return; // no duplicates
        }
        ItemStack give = new ItemStack(m, m.getMaxStackSize());
        list.add(new Entry(give, give.clone(), Component.text(pretty(m.name())), STACK_PRICE));
    }

    @SuppressWarnings("deprecation")
    private static void addSpawners(List<Entry> list) {
        record Found(EntityType type, String key, Material egg) {}
        List<Found> found = new ArrayList<>();

        for (EntityType type : Registry.ENTITY_TYPE) {
            try {
                String key = type.getKey().getKey();
                Material egg = Material.getMaterial(key.toUpperCase() + "_SPAWN_EGG");
                if (egg != null) {
                    found.add(new Found(type, key, egg));
                }
            } catch (Exception ignored) {
                // entity types without a key are skipped
            }
        }
        found.sort(Comparator.comparing(Found::key));

        for (Found f : found) {
            Component name = Component.text(pretty(f.key()) + " Spawner", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);

            ItemStack spawner = new ItemStack(Material.SPAWNER);
            BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();
            CreatureSpawner state = (CreatureSpawner) meta.getBlockState();
            state.setSpawnedType(f.type());
            meta.setBlockState(state);
            meta.displayName(name);
            spawner.setItemMeta(meta);

            ItemStack icon = new ItemStack(f.egg());
            ItemMeta iconMeta = icon.getItemMeta();
            iconMeta.displayName(name);
            icon.setItemMeta(iconMeta);

            list.add(new Entry(spawner, icon, name, SPAWNER_PRICE));
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
