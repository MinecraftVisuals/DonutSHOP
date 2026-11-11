package net.simpleshop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class ShopHolder implements InventoryHolder {

    private final Catalog.Entry pending; // null = main shop, non-null = confirm menu
    private Inventory inventory;

    public ShopHolder(Catalog.Entry pending) {
        this.pending = pending;
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
