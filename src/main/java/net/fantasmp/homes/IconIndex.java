package net.fantasmp.homes;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The list of materials offered in the icon picker, plus search. */
public final class IconIndex {

    private final List<Material> all = new ArrayList<>();

    public IconIndex() {
        for (Material m : Material.values()) {
            if (m.isLegacy()) continue;
            if (!m.isItem()) continue;
            if (m == Material.AIR) continue;
            all.add(m);
        }
        all.sort((a, b) -> a.name().compareTo(b.name()));
    }

    public List<Material> all() {
        return all;
    }

    /** Case-insensitive substring match; blank query returns everything. */
    public List<Material> search(String query) {
        if (query == null) return all;
        String q = query.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (q.isEmpty()) return all;

        List<Material> out = new ArrayList<>();
        for (Material m : all) {
            if (m.name().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }
}
