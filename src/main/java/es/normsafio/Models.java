package es.normsafio;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Modelos 3D (ItemDisplay) y seguimiento de entidades base. */
public final class Models {
    private final Map<UUID, UUID> follow = new HashMap<>();

    public Models(Normsafio pl) {
        Bukkit.getScheduler().runTaskTimer(pl, this::tick, 1L, 1L);
    }

    /** Crea un modelo cuyo "suelo" esta en feet. El modelo debe medir 0..16 (o hasta 32) de alto. */
    public ItemDisplay spawn(Location feet, int cmd, float scale, boolean persistent) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.setCustomModelData(cmd);
        it.setItemMeta(m);
        return feet.getWorld().spawn(feet, ItemDisplay.class, d -> {
            d.setItemStack(it);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.5f * scale, 0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
            d.setPersistent(persistent);
            if (!persistent) d.setTeleportDuration(3);
        });
    }

    public void follow(Entity base, ItemDisplay display) {
        follow.put(base.getUniqueId(), display.getUniqueId());
    }

    public void clear() {
        for (UUID d : follow.values()) {
            Entity e = Bukkit.getEntity(d);
            if (e != null) e.remove();
        }
        follow.clear();
    }

    private void tick() {
        Iterator<Map.Entry<UUID, UUID>> it = follow.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> en = it.next();
            Entity b = Bukkit.getEntity(en.getKey());
            Entity d = Bukkit.getEntity(en.getValue());
            if (d == null) { it.remove(); continue; }
            if (b == null || !b.isValid() || b.isDead()) { d.remove(); it.remove(); continue; }
            d.teleport(b.getLocation());
        }
    }
}
