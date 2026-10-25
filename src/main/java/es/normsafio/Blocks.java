package es.normsafio;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bloques 3D personalizados: barrera invisible + modelo + hitbox. Solo OPs los colocan y rompen. */
public final class Blocks implements Listener {
    public record Def(int cmd, int h, float w, String fx) {}

    public static final Map<String, Def> DEFS = Map.of(
            "block_vending", new Def(2001, 2, 1f, ""),
            "block_well", new Def(2002, 2, 1f, ""),
            "block_bonfire", new Def(2003, 1, 1.4f, "fire"),
            "block_crystal", new Def(2004, 2, 1f, "magic"),
            "block_throne", new Def(2005, 2, 1f, ""),
            "block_brazier", new Def(2006, 1, 1f, "fire"));

    private final Normsafio pl;
    private final NamespacedKey TAG;
    private final Map<UUID, Long> last = new HashMap<>();

    public Blocks(Normsafio pl) {
        this.pl = pl;
        this.TAG = new NamespacedKey(pl, "nb");
        Bukkit.getScheduler().runTaskTimer(pl, this::fx, 20L, 6L);
    }

    // ---------- colocar / quitar ----------

    public void place(Location base, String id, float yaw) {
        Def d = DEFS.get(id);
        if (d == null) return;
        World w = base.getWorld();
        for (int i = 0; i < d.h(); i++) base.clone().add(0, i, 0).getBlock().setType(Material.BARRIER);
        Location feet = base.clone().add(0.5, 0, 0.5);
        ItemDisplay disp = pl.models.spawn(feet, d.cmd(), 1f, true);
        disp.setRotation(yaw, 0f);
        disp.getPersistentDataContainer().set(TAG, PersistentDataType.STRING, id);
        if (!d.fx().isEmpty()) disp.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        w.spawn(feet, Interaction.class, e -> {
            e.setInteractionWidth(d.w());
            e.setInteractionHeight(d.h());
            e.setPersistent(true);
            e.getPersistentDataContainer().set(TAG, PersistentDataType.STRING, id);
        });
        pl.data.blocks.add(w.getName() + ";" + base.getBlockX() + ";" + base.getBlockY() + ";" + base.getBlockZ() + ";" + id);
        pl.data.save();
    }

    private void remove(Entity any) {
        String id = tag(any);
        Def d = id == null ? null : DEFS.get(id);
        if (d == null) { any.remove(); return; }
        Block base = any.getLocation().getBlock();
        for (Entity e : find(base)) e.remove();
        for (int i = 0; i < d.h(); i++) {
            Block b = base.getRelative(0, i, 0);
            if (b.getType() == Material.BARRIER) b.setType(Material.AIR);
        }
        Location c = base.getLocation().add(0.5, 0.5, 0.5);
        c.getWorld().dropItemNaturally(c, pl.items.create(id, 1));
        c.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        String key = base.getWorld().getName() + ";" + base.getX() + ";" + base.getY() + ";" + base.getZ() + ";" + id;
        pl.data.blocks.remove(key);
        pl.data.save();
    }

    private List<Entity> find(Block b) {
        Location c = b.getLocation().add(0.5, 0, 0.5);
        return c.getWorld().getNearbyEntities(c, 0.2, 0.3, 0.2).stream()
                .filter(e -> e.getPersistentDataContainer().has(TAG, PersistentDataType.STRING))
                .toList();
    }

    private String tag(Entity e) {
        return e == null ? null : e.getPersistentDataContainer().get(TAG, PersistentDataType.STRING);
    }

    // ---------- uso ----------

    private boolean debounce(Player p) {
        long now = System.currentTimeMillis();
        Long l = last.get(p.getUniqueId());
        if (l != null && now - l < 200) return true;
        last.put(p.getUniqueId(), now);
        return false;
    }

    private void use(Player p, String id) {
        if (debounce(p)) return;
        switch (id) {
            case "block_vending" -> pl.menus.shop(p);
            case "block_well" -> pl.menus.bank(p);
            case "block_bonfire", "block_brazier" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 1, true, true, true));
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        String id = pl.items.idOf(e.getItemInHand());
        Def d = id == null ? null : DEFS.get(id);
        if (d == null) return;
        Player p = e.getPlayer();
        if (!p.isOp()) {
            e.setCancelled(true);
            Util.msg(p, "<red>Solo los OPs pueden colocar estos bloques.");
            return;
        }
        Block b = e.getBlockPlaced();
        for (int i = 1; i < d.h(); i++) {
            if (!b.getRelative(0, i, 0).getType().isAir()) {
                e.setCancelled(true);
                Util.msg(p, "<red>No hay espacio encima para este bloque.");
                return;
            }
        }
        float yaw = Math.round(p.getLocation().getYaw() / 90f) * 90f + 180f;
        Location base = b.getLocation();
        Bukkit.getScheduler().runTask(pl, () -> place(base, id, yaw));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.BARRIER) return;
        List<Entity> l = find(b);
        if (l.isEmpty()) return;
        e.setCancelled(true);
        if (e.getPlayer().isOp()) remove(l.get(0));
    }

    @EventHandler
    public void onUseEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        String id = tag(e.getRightClicked());
        if (id == null) return;
        e.setCancelled(true);
        if (e.getPlayer().isSneaking()) return;
        use(e.getPlayer(), id);
    }

    @EventHandler
    public void onUseBlock(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.BARRIER) return;
        List<Entity> l = find(b);
        if (l.isEmpty()) return;
        String held = pl.items.idOf(e.getItem());
        if (held != null && DEFS.containsKey(held)) return;
        if (e.getPlayer().isSneaking()) return;
        e.setCancelled(true);
        use(e.getPlayer(), tag(l.get(0)));
    }

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent e) {
        Entity t = e.getAttacked();
        if (tag(t) == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.isOp()) return;
        if (!p.isSneaking()) {
            Util.msg(p, "<gray>Agáchate y golpea para quitar el bloque.");
            return;
        }
        remove(t);
    }

    // ---------- efectos ----------

    private void fx() {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        for (String s : pl.data.blocks) {
            String[] a = s.split(";");
            if (a.length < 5) continue;
            World w = Bukkit.getWorld(a[0]);
            if (w == null) continue;
            int x = Integer.parseInt(a[1]);
            int y = Integer.parseInt(a[2]);
            int z = Integer.parseInt(a[3]);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Def d = DEFS.get(a[4]);
            if (d == null || d.fx().isEmpty()) continue;
            Location c = new Location(w, x + 0.5, y, z + 0.5);
            if (d.fx().equals("fire")) {
                w.spawnParticle(Particle.FLAME, c.clone().add(0, 0.8, 0), 3, 0.25, 0.2, 0.25, 0.01);
                if (r.nextInt(4) == 0) w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.6, 0), 1, 0.2, 0.1, 0.2, 0.01);
                if (r.nextInt(10) == 0) w.playSound(c, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.6f, 1f);
            } else {
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1.4, 0), 1, 0.4, 0.6, 0.4, 0.01);
                w.spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.4, 0), 2, 0.5, 0.6, 0.5, 0.2);
            }
        }
    }
}
