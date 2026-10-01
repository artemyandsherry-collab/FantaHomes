package net.fantasmp.homes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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
    private final double crystalDamage;
    private final double anchorDamage;

    /** Stops the replacement blast from being scaled a second time. */
    private boolean replacing = false;

    public Explosions(FantaHomes plugin) {
        this.plugin = plugin;
        this.crystalMultiplier = plugin.getConfig().getDouble("explosions.crystal-multiplier", 1.0);
        this.anchorMultiplier = plugin.getConfig().getDouble("explosions.anchor-multiplier", 1.0);
        this.crystalDamage = plugin.getConfig().getDouble("explosions.crystal-damage-multiplier", 1.0);
        this.anchorDamage = plugin.getConfig().getDouble("explosions.anchor-damage-multiplier", 1.0);
        this.protectTerrain = plugin.getConfig().getBoolean("explosions.protect-terrain", false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        if (replacing || crystalMultiplier == 1.0) return;
        if (!(event.getEntity() instanceof EnderCrystal)) return;
        event.setRadius((float) (event.getRadius() * crystalMultiplier));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (replacing || anchorMultiplier == 1.0) return;
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

    /** Crystal blast damage to players/mobs. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (crystalDamage == 1.0) return;
        if (event.getCause() != DamageCause.ENTITY_EXPLOSION) return;
        if (!(event.getDamager() instanceof EnderCrystal)) return;
        event.setDamage(event.getDamage() * crystalDamage);
    }

    /** Respawn anchor blast damage to players/mobs. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnchorDamage(EntityDamageByBlockEvent event) {
        if (anchorDamage == 1.0) return;
        if (event.getCause() != DamageCause.BLOCK_EXPLOSION) return;
        var state = event.getDamagerBlockState();
        if (state == null || state.getType() != Material.RESPAWN_ANCHOR) return;
        event.setDamage(event.getDamage() * anchorDamage);
    }

    public String summary() {
        return "crystals x" + crystalMultiplier + " dmg x" + crystalDamage + ", anchors x" + anchorMultiplier + " dmg x" + anchorDamage
                + (protectTerrain ? ", terrain protected" : "");
    }
}
