package net.simpleshop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Marks the /tp and /tphere GUI so clicks in it can be recognised. */
public final class TpHolder implements InventoryHolder {

    public enum Type { TP, TPHERE }

    private final Type type;
    private final UUID target;
    private Inventory inventory;

    public TpHolder(Type type, UUID target) {
        this.type = type;
        this.target = target;
    }

    public Type type() {
        return type;
    }

    public UUID target() {
        return target;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
