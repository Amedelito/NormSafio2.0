package es.normsafio;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Items {
    public record Def(String id, Material mat, int cmd, String name, List<String> lore) {}

    private final Normsafio pl;
    public final NamespacedKey ID;
    private final Map<String, Def> defs = new LinkedHashMap<>();

    public Items(Normsafio pl) {
        this.pl = pl;
        this.ID = new NamespacedKey(pl, "id");

        reg("dedita", Material.PAPER, 1001, "<gradient:#ffb347:#ffd86b><b>Dedita</b>", "<gray>La moneda del Normsafio.", "<dark_gray>Se guarda en el pozo.");
        reg("spoon", Material.STICK, 1002, "<gradient:#a8ff78:#78ffd6><b>Cuchara de la Resurrección</b>", "<gray>Clic derecho: elige a un caído", "<gray>para devolverlo a la vida.");
        reg("kart", Material.PAPER, 1003, "<gradient:#ff5f6d:#ffc371><b>Kart</b>", "<gray>Clic derecho en el suelo para colocarlo.", "<gray>Agáchate + clic para recogerlo.");

        reg("block_vending", Material.BARRIER, 2001, "<gradient:#ff5f6d:#ffc371><b>Máquina Expendedora</b>", "<gray>Bloque 3D. Solo OPs lo colocan.");
        reg("block_well", Material.BARRIER, 2002, "<gradient:#6dd5fa:#ffffff><b>Pozo Banco</b>", "<gray>Bloque 3D. Solo OPs lo colocan.");
        reg("block_bonfire", Material.BARRIER, 2003, "<gradient:#ff9966:#ff5e62><b>Hoguera Gigante</b>", "<gray>Bloque 3D. Solo OPs lo colocan.");
        reg("block_crystal", Material.BARRIER, 2004, "<gradient:#c471f5:#fa71cd><b>Cristal Flotante</b>", "<gray>Bloque 3D. Solo OPs lo colocan.");
        reg("block_throne", Material.BARRIER, 2005, "<gradient:#f7971e:#ffd200><b>Trono Real</b>", "<gray>Bloque 3D. Solo OPs lo colocan.");
        reg("block_brazier", Material.BARRIER, 2006, "<gradient:#f12711:#f5af19><b>Brasero</b>", "<gray>Bloque 3D. Solo OPs lo colocan.");

        reg("dia_helmet", Material.DIAMOND_HELMET, 3001, "<gradient:#4facfe:#00f2fe><b>Casco del Guardián</b>", "<gray>Set Guardián: Velocidad, Salto y Respiración acuática.");
        reg("dia_chest", Material.DIAMOND_CHESTPLATE, 3002, "<gradient:#4facfe:#00f2fe><b>Peto del Guardián</b>", "<gray>Set Guardián: Velocidad, Salto y Respiración acuática.");
        reg("dia_legs", Material.DIAMOND_LEGGINGS, 3003, "<gradient:#4facfe:#00f2fe><b>Grebas del Guardián</b>", "<gray>Set Guardián: Velocidad, Salto y Respiración acuática.");
        reg("dia_boots", Material.DIAMOND_BOOTS, 3004, "<gradient:#4facfe:#00f2fe><b>Botas del Guardián</b>", "<gray>Set Guardián: Velocidad, Salto y Respiración acuática.");
        reg("net_helmet", Material.NETHERITE_HELMET, 3101, "<gradient:#434343:#ff6a00><b>Casco del Abismo</b>", "<gray>Set Abismo: Fuerza, Resistencia y Anti-fuego.");
        reg("net_chest", Material.NETHERITE_CHESTPLATE, 3102, "<gradient:#434343:#ff6a00><b>Peto del Abismo</b>", "<gray>Set Abismo: Fuerza, Resistencia y Anti-fuego.");
        reg("net_legs", Material.NETHERITE_LEGGINGS, 3103, "<gradient:#434343:#ff6a00><b>Grebas del Abismo</b>", "<gray>Set Abismo: Fuerza, Resistencia y Anti-fuego.");
        reg("net_boots", Material.NETHERITE_BOOTS, 3104, "<gradient:#434343:#ff6a00><b>Botas del Abismo</b>", "<gray>Set Abismo: Fuerza, Resistencia y Anti-fuego.");
    }

    private void reg(String id, Material m, int cmd, String name, String... lore) {
        defs.put(id, new Def(id, m, cmd, name, List.of(lore)));
    }

    public Collection<String> ids() {
        return defs.keySet();
    }

    public ItemStack create(String id, int amount) {
        Def d = defs.get(id);
        if (d == null) return null;
        ItemStack it = new ItemStack(d.mat(), amount);
        ItemMeta m = it.getItemMeta();
        m.displayName(Util.c(d.name()));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String s : d.lore()) lore.add(Util.c(s));
        m.lore(lore);
        m.setCustomModelData(d.cmd());
        m.getPersistentDataContainer().set(ID, PersistentDataType.STRING, id);
        if (id.startsWith("dia_") || id.startsWith("net_")) armor(m, id);
        it.setItemMeta(m);
        return it;
    }

    private void armor(ItemMeta m, String id) {
        boolean net = id.startsWith("net_");
        String part = id.substring(4);
        EquipmentSlotGroup slot;
        double arm;
        switch (part) {
            case "helmet" -> { slot = EquipmentSlotGroup.HEAD; arm = 3; }
            case "chest" -> { slot = EquipmentSlotGroup.CHEST; arm = 8; }
            case "legs" -> { slot = EquipmentSlotGroup.LEGS; arm = 6; }
            default -> { slot = EquipmentSlotGroup.FEET; arm = 3; }
        }
        m.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(new NamespacedKey(pl, id + "_armor"), arm, AttributeModifier.Operation.ADD_NUMBER, slot));
        m.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(pl, id + "_tough"), net ? 3 : 2, AttributeModifier.Operation.ADD_NUMBER, slot));
        if (net) {
            m.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey(pl, id + "_kb"), 0.1, AttributeModifier.Operation.ADD_NUMBER, slot));
        }
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    public String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(ID, PersistentDataType.STRING);
    }
}
