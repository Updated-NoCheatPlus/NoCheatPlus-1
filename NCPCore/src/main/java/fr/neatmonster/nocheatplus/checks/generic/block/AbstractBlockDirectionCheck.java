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
package fr.neatmonster.nocheatplus.checks.generic.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.net.FlyingQueueHandle;
import fr.neatmonster.nocheatplus.checks.net.FlyingQueueLookBlockChecker;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.components.config.ICheckConfig;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.collision.CollideRayVsAABB;
import fr.neatmonster.nocheatplus.utilities.collision.ICollideRayVsAABB;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

/**
 * A first made-abstract version of a direction-towards-a-block-check.
 * 
 * @author asofold
 *
 * @param <D>
 * @param <C>
 */
public abstract class AbstractBlockDirectionCheck<D extends ICheckData, C extends ICheckConfig> extends Check {

    private final class BoulderChecker extends FlyingQueueLookBlockChecker {
        // (Not static for convenience.)

        private double minDistance;

        @Override
        protected boolean check(final double x, final double y, final double z, 
                final float yaw, final float pitch, 
                final int blockX, final int blockY, final int blockZ) {
            final double distance = checkBoulder(x, y, z, yaw, pitch, blockX, blockY, blockZ);
            if (distance == Double.MAX_VALUE) {
                // minDistance is not updated, in case the information is interesting ever.
                return true;
            }
            else {
                minDistance = Math.min(minDistance, distance);
                return false;
            }
        }

        @Override
        public boolean checkFlyingQueue(double x, double y, double z, float oldYaw, float oldPitch, int blockX,
                int blockY, int blockZ, FlyingQueueHandle flyingHandle) {
            minDistance = Double.MAX_VALUE;
            return super.checkFlyingQueue(x, y, z, oldYaw, oldPitch, blockX, blockY, blockZ, flyingHandle);
        }

        public double getMinDistance() {
            return minDistance;
        }

    }

    private final ICollideRayVsAABB boulder = new CollideRayVsAABB();
    private final Location useLoc = new Location(null, 0, 0, 0);

    private final BoulderChecker checker = new BoulderChecker();

    /**
     * Instantiates a new direction check.
     */
    public AbstractBlockDirectionCheck(CheckType checkType) {
        super(checkType);
    }

    /**
     * Add the distance to the VL, return the resulting violation level.
     * 
     * @param distance
     * @param data
     * @return
     */
    protected abstract double addVL(Player player, double distance, D data, C cc);

    /**
     * Fetch the actions from the config.
     * @param cc
     * @return
     */
    protected abstract ActionList getActions(C cc);


    /**
     * Called on passing the check (cooldown vl, debug log, ...).
     * 
     * @param player
     * @param datam
     * @param cc
     */
    protected abstract void cooldown(Player player, D data, C cc);

    /**
     * The actual checking method.
     * 
     * @param player
     * @param loc
     *            Foot location to use as base for checking, with looking
     *            direction set.
     * @param eyeHeight
     *            The eye height above the foot location.
     * @param block
     *            The block the player is supposed to be looking at.
     * @param flyingHandle
     * @param data
     * @param cc
     * @return True, if the check has been failed.
     */
    public boolean check(final Player player, final Location loc, final double eyeHeight, 
            final Block block, final BlockFace face, final FlyingQueueHandle flyingHandle, 
            final D data, final C cc, final IPlayerData pData) {

        boolean cancel = false;
        // How far "off" is the player with their aim.
        final double x = loc.getX();
        final double y = loc.getY() + eyeHeight;
        final double z = loc.getZ();
        final int blockX = block.getX();
        final int blockY = block.getY();
        final int blockZ = block.getZ();
        // The distance is squared initially.
        double distance;
        if (checker.checkFlyingQueue(x, y, z, loc.getYaw(), loc.getPitch(), 
                blockX, blockY, blockZ, flyingHandle)) {
            distance = Double.MAX_VALUE;
        }
        else {
            distance = checker.getMinDistance();
        }

        if (face != null && !isInteractable(loc, Location.locToBlock(y), block, face)) distance = 1.0;          

        // TODO: Consider a protected field with a tolerance value.
        if (distance != Double.MAX_VALUE) {
            distance = Math.sqrt(distance);
            if (pData.isDebugActive(type)) {
                outputDebugFail(player, boulder, distance);
            }

            // Add the overall violation level of the check.
            final double vl = addVL(player, distance, data, cc);

            // TODO: Set distance parameter.

            // Execute whatever actions are associated with this check and the violation level and find out if we should
            // cancel the event.
            cancel = executeActions(player, vl, distance, getActions(cc)).willCancel();
        } else {
            // Player did likely nothing wrong, reduce violation counter to reward them.
            cooldown(player, data, cc);
        }
        return cancel;
    }

