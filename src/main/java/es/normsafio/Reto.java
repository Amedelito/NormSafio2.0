package es.normsafio;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

/** Reto: solo los OPs lo inician. Quien muere queda en espectador hasta que lo reviva la Cuchara. */
public final class Reto implements Listener {
    private final Normsafio pl;
    public boolean active;

    public Reto(Normsafio pl) {
        this.pl = pl;
    }

    public void start(Player starter, String bossId) {
        if (!pl.bosses.exists(bossId)) {
            Util.msg(starter, "<red>Ese boss no existe.");
            return;
        }
        active = true;
        Bukkit.broadcast(Util.c("<gradient:#f5a623:#ffd86b><b>¡EL RETO HA COMENZADO!</b></gradient> <gray>Quien caiga queda en espectador hasta que lo revivan con la Cuchara."));
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1f);
        pl.bosses.spawn(bossId, starter.getLocation());
    }

    public void stop(boolean victory) {
        if (!active) return;
        active = false;
        for (UUID id : new ArrayList<>(pl.data.dead)) revive(id, null);
        Bukkit.broadcast(Util.c(victory
                ? "<green><b>¡RETO SUPERADO!</b> <gray>Todos los caídos han vuelto."
                : "<red><b>Reto detenido.</b> <gray>Todos los caídos han vuelto."));
    }

    public void onBossDeath() {
        if (active) stop(true);
    }

    public void reviveWithSpoon(Player by, UUID target) {
        if (!pl.data.dead.contains(target)) {
            Util.msg(by, "<red>Ese jugador ya no está caído.");
            by.closeInventory();
            return;
        }
        ItemStack hand = by.getInventory().getItemInMainHand();
        boolean consumes = pl.getConfig().getBoolean("reto.spoon-consumes", true);
        if (consumes && "spoon".equals(pl.items.idOf(hand)) && by.getGameMode() != GameMode.CREATIVE) {
            hand.setAmount(hand.getAmount() - 1);
        }
        revive(target, by);
        by.closeInventory();
    }

    public void revive(UUID id, Player by) {
        Player t = Bukkit.getPlayer(id);
        if (t == null) {
            if (pl.data.dead.remove(id)) pl.data.pending.add(id);
            pl.data.save();
            return;
        }
        pl.data.dead.remove(id);
        pl.data.pending.remove(id);
        pl.data.save();
        t.setGameMode(GameMode.SURVIVAL);
        if (by != null) t.teleport(by.getLocation());
        AttributeInstance hp = t.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        t.setHealth(hp == null ? 20.0 : hp.getValue());
        t.setFoodLevel(20);
        t.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, t.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.3);
        t.getWorld().playSound(t.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        Util.msg(t, "<green><b>¡Has sido revivido!</b>");
        if (by != null) Bukkit.broadcast(Util.c("<green>" + by.getName() + " <gray>ha revivido a <green>" + t.getName() + "<gray>."));
    }

    @EventHandler
    public void onSpoon(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!"spoon".equals(pl.items.idOf(e.getItem()))) return;
        e.setCancelled(true);
        pl.menus.revive(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!active) return;
        pl.data.dead.add(e.getEntity().getUniqueId());
        pl.data.save();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!pl.data.dead.contains(p.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(pl, () -> {
            p.setGameMode(GameMode.SPECTATOR);
            Util.msg(p, "<red>Has caído. Espera a que alguien te reviva con la Cuchara de la Resurrección.");
        }, 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (pl.data.pending.contains(id)) {
            Bukkit.getScheduler().runTaskLater(pl, () -> revive(id, null), 20L);
        } else if (pl.data.dead.contains(id)) {
            Bukkit.getScheduler().runTaskLater(pl, () -> p.setGameMode(GameMode.SPECTATOR), 5L);
        }
    }
}
