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
package fr.neatmonster.nocheatplus.checks.blockinteract;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Player specific data for the block interact checks.
 */
public class BlockInteractData extends ACheckData {

    // Violation levels.
    public double directionVL;
    public double reachVL;
    public double speedVL;
    public double visibleVL;

    // Data shared between checks
    // Last block interacted with
    /** Set to Integer.MAX_VALUE for reset. */
    private int lastX = Integer.MAX_VALUE;
    private int lastY, lastZ;
    /** null for air */
    private Material lastType = null;
    private Block lastBlock = null;
    private int lastTick;
    private Action lastAction = null;
    private boolean lastAllowUseBlock = false;
    private boolean lastAllowUseItem = false;
    private boolean lastIsCancelled = true;
    
    // Data of the speed check.
    /** Last reset time. */
    public long speedTime   = 0;
    /** Number of interactions since last reset-time. */
    public int  speedCount  = 0;

    /** Cancel is set, times in a row. */
    public int subsequentCancel = 0;

    /**
     * Skipped actions due to rate limiting (debugging rather). May be reset
     * with logging.
     */
    public int rateLimitSkip = 0;

    /**
     * Checks that have been run and passed for the block last interacted with
     * (rather complete for block-interact checks, to skip subsequent
     * block-break/place checks).
     */
    private final Set<CheckType> passedChecks = new HashSet<CheckType>();
    /**
     * Checks that have been run and consume this event, i.e. can't be run again
     * (not complete, may contain checks from other check groups).
     */
    private final Set<CheckType> consumedChecks = new HashSet<CheckType>();

    /**
     * Set last interacted block (coordinates, type, tick). Also resets the
     * passed checks.
     * 
     * @param block
     * @param action
     * @param rightClickedBlock
     */
    public void setLastBlock(final Block block, final Action action, final Block rightClickedBlock) {
        lastTick = TickTask.getTick();
        lastAction = action;
        lastX = block.getX();
        lastY = block.getY();
        lastZ = block.getZ();
        lastType = block.getType();
        lastBlock = rightClickedBlock;
        if (lastType == Material.AIR) {
            lastType = null;
        }
        resetPassedChecks();
        resetConsumedChecks();
    }

    /**
     * Resets the last block (and passed checks).
     */
    public void resetLastBlock() {
        lastTick = 0;
        lastAction = null;
        lastX = Integer.MAX_VALUE;
        lastType = null;
        lastBlock = null;
        lastAllowUseBlock = false;
        lastAllowUseItem = false;
        lastIsCancelled = true;
        resetPassedChecks();
        resetConsumedChecks();
    }

    /**
     * Reset passed checks (concern the last block interacted with).
     */
    public void resetPassedChecks() {
        passedChecks.clear();
    }

    /**
     * Reset consumed checks (concern the last block interacted with).
     */
    public void resetConsumedChecks() {
        consumedChecks.clear();
    }

    /**
     * Full state comparison.
     * 
     * @param material
     *            null is treated as Material.AIR.
     * @param action
     * @param tick
     * @param block
     * @return
     */
    public boolean matchesLastBlock(final Material material, final Action action, final int tick, final Block block) {
        return lastX != Integer.MAX_VALUE && (material == lastType || material == null && lastType == Material.AIR) 
                && matchesLastBlock(action, tick, block);
    }

    /**
     * Compare action, tick and block.
     * 
     * @param action
     * @param tick
     * @param block
     * @return
     */
    public boolean matchesLastBlock(final Action action, final int tick, final Block block) {
        return lastX != Integer.MAX_VALUE && tick == lastTick && matchesLastBlock(action, block);
    }

    /**
     * Compare only action and coordinates.
     * 
     * @param action
     * @param block
     * @return
     */
    public boolean matchesLastBlock(final Action action, final Block block) {
        return lastX != Integer.MAX_VALUE && action == lastAction && matchesLastBlock(block);
    }

    /**
     * Compare only tick and coordinates.
     * 
     * @param tick
     * @param block
     * @return
     */
    public boolean matchesLastBlock(final int tick, final Block block) {
        return lastX != Integer.MAX_VALUE && tick == lastTick && matchesLastBlock(block);
    }

