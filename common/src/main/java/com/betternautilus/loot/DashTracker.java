package com.betternautilus.loot;

import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;

import java.util.WeakHashMap;

/**
 * Tracks which AbstractNautilus instances are currently in an active dash.
 *
 * Rather than a simple set, we store the jump cooldown value at the moment the
 * dash was triggered. The mixin calls markDashing() at the HEAD of the dash()
 * method (so we capture the cooldown that is about to be set). Each tick we
 * check whether the cooldown has actually finished by comparing the stored value
 * against the current one — this avoids the bug where clearDashing() fires
 * every tick while the nautilus is idle (cooldown == 0 in both dashing and
 * non-dashing states).
 *
 * isDashing() returns true from the moment dash() fires until the jump cooldown
 * next reaches 0 after having been > 0, giving the kill-drop handler the full
 * dash window to detect kills.
 *
 * WeakHashMap: entity references do not prevent GC if the entity unloads.
 */
public class DashTracker {

    // Maps entity → the cooldown tick at which this dash expires (i.e. cooldown
    // should go to 0 after this many ticks). We store System.nanoTime() of the
    // dash start and a "was dashing last tick" flag per entity instead, because
    // the vanilla cooldown resets to the full value immediately on dash() and we
    // cannot rely on reading it before dash() sets it.
    //
    // Simpler approach: store a per-entity boolean, set it to TRUE at dash HEAD,
    // set it to FALSE in the tick inject only when cooldown transitions TO 0
    // (i.e. prev > 0 → now 0). We track prevCooldown to detect that transition.

    private static final WeakHashMap<AbstractNautilus, Integer> PREV_COOLDOWN =
            new WeakHashMap<>();
    private static final WeakHashMap<AbstractNautilus, Boolean> DASHING =
            new WeakHashMap<>();

    /** Called at the HEAD of AbstractNautilus#dash(). */
    public static void markDashing(AbstractNautilus entity) {
        DASHING.put(entity, Boolean.TRUE);
    }

    /**
     * Called every tick. Clears the dashing flag only when the jump cooldown
     * transitions from > 0 back down to 0, meaning the dash window just ended.
     * While the entity is idle (cooldown already 0 before a dash), this never
     * triggers the clear.
     */
    public static void tickEntity(AbstractNautilus entity) {
        int prev = PREV_COOLDOWN.getOrDefault(entity, 0);
        int current = entity.getJumpCooldown();
        PREV_COOLDOWN.put(entity, current);

        // Transition: was counting down, just hit 0 → dash window ended
        if (prev > 0 && current == 0) {
            DASHING.remove(entity);
        }
    }

    public static boolean isDashing(AbstractNautilus entity) {
        return Boolean.TRUE.equals(DASHING.get(entity));
    }
}
