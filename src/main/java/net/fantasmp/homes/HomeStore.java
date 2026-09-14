package net.fantasmp.homes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Per-player home storage, one YAML file per player under /playerdata.
 * Kept in memory while the player is online and written on change.
 */
public final class HomeStore {

    private final FantaHomes plugin;
    private final File dir;
    private final Map<UUID, List<Home>> cache = new HashMap<>();

    public HomeStore(FantaHomes plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "playerdata");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create " + dir.getPath());
        }
    }

    private File fileFor(UUID id) {
        return new File(dir, id + ".yml");
    }

    public List<Home> get(UUID id) {
        List<Home> cached = cache.get(id);
        if (cached != null) return cached;

        List<Home> homes = new ArrayList<>();
        File f = fileFor(id);
        if (f.exists()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
            ConfigurationSection root = yml.getConfigurationSection("homes");
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    ConfigurationSection sec = root.getConfigurationSection(key);
                    if (sec == null) continue;
                    Home h = Home.load(sec);
                    if (h != null) homes.add(h);
                }
            }
        }
        cache.put(id, homes);
        return homes;
    }

    public void save(UUID id) {
        List<Home> homes = cache.get(id);
        if (homes == null) return;

        YamlConfiguration yml = new YamlConfiguration();
        for (int i = 0; i < homes.size(); i++) {
            homes.get(i).save(yml.createSection("homes." + i));
        }
        try {
            yml.save(fileFor(id));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save homes for " + id, e);
        }
    }

    public void unload(UUID id) {
        save(id);
        cache.remove(id);
    }

    public void saveAll() {
        for (UUID id : new ArrayList<>(cache.keySet())) save(id);
    }

    public Home byName(UUID id, String name) {
        for (Home h : get(id)) {
            if (h.getName().equalsIgnoreCase(name)) return h;
        }
        return null;
    }

    public boolean add(UUID id, String name, Location loc) {
        List<Home> homes = get(id);
        if (byName(id, name) != null) return false;
        homes.add(new Home(name, Material.WHITE_BED, loc));
        save(id);
        return true;
    }

    public void remove(UUID id, Home home) {
        get(id).remove(home);
        save(id);
    }

    /**
     * One-time import from Simplehomes' homes.yml so nothing is lost on the switch.
     * Only imports for players who have no FantaHomes file yet.
     */
    public int importFromSimplehomes(File simpleHomesFile) {
        if (!simpleHomesFile.exists()) return 0;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(simpleHomesFile);
        int imported = 0;

        for (String uuidKey : yml.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (fileFor(id).exists()) continue;

            ConfigurationSection playerSec = yml.getConfigurationSection(uuidKey);
            if (playerSec == null) continue;

            List<Home> homes = new ArrayList<>();
            for (String homeName : playerSec.getKeys(false)) {
                ConfigurationSection hs = playerSec.getConfigurationSection(homeName);
                if (hs == null) continue;

                String world = hs.getString("world");
                if (world == null) continue;

                YamlConfiguration tmp = new YamlConfiguration();
                ConfigurationSection out = tmp.createSection("h");
                out.set("name",  homeName);
                out.set("icon",  Material.WHITE_BED.name());
                out.set("world", world);
                out.set("x", hs.getDouble("x"));
                out.set("y", hs.getDouble("y"));
                out.set("z", hs.getDouble("z"));
                out.set("yaw",   hs.getDouble("yaw"));
                out.set("pitch", hs.getDouble("pitch"));

                Home h = Home.load(out);
                if (h != null) homes.add(h);
            }

            if (!homes.isEmpty()) {
                cache.put(id, homes);
                save(id);
                cache.remove(id);
                imported += homes.size();
            }
        }
        return imported;
    }
}
