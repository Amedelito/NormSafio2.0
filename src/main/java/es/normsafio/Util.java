package es.normsafio;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class Util {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Util() {}

    public static Component c(String s) {
        return MM.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    public static void msg(CommandSender to, String s) {
        to.sendMessage(c("<gradient:#f5a623:#ffd86b><b>Normsafio</b></gradient> <dark_gray>»</dark_gray> <gray>" + s));
    }

    public static ItemStack icon(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        it.setItemMeta(meta);
        return it;
    }

    public static void give(Player p, ItemStack it) {
        if (it == null) return;
        for (ItemStack left : p.getInventory().addItem(it).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
    }
}