    /**
     * Check one configuration.
     * 
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     * @param blockX
     * @param blockY
     * @param blockZ
     * @return Double.MAX_VALUE if this passes the check, otherwise the squared
     *         violation distance (some measure).
     */
    private double checkBoulder(final double x, final double y, final double z,
            final float yaw, final float pitch,
            final int blockX, final int blockY, final int blockZ) {
        useLoc.setYaw(yaw);
        useLoc.setPitch(pitch);
        final Vector dir = useLoc.getDirection(); // TODO: More efficient.
        final double dirX = dir.getX();
        final double dirY = dir.getY();
        final double dirZ = dir.getZ();
        // Initialize fully each time.
        boulder.setFindNearestPointIfNotCollide(true)
        .setRay(x, y, z, dirX, dirY, dirZ)
        .setAABB(blockX, blockY, blockZ, Bridge1_13.hasIsSwimming() ? 0.9 : 0.1)
        .loop();
        // Interpret result.
        if (boulder.collides()) {
            return Double.MAX_VALUE;
        }
        else {
            return boulder.getClosestDistanceSquared();
        }
    }

    private void outputDebugFail(Player player, ICollideRayVsAABB boulder, double distance) {
        debug(player, "Failed: collides: " + boulder.collides() + " , dist: " + distance + " , pos: " + LocUtil.simpleFormat(boulder));
    }

    /**
     * Check if interact with right direction block facing.
     * 
     * @param loc player location
     * @param locY yblock at eye location 
     * @param block interacted block
     * @param face interacted face
     * @return boolean if are allowed to interact the block with given direction
     */
    private static boolean isInteractable(final Location loc, final int locY, final Block block, final BlockFace face) {
        final int locX = loc.getBlockX();
        final int locZ = loc.getBlockZ();
        final int blockX = block.getX();
        final int blockZ = block.getZ();
        final int blockY = block.getY();
        if (locX == blockX && locZ == blockZ && locY == blockY) return true;

        final long blockflags = BlockProperties.getBlockFlags(block.getType());
        final boolean fullbounds = (blockflags & BlockProperties.F_HEIGHT100) != 0 && (blockflags & BlockProperties.F_XZ100) != 0;
        final List<BlockFace> interactablefaces = getInteractableFaces(locX - blockX, locZ - blockZ, locY - blockY, fullbounds);

        if (!interactablefaces.contains(face)) return false;

        return !isDirectionBlocked(block, interactablefaces, face, fullbounds);
    }

    /**
     * Get block faces player can intractable
     * 
     * @param xdiff different X_Axis from player to block
     * @param zdiff different Z_Axis from player to block
     * @param ydiff different Y_Axis from player eye to block
     * @param fullbounds is given block has full bounding boxes
     * @return List of block faces are allowed to touch 
     */
    private static List<BlockFace> getInteractableFaces(final int xdiff, final int zdiff, final int ydiff, final boolean fullbounds) {
        final List<BlockFace> faces = new ArrayList<BlockFace>(6);
        // Allow on sides if isn't interact with full_bounds block 
        if (!fullbounds) {
            if (xdiff == 0) {faces.add(BlockFace.EAST); faces.add(BlockFace.WEST);}
            if (zdiff == 0) {faces.add(BlockFace.SOUTH); faces.add(BlockFace.NORTH);}
        }
        if (ydiff == 0) {faces.add(BlockFace.UP); faces.add(BlockFace.DOWN);} else
            faces.add(ydiff > 0 ? BlockFace.UP : BlockFace.DOWN);
        if (xdiff != 0) faces.add(xdiff > 0 ? BlockFace.EAST : BlockFace.WEST);
        if (zdiff != 0) faces.add(zdiff > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
        return faces;
    }

    private static boolean isDirectionBlocked(Block block, List<BlockFace> interactablefaces, BlockFace tface, boolean hasfullbounds) {
    	if (hasfullbounds) {
    		final long blockRelativeflags = BlockProperties.getBlockFlags(block.getRelative(tface).getType());
    		return (blockRelativeflags & BlockProperties.F_IGN_PASSABLE) == 0
    	    && (blockRelativeflags & BlockProperties.F_HEIGHT100) != 0 && (blockRelativeflags & BlockProperties.F_XZ100) != 0;
    	} else
    	for (BlockFace face : interactablefaces) {
    		final long blockRelativeflags = BlockProperties.getBlockFlags(block.getRelative(face).getType());
    		final boolean relativefullbounds = (blockRelativeflags & BlockProperties.F_HEIGHT100) != 0 && (blockRelativeflags & BlockProperties.F_XZ100) != 0;
    		if (!relativefullbounds || (blockRelativeflags & BlockProperties.F_IGN_PASSABLE) == 0) return false;
    	}
    	return true;
    }
}
