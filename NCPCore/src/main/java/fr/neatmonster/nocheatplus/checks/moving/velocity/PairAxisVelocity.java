/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.checks.moving.velocity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.neatmonster.nocheatplus.utilities.TickTask;

/**
 * Velocity accounting for Oxz plane (positive + negative direction).
 * 
 *
 */
public class PairAxisVelocity {

    /** Size of queued for which to force cleanup on add. */
    private static final double thresholdCleanup = 40;

    private final LinkedList<PairEntry> queued = new LinkedList<PairEntry>();

    /** Activation flag for tracking unused velocity. */
    private boolean unusedActive = true;
    /**
     * Sensitivity for tracking unused velocity (absolute amount, counts for
     * positive and negative).
     */
    // TODO: Ignoring 0-dist velocity allows 'moving on', though.
    private double unusedSensitivity = 0.1;

    /**
     * Add to the front of the queue.
     * @param entry
     */
    public void addToFront(PairEntry entry) {
        queued.addFirst(entry);
    }

    /**
     * Add to the end of the queue.
     * @param entry
     */
    public void add(PairEntry entry) { 
        queued.add(entry);
        if (queued.size() > thresholdCleanup) {
            removeInvalid(TickTask.getTick());
        }
    }

    public boolean hasQueued() {
        return !queued.isEmpty();
    }
    /**
     * Use the next matching entry.
     * 
     * @param amount
     * @param tolerance
     *            Allow using entries with less amount (still sign-specific).
     *            Must be equal or greater than 0.0.
     * @return The first matching entry. Returns null if no entry is available.
     *         This will directly invalidate leading entries with the wrong
     *         sign.
     */
    public List<PairEntry> use(final double x, final double z, final double tolerance) {
        final Iterator<PairEntry> it = queued.iterator();
        PairEntry entry = null;
        final List<PairEntry> result = new LinkedList<>();
        double totalAmountX = 0;
        double totalAmountZ = 0;
        int lastTick = 0;
        boolean hasVel = false;
        while (it.hasNext()) {
            entry = it.next();
            it.remove();
            if ((entry.flags & VelocityFlags.ADDITIVE) != 0 && lastTick == entry.tick) {
                totalAmountX += entry.x;
                totalAmountZ += entry.z;
                result.add(entry);
                hasVel = true;
            } else {
                if (hasVel && Math.abs(totalAmountX - x) < tolerance && Math.abs(totalAmountZ - z) < tolerance) {
                    // Success
                    break;
                }
                if (unusedActive) {
                    //for (PairEntry unused : result) {
                    //    addUnused(unused);
                    //}
                }
                result.clear();
                totalAmountX = entry.x;
                totalAmountZ = entry.z;
                lastTick = entry.tick;
                result.add(entry);
                hasVel = true;
            }
            if (!hasVel && Math.abs(totalAmountX - x) >= tolerance && Math.abs(totalAmountZ - z) >= tolerance) result.clear();
        }
        return result;
//        if (entry == null) {
//            // None found.
//            return null;
//        }
//        else {
//            if ((entry.flags & FILTER_POST_USE) != 0L) {
//                return processFlagsPostUse(entry, amount);
//            }
//            else {
//                return entry;
//            }
//        }
    }
    
    /**
     * Use the matching tick entry.
     * 
     * @param tick
     * @return The first matching entry. Returns null if no entry is available.
     *         This will directly invalidate leading entries with the wrong
     *         sign.
     */
    public List<PairEntry> use(final int tick) {
        final Iterator<PairEntry> it = queued.iterator();
        PairEntry entry = null;
        final List<PairEntry> result = new LinkedList<>();
        int lastTick = 0;
        boolean hasVel = false;
        while (it.hasNext()) {
            entry = it.next();
            it.remove();
            if ((entry.flags & VelocityFlags.ADDITIVE) != 0 && lastTick == entry.tick) {
                result.add(entry);
                hasVel = true;
            } else {
                if (hasVel && tick == result.get(0).tick) {
                    // Success
                    break;
                }
                if (unusedActive) {
                    //for (PairEntry unused : result) {
                    //    addUnused(unused);
                    //}
                }
                result.clear();
                lastTick = entry.tick;
                result.add(entry);
                hasVel = true;
            }
        }
        return result;
    }

