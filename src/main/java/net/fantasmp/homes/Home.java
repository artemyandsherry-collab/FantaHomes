package net.fantasmp.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/** A single saved home. */
public final class Home {

    private String name;
    private Material icon;
    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;

    public Home(String name, Material icon, Location loc) {
        this.name  = name;
        this.icon  = icon;
        this.world = loc.getWorld().getName();
        this.x     = loc.getX();
        this.y     = loc.getY();
        this.z     = loc.getZ();
        this.yaw   = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    private Home(String name, Material icon, String world,
                 double x, double y, double z, float yaw, float pitch) {
        this.name = name;  this.icon = icon;  this.world = world;
        this.x = x;  this.y = y;  this.z = z;  this.yaw = yaw;  this.pitch = pitch;
    }

    public String getName()      { return name; }
    public void setName(String n){ this.name = n; }
    public Material getIcon()    { return icon; }
    public void setIcon(Material m) { this.icon = m; }

    /** Null when the world is no longer loaded. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void save(ConfigurationSection sec) {
        sec.set("name",  name);
        sec.set("icon",  icon.name());
        sec.set("world", world);
        sec.set("x", x);
        sec.set("y", y);
        sec.set("z", z);
        sec.set("yaw",   yaw);
        sec.set("pitch", pitch);
    }

    /** Returns null if the section is malformed, so one bad entry can't break a player's file. */
    public static Home load(ConfigurationSection sec) {
        String name  = sec.getString("name");
        String world = sec.getString("world");
        if (name == null || world == null) return null;

        Material icon = Material.WHITE_BED;
        String iconName = sec.getString("icon");
        if (iconName != null) {
            Material m = Material.matchMaterial(iconName);
            if (m != null && m.isItem()) icon = m;
        }
        return new Home(
                name, icon, world,
                sec.getDouble("x"), sec.getDouble("y"), sec.getDouble("z"),
                (float) sec.getDouble("yaw"), (float) sec.getDouble("pitch"));
    }
}
