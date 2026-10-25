package es.normsafio;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class Gui implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, Consumer<Player>> acts = new HashMap<>();

    public Gui(Component title, int size) {
        this.inv = Bukkit.createInventory(this, size, title);
    }

    public Gui set(int slot, ItemStack it, Consumer<Player> act) {
        inv.setItem(slot, it);
        if (act != null) acts.put(slot, act);
        return this;
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    public void click(int slot, Player p) {
        Consumer<Player> a = acts.get(slot);
        if (a != null) a.accept(p);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