    /**
     * Without checking for invalidation, test if there is a matching entry with
     * same or less the activation count.
     * 
     * @param amount
     * @param maxActCount
     * @param tolerance
     * @return
     */
    public List<PairEntry> peek(final double x, final double z, final int minActCount, final int maxActCount, 
            final double tolerance) {
        final List<PairEntry> result = new LinkedList<>();
        double totalAmountX = 0;
        double totalAmountZ = 0;
        int lastTick = 0;
        boolean hasVel = false;
        final Iterator<PairEntry> it = queued.iterator();
        while (it.hasNext()) {
            final PairEntry entry = it.next();
            if (entry.actCount >= minActCount && entry.actCount <= maxActCount) {
                if ((entry.flags & VelocityFlags.ADDITIVE) != 0 && lastTick == entry.tick) {
                    totalAmountX += entry.x;
                    totalAmountZ += entry.z;
                    result.add(entry);
                    hasVel = true;
                } else {
                    if (hasVel && Math.abs(totalAmountX - x) < tolerance && Math.abs(totalAmountZ - z) < tolerance) return result;
                    result.clear();
                    totalAmountX = entry.x;
                    totalAmountZ = entry.z;
                    lastTick = entry.tick;
                    result.add(entry);
                    hasVel = true;
                }
            }
        }
        if (Math.abs(totalAmountX - x) >= tolerance || Math.abs(totalAmountZ - z) >= tolerance) result.clear();
        return result;
    }
    
    /**
     * Without checking for invalidation, test if there is a matching entry with
     * same or less the activation count and tick.
     * 
     * @param amount
     * @param maxActCount
     * @param tolerance
     * @return
     */
    public List<PairEntry> peek(final int tick, final int minActCount, final int maxActCount, 
            final double tolerance) {
        final List<PairEntry> result = new LinkedList<>();
        int lastTick = 0;
        boolean hasVel = false;
        final Iterator<PairEntry> it = queued.iterator();
        while (it.hasNext()) {
            final PairEntry entry = it.next();
            if (entry.actCount >= minActCount && entry.actCount <= maxActCount) {
                if ((entry.flags & VelocityFlags.ADDITIVE) != 0 && lastTick == entry.tick) {
                    result.add(entry);
                    hasVel = true;
                } else {
                    if (hasVel && tick == result.get(0).tick) return result;
                    result.clear();
                    lastTick = entry.tick;
                    result.add(entry);
                    hasVel = true;
                }
            }
        }
        return result;
    }

    /**
     * Check if the demanded amount can be covered by this velocity entry. Might
     * return an entry with a small value with a different sign, if amount is
     * set to 0.0. Needed also for testing stored entries.
     * 
     * @param entry
     * @param amount
     * @param tolerance
     *            Allow using entries with less amount (still sign-specific).
     *            Must be equal or greater than 0.0.
     * @return
     */
    public boolean matchesEntry(final PairEntry entry, final double x, final double z, double tolerance) {
        return Math.abs(z) <= Math.abs(entry.z) + tolerance && Math.abs(x) <= Math.abs(entry.x) + tolerance;
    }

    /**
     * Remove all entries that have been added before the given tick, or for
     * which the activation count has reached 0.
     * 
     * @param invalidateBeforeTick
     *            Entries with a smaller tick value get removed.
     */
    public void removeInvalid(final int invalidateBeforeTick) {
        // Note: clear invalidated here, append unused to invalidated.
        final Iterator<PairEntry> it = queued.iterator();
        while (it.hasNext()) {
            final PairEntry entry = it.next();
            entry.actCount --; // Let others optimize this.
            if (entry.actCount <= 0 || entry.tick < invalidateBeforeTick) {
                it.remove();
                // Track unused velocity.
                //if (unusedActive) {
                //    addUnused(entry);
                //}
            }
        }
    }

    public void clear() {
        if (unusedActive && !queued.isEmpty()) {
            removeInvalid(TickTask.getTick());
        }
        queued.clear();
    }

    /**
     * Debugging.
     * 
     * @param builder
     */
    public void addQueued(final StringBuilder builder) {
        for (final PairEntry vel: queued) {
            builder.append(" ");
            builder.append(vel);
        }
    }
    public boolean isUnusedActive() {
        return unusedActive;
    }

    public void setUnusedActive(final boolean unusedActive) {
        this.unusedActive = unusedActive;
    }

    /**
     * Remove from start while the flag is present.
     * @param flag
     */
    public void removeLeadingQueuedVerticalVelocityByFlag(final long flag) {
        if (queued.isEmpty()) {
            return;
        }
        final Iterator<PairEntry> it = queued.iterator();
        while (it.hasNext()) {
            final PairEntry entry = it.next();
            if (entry.hasFlag(flag)) {
                it.remove();
            }
            else {
                break;
            }
        }
    }

}
