package es.normsafio;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Bosses con modelo 3D propio: un mob invisible como hitbox + un ItemDisplay que lo sigue. */
public final class Bosses implements Listener {
    public record Def(String id, String name, EntityType type, double hp, double attrScale, float dispScale,
                      int cmd, int drops, BossBar.Color color, List<String> skills) {}

    private static final class Active {
        UUID base;
        UUID disp;
        Def def;
        BossBar bar;
    }

    public static final Map<String, Def> DEFS = new java.util.LinkedHashMap<>();

    static {
        DEFS.put("coloso", new Def("coloso", "Coloso de Ceniza", EntityType.RAVAGER, 420, 1.5, 1.7f, 4001, 40,
                BossBar.Color.RED, List.of("slam", "fireballs", "summon", "leap")));
        DEFS.put("reina", new Def("reina", "Reina de Escarcha", EntityType.VINDICATOR, 320, 1.8, 2.0f, 4002, 35,
                BossBar.Color.BLUE, List.of("freeze", "slam", "summon", "fireballs")));
        DEFS.put("devorador", new Def("devorador", "Devorador del Vacío", EntityType.WITHER_SKELETON, 520, 1.8, 2.0f, 4003, 60,
                BossBar.Color.PURPLE, List.of("pull", "leap", "fireballs", "slam")));
    }

    private final Normsafio pl;
    private final NamespacedKey KEY;
    private final NamespacedKey MINION;
    private final Map<UUID, Active> active = new HashMap<>();
    private long ticks;

    public Bosses(Normsafio pl) {
        this.pl = pl;
        this.KEY = new NamespacedKey(pl, "boss");
        this.MINION = new NamespacedKey(pl, "minion");
        Bukkit.getScheduler().runTaskTimer(pl, this::tick, 1L, 1L);
    }

    public boolean exists(String id) {
        return DEFS.containsKey(id);
    }

    public LivingEntity spawn(String id, Location l) {
        Def d = DEFS.get(id);
        if (d == null) return null;
        LivingEntity b = (LivingEntity) l.getWorld().spawnEntity(l, d.type());
        b.setInvisible(true);
        b.setPersistent(false);
        b.setRemoveWhenFarAway(false);
        b.setCanPickupItems(false);
        EntityEquipment eq = b.getEquipment();
        if (eq != null) eq.clear();
        attr(b, Attribute.GENERIC_MAX_HEALTH, d.hp());
        b.setHealth(d.hp());
        attr(b, Attribute.GENERIC_SCALE, d.attrScale());
        attr(b, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.9);
        attr(b, Attribute.GENERIC_FOLLOW_RANGE, 48);
        attr(b, Attribute.GENERIC_ATTACK_DAMAGE, 14);
        b.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, id);

        ItemDisplay disp = pl.models.spawn(l, d.cmd(), d.dispScale(), false);
        pl.models.follow(b, disp);

        Active a = new Active();
        a.base = b.getUniqueId();
        a.disp = disp.getUniqueId();
        a.def = d;
        a.bar = BossBar.bossBar(Util.c("<b>" + d.name()), 1f, d.color(), BossBar.Overlay.NOTCHED_10);
        active.put(a.base, a);

        l.getWorld().playSound(l, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
        l.getWorld().spawnParticle(Particle.EXPLOSION, l, 3, 1, 1, 1, 0);
        return b;
    }

    private void attr(LivingEntity e, Attribute at, double v) {
        AttributeInstance a = e.getAttribute(at);
        if (a != null) a.setBaseValue(v);
    }

    public void clearAll() {
        for (Active a : new ArrayList<>(active.values())) {
            Entity b = Bukkit.getEntity(a.base);
            if (b != null) b.remove();
            cleanup(a);
        }
        active.clear();
    }

    private void cleanup(Active a) {
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(a.bar);
        Entity d = Bukkit.getEntity(a.disp);
        if (d != null) d.remove();
    }

    // ---------- bucle ----------

