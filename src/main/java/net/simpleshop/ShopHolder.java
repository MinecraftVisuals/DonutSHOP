package net.simpleshop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class ShopHolder implements InventoryHolder {

    private final Catalog.Category category; // null = main menu
    private final int page;
    private final Catalog.Entry pending;     // non-null = confirm menu
    private Inventory inventory;

    public ShopHolder(Catalog.Category category, int page) {
        this(category, page, null);
    }

    public ShopHolder(Catalog.Category category, int page, Catalog.Entry pending) {
        this.category = category;
        this.page = page;
        this.pending = pending;
    }

    public Catalog.Category category() {
        return category;
    }

    public int page() {
        return page;
    }

    public Catalog.Entry pending() {
        return pending;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
