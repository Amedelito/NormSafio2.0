package es.normsafio;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Efectos de set completo: Guardian (diamante) y Abismo (netherita). */
public final class Armor {
    private final Normsafio pl;

    public Armor(Normsafio pl) {
        this.pl = pl;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, this::tick, 40L, 40L);
    }

    private boolean set(PlayerInventory inv, String prefix) {
        ItemStack[] a = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};
        String[] parts = {"helmet", "chest", "legs", "boots"};
        for (int i = 0; i < 4; i++) {
            if (!(prefix + parts[i]).equals(pl.items.idOf(a[i]))) return false;
        }
        return true;
    }

    private void fx(Player p, PotionEffectType t, int amp) {
        p.addPotionEffect(new PotionEffect(t, 120, amp, true, false, true));
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerInventory inv = p.getInventory();
            if (set(inv, "dia_")) {
                fx(p, PotionEffectType.SPEED, 0);
                fx(p, PotionEffectType.JUMP_BOOST, 0);
                fx(p, PotionEffectType.WATER_BREATHING, 0);
                p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 6, 0.4, 0.8, 0.4, 0.02);
            } else if (set(inv, "net_")) {
                fx(p, PotionEffectType.STRENGTH, 0);
                fx(p, PotionEffectType.RESISTANCE, 0);
                fx(p, PotionEffectType.FIRE_RESISTANCE, 0);
                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 0.2, 0), 8, 0.4, 0.1, 0.4, 0.02);
            }
        }
    }
}