    private void tick() {
        ticks++;
        Iterator<Map.Entry<UUID, Active>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Active a = it.next().getValue();
            Entity be = Bukkit.getEntity(a.base);
            if (!(be instanceof LivingEntity b) || b.isDead() || !b.isValid()) {
                cleanup(a);
                it.remove();
                continue;
            }
            if (ticks % 10 == 0) bar(a, b);
            if (ticks % 90 == 0) skill(a, b);
        }
    }

    private void bar(Active a, LivingEntity b) {
        AttributeInstance mh = b.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = mh == null ? 1 : mh.getValue();
        a.bar.progress((float) Math.max(0, Math.min(1, b.getHealth() / max)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(b.getWorld()) && p.getLocation().distanceSquared(b.getLocation()) < 70 * 70) p.showBossBar(a.bar);
            else p.hideBossBar(a.bar);
        }
    }

    private List<Player> targets(LivingEntity b, double r) {
        List<Player> l = new ArrayList<>();
        for (Player p : b.getWorld().getNearbyPlayers(b.getLocation(), r)) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) l.add(p);
        }
        return l;
    }

    private void skill(Active a, LivingEntity b) {
        if (targets(b, 30).isEmpty()) return;
        List<String> s = a.def.skills();
        String k = s.get(ThreadLocalRandom.current().nextInt(s.size()));
        switch (k) {
            case "slam" -> slam(b);
            case "fireballs" -> fireballs(b);
            case "summon" -> summon(b);
            case "freeze" -> freeze(b);
            case "pull" -> pull(b);
            case "leap" -> leap(b);
            default -> { }
        }
    }

    private Vector away(Location from, Location to) {
        Vector v = to.toVector().subtract(from.toVector()).setY(0);
        if (v.lengthSquared() < 1e-4) v = new Vector(1, 0, 0);
        return v.normalize();
    }

    private void slam(LivingEntity b) {
        Location l = b.getLocation();
        b.getWorld().spawnParticle(Particle.EXPLOSION, l, 6, 3, 0.3, 3, 0);
        b.getWorld().spawnParticle(Particle.CLOUD, l, 60, 3, 0.2, 3, 0.1);
        b.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        for (Player p : targets(b, 7)) {
            p.damage(10, b);
            p.setVelocity(away(l, p.getLocation()).multiply(1.4).setY(0.7));
        }
    }

    private void fireballs(LivingEntity b) {
        int n = 0;
        for (Player p : targets(b, 30)) {
            if (n++ >= 3) break;
            Vector dir = p.getEyeLocation().toVector().subtract(b.getEyeLocation().toVector());
            if (dir.lengthSquared() < 1e-4) continue;
            SmallFireball f = b.launchProjectile(SmallFireball.class, dir.normalize().multiply(1.1));
            f.setIsIncendiary(false);
        }
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
    }

    private void summon(LivingEntity b) {
        Location l = b.getLocation();
        int near = 0;
        for (Entity e : l.getWorld().getNearbyEntities(l, 30, 30, 30)) {
            if (e.getPersistentDataContainer().has(MINION, PersistentDataType.BYTE)) near++;
        }
        if (near >= 6) return;
        for (int i = 0; i < 2; i++) {
            Location s = l.clone().add(ThreadLocalRandom.current().nextDouble(-3, 3), 0, ThreadLocalRandom.current().nextDouble(-3, 3));
            Entity m = l.getWorld().spawnEntity(s, i == 0 ? EntityType.ZOMBIE : EntityType.SKELETON);
            m.setPersistent(false);
            m.getPersistentDataContainer().set(MINION, PersistentDataType.BYTE, (byte) 1);
        }
        l.getWorld().playSound(l, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
        l.getWorld().spawnParticle(Particle.PORTAL, l, 80, 2, 1, 2, 0.5);
    }

    private void freeze(LivingEntity b) {
        Location l = b.getLocation();
        b.getWorld().spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 1, 0), 150, 5, 1.5, 5, 0.05);
        b.getWorld().playSound(l, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.6f);
        for (Player p : targets(b, 11)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, true, true));
            p.setFreezeTicks(220);
            p.damage(4, b);
        }
    }

    private void pull(LivingEntity b) {
        Location l = b.getLocation();
        b.getWorld().spawnParticle(Particle.REVERSE_PORTAL, l.clone().add(0, 1, 0), 120, 4, 1.5, 4, 0.3);
        b.getWorld().playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        for (Player p : targets(b, 16)) {
            Vector v = away(p.getLocation(), l).multiply(1.3).setY(0.3);
            p.setVelocity(v);
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true, true));
        }
    }

    private void leap(LivingEntity b) {
        List<Player> t = targets(b, 25);
        if (t.isEmpty()) return;
        Player p = t.get(0);
        Vector v = away(b.getLocation(), p.getLocation()).multiply(1.2).setY(0.9);
        b.setVelocity(v);
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
    }

    // ---------- muerte ----------

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        String id = e.getEntity().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        if (id == null) return;
        Def d = DEFS.get(id);
        if (d == null) return;
        e.getDrops().clear();
        e.setDroppedExp(1500);
        Location l = e.getEntity().getLocation();
        int n = d.drops();
        while (n > 0) {
            int c = Math.min(64, n);
            l.getWorld().dropItemNaturally(l, pl.items.create("dedita", c));
            n -= c;
        }
        l.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0, 1, 0), 200, 1.5, 2, 1.5, 0.4);
        l.getWorld().playSound(l, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.2f, 1f);
        Bukkit.broadcast(Util.c("<gold><b>" + d.name() + "</b> <gray>ha sido derrotado."));
        Active a = active.remove(e.getEntity().getUniqueId());
        if (a != null) cleanup(a);
        pl.reto.onBossDeath();
    }

    public Collection<String> ids() {
        return DEFS.keySet();
    }
}
