package es.normsafio;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Commands implements CommandExecutor, TabCompleter {
    private final Normsafio pl;

    public Commands(Normsafio pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("norsafio.admin")) {
            Util.msg(s, "<red>Solo los OPs pueden usar este comando.");
            return true;
        }
        if (a.length == 0) {
            help(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "give" -> {
                if (a.length < 3) { Util.msg(s, "<gray>/norsafio give <jugador> <id> [cantidad]"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { Util.msg(s, "<red>Jugador no encontrado."); return true; }
                int n = 1;
                if (a.length > 3) {
                    try { n = Math.max(1, Math.min(64, Integer.parseInt(a[3]))); } catch (NumberFormatException ex) { n = 1; }
                }
                ItemStack it = pl.items.create(a[2], n);
                if (it == null) { Util.msg(s, "<red>Objeto desconocido. Usa /norsafio items"); return true; }
                Util.give(t, it);
                Util.msg(s, "<green>Entregado.");
            }
            case "items" -> Util.msg(s, "<gray>Objetos: <white>" + String.join(", ", pl.items.ids()));
            case "boss" -> {
                if (!(s instanceof Player p)) { Util.msg(s, "<red>Solo jugadores."); return true; }
                if (a.length < 2 || !pl.bosses.exists(a[1])) { Util.msg(s, "<gray>Bosses: <white>" + String.join(", ", pl.bosses.ids())); return true; }
                pl.bosses.spawn(a[1], p.getLocation());
            }
            case "reto" -> {
                if (a.length >= 2 && a[1].equalsIgnoreCase("start")) {
                    if (!(s instanceof Player p)) { Util.msg(s, "<red>Solo jugadores."); return true; }
                    if (pl.reto.active) { Util.msg(s, "<red>Ya hay un reto en marcha."); return true; }
                    String boss = a.length > 2 ? a[2] : "coloso";
                    pl.reto.start(p, boss);
                } else if (a.length >= 2 && a[1].equalsIgnoreCase("stop")) {
                    pl.bosses.clearAll();
                    pl.reto.stop(false);
                } else {
                    Util.msg(s, "<gray>/norsafio reto start [boss] | stop");
                }
            }
            case "revive" -> {
                if (a.length < 2) { Util.msg(s, "<gray>/norsafio revive <jugador>"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { Util.msg(s, "<red>Jugador no encontrado."); return true; }
                pl.reto.revive(t.getUniqueId(), s instanceof Player p ? p : null);
            }
            case "saldo" -> {
                if (a.length < 3) { Util.msg(s, "<gray>/norsafio saldo <jugador> <cantidad>"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { Util.msg(s, "<red>Jugador no encontrado."); return true; }
                try {
                    pl.data.add(t.getUniqueId(), Integer.parseInt(a[2]));
                    Util.msg(s, "<green>Saldo de " + t.getName() + ": <yellow>" + pl.data.bal(t.getUniqueId()));
                } catch (NumberFormatException ex) {
                    Util.msg(s, "<red>Cantidad no válida.");
                }
            }
            case "limpiar" -> {
                pl.bosses.clearAll();
                pl.models.clear();
                Util.msg(s, "<green>Bosses y karts eliminados.");
            }
            case "reload" -> {
                pl.reloadConfig();
                Util.msg(s, "<green>Config recargada.");
            }
            default -> help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        Util.msg(s, "<white>Comandos de administración:");
        s.sendMessage(Util.c("<gray> /norsafio give <jugador> <id> [n]"));
        s.sendMessage(Util.c("<gray> /norsafio items"));
        s.sendMessage(Util.c("<gray> /norsafio boss <id>"));
        s.sendMessage(Util.c("<gray> /norsafio reto start [boss] | stop"));
        s.sendMessage(Util.c("<gray> /norsafio revive <jugador>"));
        s.sendMessage(Util.c("<gray> /norsafio saldo <jugador> <cantidad>"));
        s.sendMessage(Util.c("<gray> /norsafio limpiar | reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("norsafio.admin")) return out;
        if (a.length == 1) {
            out.addAll(List.of("give", "items", "boss", "reto", "revive", "saldo", "limpiar", "reload"));
        } else if (a.length == 2) {
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "give", "revive", "saldo" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                case "boss" -> out.addAll(pl.bosses.ids());
                case "reto" -> out.addAll(List.of("start", "stop"));
                default -> { }
            }
        } else if (a.length == 3) {
            if (a[0].equalsIgnoreCase("give")) out.addAll(pl.items.ids());
            if (a[0].equalsIgnoreCase("reto")) out.addAll(pl.bosses.ids());
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