    /**
     * Compare the block coordinates.
     * 
     * @param block
     * @return
     */
    public boolean matchesLastBlock(final Block block) {
        // Skip the MAX_VALUE check.
        return lastX == block.getX() && lastY == block.getY() && lastZ == block.getZ();
    }

    /**
     * The Manhattan distance to the set last block, Integer.MAX_VALUE is
     * returned if none is set.
     * 
     * @param block
     * @return The Manhattan distance if appropriate - if no block is set,
     *         Integer.MAX_VALUE is returned.
     */
    public int manhattanLastBlock(final Block block) {
        return lastX == Integer.MAX_VALUE ? Integer.MAX_VALUE : TrigUtil.manhattan(lastX, lastY, lastZ,
                block.getX(), block.getY(), block.getZ());
    }

    public boolean getLastAllowUseItem() {
        return lastAllowUseItem;
    }

    public boolean getLastAllowUseBlock() {
        return lastAllowUseBlock;
    }

    public boolean getLastIsCancelled() {
        return lastIsCancelled;
    }

    /**
     * Return the type of the block last interacted with. This does not check
     * for invalidation.
     * 
     * @return
     */
    public Material getLastType() {
        return lastType;
    }

    /**
     * Return the block last interacted with.
     * 
     * @return
     */
    public Block getLastBlock() {
        return lastBlock;
    }

    /**
     * Get the tick of the last interaction with a block.
     * 
     * @return
     */
    public int getLastTick() {
        return lastTick;
    }

    /**
     * Get the action of the last interaction with a block.
     * 
     * @return
     */
    public Action getLastAction() {
        return lastAction;
    }

    /**
     * Set the check type to be passed for the last block.
     * 
     * @param checkType
     */
    public void addPassedCheck(final CheckType checkType) {
        passedChecks.add(checkType);
    }

    /**
     * Check if this check type was set as passed for the last block.
     * 
     * @param checkType
     * @return
     */
    public boolean isPassedCheck(final CheckType checkType) {
        return passedChecks.contains(checkType);
    }

    /**
     * 
     * @return Unmodifiable collection.
     */
    public Collection<CheckType> getPassedChecks() {
        return Collections.unmodifiableCollection(passedChecks);
    }

    /**
     * Check if the last block was set to be consumed by this check type.
     * 
     * @param checkType
     * @return
     */
    public boolean isConsumedCheck(final CheckType checkType) {
        return consumedChecks.contains(checkType);
    }

    /**
     * Set last block to be consumed by the given check type.
     * 
     * @param checkType
     */
    public void addConsumedCheck(final CheckType checkType) {
        consumedChecks.add(checkType);
    }

    /**
     * Adjust to the results of a BlockInteractEvent - only the results, the
     * coordinates and action is not updated.
     * 
     * @param event
     */
    public void setPlayerInteractEventResolution(final PlayerInteractEvent event) {
        if (event.isCancelled()) {
            // TODO: resetPassedChecks() ?
            lastIsCancelled = true;
            lastAllowUseItem = event.useItemInHand() == Result.ALLOW;
            lastAllowUseBlock = event.useInteractedBlock() == Result.ALLOW;
            subsequentCancel ++;
        }
        else {
            lastIsCancelled = false;
            lastAllowUseItem = event.useItemInHand() != Result.DENY;
            lastAllowUseBlock = event.useInteractedBlock() != Result.DENY;
            subsequentCancel = 0;
        }
    }

    /**
     * Allow overriding the last PlayerInteractEvent resolution.
     * 
     * @param isCancelled
     */
    public void setLastIsCancelled(final boolean isCancelled) {
        this.lastIsCancelled = isCancelled;
    }

    /**
     * Allow overriding the last PlayerInteractEvent resolution.
     * 
     * @param allowUseItem
     */
    public void setLastAllowUseItem(final boolean allowUseItem) {
        this.lastAllowUseItem = allowUseItem;
    }

    /**
     * Allow overriding the last PlayerInteractEvent resolution.
     * 
     * @param allowUseBlock
     */
    public void setLastAllowUseBlock(final boolean allowUseBlock) {
        this.lastAllowUseBlock = allowUseBlock;
    }
}