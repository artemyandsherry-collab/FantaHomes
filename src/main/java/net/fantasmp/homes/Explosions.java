package net.fantasmp.homes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/**
 * Scales end crystal and respawn anchor explosions.
 *
 * Crystals are entities, so their power is set before the blast is calculated.
 * Respawn anchors are block explosions with no power setter, so the vanilla
 * blast is cancelled and replaced with a stronger one at the same spot.
 */
public final class Explosions implements Listener {

    /** Vanilla respawn anchor explosion power. */
    private static final float ANCHOR_BASE = 5.0f;

    private final FantaHomes plugin;
    private final double crystalMultiplier;
    private final double anchorMultiplier;
    private final boolean protectTerrain;

    /** Stops the replacement blast from being scaled a second time. */
    private boolean replacing = false;

    public Explosions(FantaHomes plugin) {
        this.plugin = plugin;
        this.crystalMultiplier = plugin.getConfig().getDouble("explosions.crystal-multiplier", 2.5);
        this.anchorMultiplier = plugin.getConfig().getDouble("explosions.anchor-multiplier", 2.5);
        this.protectTerrain = plugin.getConfig().getBoolean("explosions.protect-terrain", false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        if (replacing) return;
        if (!(event.getEntity() instanceof EnderCrystal)) return;
        event.setRadius((float) (event.getRadius() * crystalMultiplier));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (replacing) return;
        if (event.getExplodedBlockState().getType() != Material.RESPAWN_ANCHOR) return;

        Location at = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        event.setCancelled(true);

        float power = (float) (ANCHOR_BASE * anchorMultiplier);
        replacing = true;
        try {
            at.getWorld().createExplosion(at, power, true, !protectTerrain);
        } finally {
            replacing = false;
        }
    }

    /** Full damage, no terrain damage, when protect-terrain is on. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protectTerrain) return;
        if (!(event.getEntity() instanceof EnderCrystal)) return;
        event.blockList().clear();
    }

    public String summary() {
        return "crystals x" + crystalMultiplier + ", anchors x" + anchorMultiplier
                + (protectTerrain ? ", terrain protected" : "");
    }
}
