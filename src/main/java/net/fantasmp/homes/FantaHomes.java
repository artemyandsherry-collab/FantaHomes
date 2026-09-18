package net.fantasmp.homes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class FantaHomes extends JavaPlugin implements Listener {

    private static final String LIMIT_PREFIX = "fantahomes.limit.";

    private HomeStore store;
    private IconIndex icons;
    private Sprites sprites;
    private Menus menus;

    private int defaultLimit;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.defaultLimit = getConfig().getInt("default-limit", 3);

        this.store = new HomeStore(this);
        this.sprites = new Sprites(new File(getDataFolder(), "sprites.txt"));
        this.sprites.loadGlyphs(new File(getDataFolder(), "glyphs.txt"));
        this.icons = new IconIndex(sprites);
        this.menus = new Menus(this);

        getServer().getPluginManager().registerEvents(this, this);

        if (getConfig().getBoolean("import-simplehomes-on-start", true)) {
            File f = new File(getDataFolder().getParentFile(), "Simplehomes/homes.yml");
            int n = store.importFromSimplehomes(f);
            if (n > 0) getLogger().info("Imported " + n + " home(s) from Simplehomes.");
        }

        getLogger().info("FantaHomes enabled. Default limit: " + defaultLimit
                + ", icons: " + sprites.size()
                + " (glyphs: " + sprites.glyphCount() + ")");
    }

    @Override
    public void onDisable() {
        if (store != null) store.saveAll();
    }

    public HomeStore store() { return store; }
    public IconIndex icons() { return icons; }
    public Sprites sprites() { return sprites; }

    /**
     * Highest fantahomes.limit.<n> the player holds, else the configured default.
     * fantahomes.unlimited wins outright.
     */
    public int limitFor(Player player) {
        int best = defaultLimit;
        boolean unlimited = false;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.equals("fantahomes.unlimited")) { unlimited = true; continue; }
            if (!perm.startsWith(LIMIT_PREFIX)) continue;
            try {
                int n = Integer.parseInt(perm.substring(LIMIT_PREFIX.length()));
                if (n > best) best = n;
            } catch (NumberFormatException ignored) {
                // not a numeric limit node
            }
        }
        return unlimited ? 1000 : best;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        store.unload(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("fantahomes.use")) {
            player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }
        menus.openHomes(player);
        return true;
    }
}
