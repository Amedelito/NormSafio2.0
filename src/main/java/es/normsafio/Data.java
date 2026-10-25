package es.normsafio;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Data {
    private final Normsafio pl;
    private final File file;
    public final Map<UUID, Integer> bal = new HashMap<>();
    public final Set<UUID> dead = new HashSet<>();
    public final Set<UUID> pending = new HashSet<>();
    public final List<String> blocks = new ArrayList<>();

    public Data(Normsafio pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "data.yml");
    }

    public void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("bal");
        if (s != null) for (String k : s.getKeys(false)) bal.put(UUID.fromString(k), s.getInt(k));
        for (String u : y.getStringList("dead")) dead.add(UUID.fromString(u));
        for (String u : y.getStringList("pending")) pending.add(UUID.fromString(u));
        blocks.addAll(y.getStringList("blocks"));
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> e : bal.entrySet()) y.set("bal." + e.getKey(), e.getValue());
        y.set("dead", dead.stream().map(UUID::toString).toList());
        y.set("pending", pending.stream().map(UUID::toString).toList());
        y.set("blocks", blocks);
        try {
            pl.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            pl.getLogger().warning("No se pudo guardar data.yml: " + ex.getMessage());
        }
    }

    public int bal(UUID id) {
        return bal.getOrDefault(id, 0);
    }

    public void add(UUID id, int n) {
        bal.merge(id, n, Integer::sum);
        save();
    }

    public boolean take(UUID id, int n) {
        int b = bal(id);
        if (b < n) return false;
        bal.put(id, b - n);
        save();
        return true;
    }
}
