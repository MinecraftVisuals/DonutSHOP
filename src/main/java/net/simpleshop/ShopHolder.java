package net.simpleshop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class ShopHolder implements InventoryHolder {

    private final Catalog.Category category; // null = main menu
    private final int page;
    private Inventory inventory;

    public ShopHolder(Catalog.Category category, int page) {
        this.category = category;
        this.page = page;
    }

    public Catalog.Category category() {
        return category;
    }

    public int page() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
