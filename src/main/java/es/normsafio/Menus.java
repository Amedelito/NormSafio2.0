package es.normsafio;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Menus {
    private final Normsafio pl;

    public Menus(Normsafio pl) {
        this.pl = pl;
    }

    // ---------- Maquina expendedora ----------

    public void shop(Player p) {
        Gui g = new Gui(Util.c("<gradient:#ff5f6d:#ffc371><b>Máquina Expendedora"), 54);
        ConfigurationSection s = pl.getConfig().getConfigurationSection("shop");
        int slot = 10;
        if (s != null) {
            for (String id : s.getKeys(false)) {
                ItemStack it = pl.items.create(id, 1);
                if (it == null) continue;
                int price = s.getInt(id);
                ItemMeta m = it.getItemMeta();
                List<Component> lore = new ArrayList<>();
                if (m.lore() != null) lore.addAll(m.lore());
                lore.add(Util.c(""));
                lore.add(Util.c("<yellow>Precio: <white>" + price + " deditas"));
                lore.add(Util.c("<green>Clic para comprar"));
                m.lore(lore);
                it.setItemMeta(m);
                g.set(slot, it, pp -> buy(pp, id, price));
                slot++;
                if (slot % 9 == 8) slot += 2;
                if (slot > 43) break;
            }
        }
        g.set(49, Util.icon(Material.SUNFLOWER, "<gold><b>Tu saldo", "<yellow>" + pl.data.bal(p.getUniqueId()) + " deditas", "<gray>Se ingresan en el pozo."), null);
        g.open(p);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    private void buy(Player p, String id, int price) {
        if (!pl.data.take(p.getUniqueId(), price)) {
            Util.msg(p, "<red>No tienes suficientes deditas (" + price + ").");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        Util.give(p, pl.items.create(id, 1));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
        shop(p);
    }

    // ---------- Pozo banco ----------

    public void bank(Player p) {
        Gui g = new Gui(Util.c("<gradient:#6dd5fa:#ffffff><b>Pozo Banco"), 27);
        int bal = pl.data.bal(p.getUniqueId());
        g.set(10, Util.icon(Material.PAPER, "<yellow>Retirar 1 dedita", "<gray>Del saldo al inventario."), pp -> withdraw(pp, 1));
        g.set(11, Util.icon(Material.PAPER, "<yellow>Retirar 10 deditas", "<gray>Del saldo al inventario."), pp -> withdraw(pp, 10));
        g.set(12, Util.icon(Material.PAPER, "<yellow>Retirar 64 deditas", "<gray>Del saldo al inventario."), pp -> withdraw(pp, 64));
        g.set(13, Util.icon(Material.SUNFLOWER, "<gold><b>Saldo", "<yellow>" + bal + " deditas"), null);
        g.set(15, Util.icon(Material.HOPPER, "<green><b>Depositar todas", "<gray>Mete al saldo todas las deditas", "<gray>que lleves en el inventario."), this::deposit);
        g.set(16, Util.icon(Material.BARRIER, "<red>Cerrar"), pp -> pp.closeInventory());
        g.open(p);
        p.playSound(p.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1f, 1f);
    }

    private void withdraw(Player p, int n) {
        if (!pl.data.take(p.getUniqueId(), n)) {
            Util.msg(p, "<red>No tienes tantas deditas en el saldo.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        Util.give(p, pl.items.create("dedita", n));
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        bank(p);
    }

    private void deposit(Player p) {
        PlayerInventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s != null && "dedita".equals(pl.items.idOf(s))) {
                n += s.getAmount();
                inv.setItem(i, null);
            }
        }
        if (n == 0) {
            Util.msg(p, "<red>No llevas deditas.");
            return;
        }
        pl.data.add(p.getUniqueId(), n);
        Util.msg(p, "<green>Has ingresado <yellow>" + n + "</yellow> deditas.");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        bank(p);
    }

    // ---------- Cuchara de la Resurreccion ----------

    public void revive(Player p) {
        List<UUID> dead = new ArrayList<>(pl.data.dead);
        if (dead.isEmpty()) {
            Util.msg(p, "<gray>Nadie ha caído. La cuchara permanece en silencio.");
            return;
        }
        Gui g = new Gui(Util.c("<gradient:#a8ff78:#78ffd6><b>Caídos"), 54);
        int slot = 0;
        for (UUID id : dead) {
            if (slot >= 54) break;
            String name = Bukkit.getOfflinePlayer(id).getName();
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(Bukkit.getOfflinePlayer(id));
            sm.displayName(Util.c("<green><b>" + (name == null ? "Desconocido" : name)));
            sm.lore(List.of(Util.c("<gray>Clic para devolverlo a la vida.")));
            head.setItemMeta(sm);
            g.set(slot++, head, pp -> pl.reto.reviveWithSpoon(pp, id));
        }
        g.open(p);
    }
}
