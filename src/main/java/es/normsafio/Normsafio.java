package es.normsafio;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Normsafio extends JavaPlugin implements Listener {
    public Items items;
    public Data data;
    public Models models;
    public Menus menus;
    public Blocks blocks;
    public Bosses bosses;
    public Karts karts;
    public Reto reto;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new Items(this);
        data = new Data(this);
        data.load();
        models = new Models(this);
        menus = new Menus(this);
        blocks = new Blocks(this);
        bosses = new Bosses(this);
        karts = new Karts(this);
        reto = new Reto(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(blocks, this);
        pm.registerEvents(bosses, this);
        pm.registerEvents(karts, this);
        pm.registerEvents(reto, this);
        pm.registerEvents(this, this);
        new Armor(this).start();

        Commands cmd = new Commands(this);
        getCommand("norsafio").setExecutor(cmd);
        getCommand("norsafio").setTabCompleter(cmd);

        Bukkit.getScheduler().runTaskTimer(this, this::hud, 40L, 20L);
        getLogger().info("Normsafio activado.");
    }

    @Override
    public void onDisable() {
        if (bosses != null) bosses.clearAll();
        if (models != null) models.clear();
        if (data != null) data.save();
    }

    private void hud() {
        if (!getConfig().getBoolean("hud.actionbar", true)) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (items.idOf(p.getInventory().getItemInMainHand()) != null) {
                p.sendActionBar(Util.c("<gold>✦ <yellow>" + data.bal(p.getUniqueId()) + " <gold>deditas"));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String url = getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) return;
        String sha = getConfig().getString("resource-pack.sha1", "");
        boolean force = getConfig().getBoolean("resource-pack.force", false);
        byte[] hash = null;
        if (sha != null && sha.length() == 40) {
            hash = new byte[20];
            try {
                for (int i = 0; i < 20; i++) hash[i] = (byte) Integer.parseInt(sha.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException ex) {
                hash = null;
            }
        }
        final byte[] h = hash;
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> p.setResourcePack(url, h, force), 20L);
    }
}
