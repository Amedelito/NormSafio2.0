package es.normsafio;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Kart: un caballo invisible y silencioso hace de motor (asi se maneja con WASD) y encima va el modelo 3D.
 * Nadie ve al caballo, solo el kart.
 */
public final class Karts implements Listener {
    private final Normsafio pl;
    private final NamespacedKey KEY;

    public Karts(Normsafio pl) {
        this.pl = pl;
        this.KEY = new NamespacedKey(pl, "kart");
    }

    private boolean isKart(Entity e) {
        return e != null && e.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    public Horse spawn(Location l, Player owner) {
        Horse h = l.getWorld().spawn(l, Horse.class, e -> {
            e.setAdult();
            e.setTamed(true);
            e.setOwner(owner);
            e.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            e.setInvisible(true);
            e.setSilent(true);
            e.setPersistent(false);
            e.setRemoveWhenFarAway(false);
            e.setInvulnerable(true);
            e.setCollidable(false);
            e.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
            AttributeInstance sp = e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (sp != null) sp.setBaseValue(0.38);
            AttributeInstance jp = e.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
            if (jp != null) jp.setBaseValue(0.2);
        });
        Bukkit.getMobGoals().removeAllGoals(h);
        ItemDisplay d = pl.models.spawn(l, 1003, 1.3f, false);
        pl.models.follow(h, d);
        return h;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!"kart".equals(pl.items.idOf(e.getItem())) || e.getClickedBlock() == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Location l = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        l.setYaw(p.getLocation().getYaw());
        Horse h = spawn(l, p);
        if (p.getGameMode() != GameMode.CREATIVE) e.getItem().setAmount(e.getItem().getAmount() - 1);
        h.addPassenger(p);
        p.playSound(l, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.4f);
    }

    @EventHandler
    public void onUse(PlayerInteractEntityEvent e) {
        if (!isKart(e.getRightClicked())) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Entity k = e.getRightClicked();
        if (p.isSneaking()) {
            k.remove();
            Util.give(p, pl.items.create("kart", 1));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            return;
        }
        if (k.getPassengers().isEmpty()) k.addPassenger(p);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (isKart(e.getEntity())) e.setCancelled(true);
    }
}
