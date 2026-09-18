package net.fantasmp.homes;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps a Material to the texture-atlas sprite Minecraft actually renders for it.
 *
 * The mapping is not derivable from the Bukkit API: doors, signs, cake and beds are
 * all "blocks" yet use flat item sprites, while stone and planks use 3D block renders.
 * So the table is loaded from plugins/FantaHomes/sprites.txt, one entry per line:
 *
 *     <material> <atlas> <sprite>
 *
 * Anything absent from the table simply renders without an icon.
 */
public final class Sprites {

    private final Map<Material, Component> icons = new EnumMap<>(Material.class);
    private boolean supported = true;
    private int glyphs = 0;

    public Sprites(File file) {
        load(file);
    }

    /**
     * Glyphs from the resource pack take priority: they are pre-rendered 3D icons,
     * where a sprite is only a flat texture. Format: <material> <codepoint-hex>.
     */
    public void loadGlyphs(File file) {
        if (file == null || !file.isFile()) return;

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                Material material = Material.matchMaterial(parts[0]);
                if (material == null) continue;

                try {
                    int cp = Integer.parseInt(parts[1], 16);
                    icons.put(material, Component.text(new String(Character.toChars(cp))));
                    glyphs++;
                } catch (NumberFormatException ignored) {
                    // not a codepoint
                }
            }
        } catch (Exception ignored) {
            // no glyphs: sprites still apply
        }
    }

    public int glyphCount() {
        return glyphs;
    }

    private void load(File file) {
        if (file == null || !file.isFile()) return;

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;

                Material material = Material.matchMaterial(parts[0]);
                if (material == null) continue;

                String atlas = parts[1];
                String sprite = parts[2];

                // Entity atlases hold unwrapped 3D model sheets, not icons: drawn
                // into a 16px button they come out as a jumble. Substitute a flat
                // texture where there is a sensible one, otherwise skip.
                if (atlas.endsWith(":beds")) {
                    String colour = material.name().toLowerCase(Locale.ROOT).replace("_bed", "");
                    atlas = "minecraft:blocks";
                    sprite = "minecraft:block/" + colour + "_wool";
                } else if (atlas.endsWith(":chests") || atlas.endsWith(":shield_patterns")) {
                    continue;
                }

                Component icon = build(atlas, sprite);
                if (icon != null) icons.put(material, icon);
            }
        } catch (Exception ignored) {
            // A missing or malformed table just means no icons.
        }
    }

    /** Builds one sprite component, or null if this server is too old to render them. */
    private Component build(String atlas, String sprite) {
        if (!supported) return null;
        try {
            return Component.object(ObjectContents.sprite(key(atlas), key(sprite)));
        } catch (Throwable t) {
            supported = false;
            return null;
        }
    }

    private static Key key(String value) {
        int colon = value.indexOf(':');
        return colon < 0
                ? Key.key("minecraft", value)
                : Key.key(value.substring(0, colon), value.substring(colon + 1));
    }

    /** The sprite for this material, or null when it has none. */
    public Component icon(Material material) {
        return icons.get(material);
    }

    public boolean has(Material material) {
        return icons.containsKey(material);
    }

    public int size() {
        return icons.size();
    }
}
