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
package fr.neatmonster.nocheatplus.utilities.location;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache.IBlockCacheNode;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

/**
 * A location with an entity with a lot of extra stuff.
 * 
 * @author asofold
 *
 */
public class RichEntityLocation extends RichBoundsLocation {
    
    /*
     * TODO: HumanEntity default with + height (1.11.2): elytra 0.6/0.6,
     * sleeping 0.2/0.2, sneaking 0.6/1.65, normal 0.6/1.8 - head height is 0.4
     * with elytra, 0.2 with sleeping, height - 0.08 otherwise.
     */

    /** The mc access. */
    // Final members //
    private final IHandle<MCAccess> mcAccess;


    // Simple members //

    /** Full bounding box width. */
    /*
     * TODO: This is the entity width, happens to usually be the bounding box
     * width +-. Move to entity / replace.
     */
    private double width; 

    /** Some entity collision height. */
    private double height; // TODO: Move to entity / replace.

    /** Indicate that this is a living entity. */
    private boolean isLiving;

    /** Living entity eye height, otherwise same as height.*/
    private double eyeHeight;

    /**
     * Entity is on ground, due to standing on an entity. (Might not get
     * evaluated if the player is on ground anyway.)
     */
    private boolean standsOnEntity = false;


    // "Heavy" object members that need to be set to null on cleanup. //

    /** The entity. */
    private Entity entity = null;


    /**
     * Instantiates a new rich entity location.
     *
     * @param mcAccess
     *            the mc access
     * @param blockCache
     *            BlockCache instance, may be null.
     */
    public RichEntityLocation(final IHandle<MCAccess> mcAccess, final BlockCache blockCache) {
        super(blockCache);
        this.mcAccess = mcAccess;
    }

    /**
     * Gets the width.
     *
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    /**
     * Gets the height.
     *
     * @return the height
     */
    public double getHeight() {
        return height;
    }

    /**
     * Gets the eye height.
     *
     * @return the eye height
     */
    public double getEyeHeight() {
        return eyeHeight;
    }

    /**
     * Gets the entity.
     *
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Test if this is a LivingEntity instance.
     *
     * @return true, if is living
     */
    public boolean isLiving() {
        return isLiving;
    }

    /**
     * Retrieve the currently registered MCAccess instance.
     *
     * @return the MC access
     */
    public MCAccess getMCAccess() {
        return mcAccess.getHandle();
    }

    /**
     * Get the internally stored IHandle instance for retrieving the currently
     * registered instance of MCAccess.
     * 
     * @return
     */
    public IHandle<MCAccess> getMCAccessHandle() {
        return mcAccess;
    }
    
    /**
     * @return true, if is in a water logged block.
     */
    public boolean isInWaterLogged() {
    	final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        boolean res = super.isInWaterLogged();
        if (p != null && res && pData.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
        	// Waterlogged blocks don't exist for older clients.
            res = inWaterLogged = false;
        }
        return res;
    }

    /**
     * Check if the location is in lava, accounting for version-dependant subtleties.
     * 
     * @return true, if the player is in lava
     */
    public boolean isInLava() {
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        // 1.13 and below clients use this no-sense method to check for lava collision... 
        // 1.8 client, Entity.java -> handleLavaMovement() -> isMaterialInBB in World.java
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
            // Force-override the inLava result from RichBoundsLocation.
            inLava = false;
            double[] aaBB = getAABBCopy();
            int iMinX = MathUtil.floor(aaBB[0] + 0.1);
            int iMaxX = MathUtil.floor(aaBB[3] - 0.1 + 1.0);
            int iMinY = MathUtil.floor(aaBB[1] + 0.4);
            int iMaxY = MathUtil.floor(aaBB[4] - 0.4 + 1.0);
            int iMinZ = MathUtil.floor(aaBB[2] + 0.1);
            int iMaxZ = MathUtil.floor(aaBB[5] - 0.1 + 1.0);
            for (int x = iMinX; x < iMaxX; x++) {
                for (int y = iMinY; y < iMaxY; y++) {
                    for (int z = iMinZ; z < iMaxZ; z++) {
                        final IBlockCacheNode node = blockCache.getOrCreateBlockCacheNode(x, y, z, false);
                        if ((BlockFlags.getBlockFlags(node.getType()) & BlockFlags.F_LAVA) != 0) {
                            inLava = true;
                            return inLava;
                        }
                    }
                }
            }
            // Did not collide.
            return inLava;
        }
        // Mojang tweaked lava collision in 1.14 to use the checkInsideBlocks method, like webs / berry bushes / powder snow etc...)
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_16) 
            && pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            // Force-override the inLava result from RichBoundsLocation
            inLava = false;
            double[] aaBB = getAABBCopy();
            inLava = BlockProperties.collides(blockCache, aaBB[0]+0.001, aaBB[1]+0.001, aaBB[2]+0.001, aaBB[3]-0.001, aaBB[4]-0.001, aaBB[5]-0.001, BlockFlags.F_LIQUID);
            return inLava;
        }
        // Not a legacy client, nothing to do.
        return super.isInLava();
    }

    /**
     * Checks if the location is in water using Minecraft collision logic.
     * 
     * @return true, if is in water
     */
    public boolean isInWater() {
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
            // 1.13 and below use this extra contraction for water collision.
            inWater = false;
            double extraContraction = 0.4;
            final int iMinX = MathUtil.floor(minX + 0.001);
            final int iMaxX = MathUtil.ceil(maxX - 0.001);
            final int iMinY = MathUtil.floor(minY + 0.001 + extraContraction); // + 0.001 <- Minecraft actually deflates the AABB by this amount.
            final int iMaxY = MathUtil.ceil(maxY - 0.001 - extraContraction);
            final int iMinZ = MathUtil.floor(minZ + 0.001);
            final int iMaxZ = MathUtil.ceil(maxZ - 0.001);
            // NMS collision method
            for (int iX = iMinX; iX < iMaxX; iX++) {
                for (int iY = iMinY; iY < iMaxY; iY++) {
                    for (int iZ = iMinZ; iZ < iMaxZ; iZ++) {
                        double liquidHeight = BlockProperties.getLiquidHeight(blockCache, iX, iY, iZ, BlockFlags.F_WATER);
                        double liquidHeightToWorld = iY + liquidHeight;
                        if (liquidHeightToWorld >= minY && liquidHeight != 0.0) {
                            // Collided.
                            inWater = true;
                            return inWater;
                        }
                    }
                }
            }
            // Did not collide, override the inWater flag.
            return inWater;
        }
        // Not a legacy client, return the result.
        return super.isInWater();
    }
    
    /**
     * Check if the entity is on a honey block including: cross-version checking and Mojang's 1.20 block-collision fix.
     * @return Whether the entity is on a honey block.
     */
    public boolean isOnHoneyBlock() {
        // NOTE: 0.001 is a magic value from NMS (Entity.java, checkInsideBlocks())
        if (onHoneyBlock != null) {
            return onHoneyBlock;
        }
        boolean res = super.isOnHoneyBlock();
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        if (res) {
            // Is the player actually in the block?
            if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
                // Legacy clients don't have such block.
                // This will allow "jumping" on it but won't solve legacy players "floating" mid air due to the honey block's lower height (ViaVersion maps it to slime, thus full collision box (1.0))
                // We'd need per-player blocks for such.
                res = onHoneyBlock = false;
            }
        }
        // TODO: 1.20 block collision fix.
        return res;
    }
    
    /**
     * Check if the entity is in soul sand including Mojang's 1.20 block-collision fix.
     * @return Whether the entity is in a soul sand block.
     */
    public boolean isInSoulSand() {
        // NOTE: 0.001 is a magic value from NMS (Entity.java, checkInsideBlocks())
        if (inSoulSand != null) {
            return inSoulSand;
        }
        boolean res = super.isInSoulSand();
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        // TODO: 1.20 block collision fix.
        return res;
    }

    /**
     * Check if the entity is in a berry bush, including cross-version checking.
     * @return Whether the entity is in a berry bush.
     */
    public boolean isInBerryBush() {
        if (inBerryBush != null) {
            return inBerryBush;
        }
        boolean res = super.isInBerryBush();
        final Player p = (Player) entity;
        if (res && p != null) {
            // Is the player actually in the block?
            final IPlayerData pData = DataManager.getPlayerData(p);
            if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
                // (Mapped to grass with viaver)
                res = inBerryBush = false;
            }
        }
        return res;
    }
    
    /**
     * Check if the entity is in powder snow, including NMS fall-distance checking.
     * @return Whether the entity is in powder snow.
     */
    public boolean isInPowderSnow() {
        // NOTE: 0.001 is a magic value from NMS (Entity.java, checkInsideBlocks())
        if (inPowderSnow != null) {
            return inPowderSnow;
        }
        boolean res = super.isInPowderSnow();
        final Player p = (Player) entity;
        // TODO: Handle powder snow bb contraction if falling with fall distance >2.5
        return res;
    }

    /**
     * From HoneyBlock.java 
     * Test if the player is sliding sideway with a honey block (NMS, checks for speed as well)
     * 
     * @return if the player is sliding on a honey block.
     */
    public boolean isSlidingDown() {
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        if (p == null) {
            return false;
        }
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
            // This mechanic was introduced in 1.15 alongside honey blocks
            return false;
        }
        if (isOnGround()) {
            // Not sliding, clearly.
            return false;
        }
        // With the current implementation, this condition is never run due to from.getBlockY(), it should be the location of the block not player's
        //if (from.getY() > from.getBlockY() + 0.9375D - 1.0E-7D) {
        //    // Too far from the block.
        //    return false;
        //} 
        if (thisMove.yDistance >= -Magic.DEFAULT_GRAVITY) {
            // Minimum speed.
            return false;
        }
        // With the current implementation, this condition will always return false, see above
        //double xDistanceToBlock = Math.abs((double)from.getBlockX() + 0.5D - from.getX());
        //double zDistanceToBlock = Math.abs((double)from.getBlockZ() + 0.5D - from.getZ());
        //double var7 = 0.4375D + (width / 2.0F);
        //return xDistanceToBlock + 1.0E-7D > var7 || zDistanceToBlock + 1.0E-7D > var7;
        collectBlockFlags(); // Do call here, else NPE for some places.
        if ((blockFlags & BlockFlags.F_STICKY) == 0) {
            return false;
        }
        // Finally, test for collision
        return isNextToBlock(0.01, BlockFlags.F_STICKY);
    }

    /**
     * Simple check with custom margins (Boat, Minecart). Does not update the
     * internally stored standsOnEntity field.
     *
     * @param yOnGround
     *            Margin below the player.
     * @param xzMargin
     *            the xz margin
     * @param yMargin
     *            Extra margin added below and above.
     * @return true, if successful
     */
    public boolean standsOnEntity(final double yOnGround, final double xzMargin, final double yMargin) {
        return blockCache.standsOnEntity(entity, minX - xzMargin, minY - yOnGround - yMargin, minZ - xzMargin, maxX + xzMargin, minY + yMargin, maxZ + xzMargin);
    }
    
    // /**
    //  * Check if the entity is on ground according to lost ground workarounds.
    //  * Assuming this gets called after the regular isOnGround (or isOnGroundOpportune) returns false.
    //  * @return true, if a lost ground workaround can apply.
    //  */
    // public boolean isOnGroundApproximate() {
    //     final Player p = (Player) entity;
    //     final IPlayerData pData = DataManager.getPlayerData(p);
    //     final MovingData data = pData.getGenericInstance(MovingData.class);
    //     final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
    //     final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
    //     return LostGround.lostGround(player, thisMove.from, thisMove.to, thisMove.hDistance, thisMove.yDistance, false, lastMove, data, cc, null, null);
    // 
    // }

    /**
     * Checks if the entity is on ground incluing special cases/properties such as powder snow and entities.
     * 
     * @return true, if the player is on ground
     */
    public boolean isOnGround() {
        if (onGround != null) {
            return onGround;
        }
        boolean res = super.isOnGround();
        Player p = (Player) entity;
        if (res) {
            // Is the player really on ground?
            if (p != null) {
                if (BlockProperties.isPowderSnow(getTypeId(blockX, Location.locToBlock(y - 0.01), blockZ))
                    && !BridgeMisc.hasLeatherBootsOn(p)) { 
                    res = onGround = false;
                    // Powder snow below without boots: no candidate for ground.
                    // Standing in between max and min isn't possible with or without boots.
                    // Standing on a block different than powder snow will fallback to the usual onGround logic (will return true).
                }
            }
        }   
        if (!res) {
            // Is the player actually off ground? 
            final double d1 = 0.25;
            if (blockCache.standsOnEntity(entity, minX - d1, minY - yOnGround, minZ - d1, maxX + d1, minY, maxZ + d1)) {
                // On ground due to an entity
                // TODO: Again, this check needs to be refined to be as close as possible to vanilla. With prediction, we cannot use a leniency magic value.
                res = onGround = standsOnEntity = true;
            }
            // TODO: Perhaps move all lostground stuff in here?
        }
        return res;
    }

    /**
     * Test if the player is just on ground due to standing on an entity.
     * 
     * @return True, if the player is not standing on blocks, but on an entity.
     */
    public boolean isOnGroundDueToStandingOnAnEntity() {
        return isOnGround() && standsOnEntity; // Just ensure it is initialized.
    }
    
    /**
     * Straw-man method to account for this specific bug: https://bugs.mojang.com/browse/MC-2404
     * Should not be used outside of its intended context (sneaking on edges), or if vanilla uses it.
     */
    public boolean isAboveGround() {
        Player player = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        double[] aaBBCopy = getAABBCopy();
        double yBelow = player.getFallDistance() - cc.sfStepHeight;
        return  isOnGround() 
                || pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) 
                && (
                    player.getFallDistance() < cc.sfStepHeight 
                    && BlockProperties.collides(blockCache, aaBBCopy[0], aaBBCopy[1]+yBelow, aaBBCopy[2], aaBBCopy[3], aaBBCopy[4]+yBelow, aaBBCopy[5], BlockFlags.SOLID_GROUND)
                )
            ;
    }
    
    /**
     * From EntityHuman.java <br>
     * Set the speed needed to back the player off from edges in the given vector.
     * This assumes that you have checked for preconditions already (!) <br>
     * [Pre-requisites are: the player must be _shifting_(not sneaking); must be above ground; must have negative or 0 y-distance; must not be flying]
     * 
     * @param Vector
     * @return the modified vector
     */
    public Vector maybeBackOffFromEdge(Vector vector) {
        Player player = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        double xDistance = vector.getX();
        double zDistance = vector.getZ();
        /** Parameter for searching for collisions below */
        double yBelow = pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_11) ? -cc.sfStepHeight : -1;
        double[] aaBBCopy = getAABBCopy();

        // Move AABB alongisde the X axis.
        boolean collidesX = BlockProperties.collides(blockCache, aaBBCopy[0]+xDistance, aaBBCopy[1]+yBelow, aaBBCopy[2], aaBBCopy[3]+xDistance, aaBBCopy[4]+yBelow, aaBBCopy[5], BlockFlags.SOLID_GROUND);
        while (xDistance != 0.0 && !collidesX) {
            if (xDistance < 0.05 && xDistance >= -0.05) {
                xDistance = 0.0;
            } 
            else if (xDistance > 0.0) {
                xDistance -= 0.05;
            } 
            else xDistance += 0.05;
        }
        // Move AABB alongside the Z axis.
        boolean collidesZ = BlockProperties.collides(blockCache, aaBBCopy[0], aaBBCopy[1]+yBelow, aaBBCopy[2]+zDistance, aaBBCopy[3], aaBBCopy[4]+yBelow, aaBBCopy[5]+zDistance, BlockFlags.SOLID_GROUND);
        while (zDistance != 0.0 && !collidesZ) {
            if (zDistance < 0.05 && zDistance >= -0.05) {
                zDistance = 0.0;
            } 
            else if (zDistance > 0.0) {
                zDistance -= 0.05;
            } 
            else zDistance += 0.05;
        }
        // Move AABB alongside both (diagonally)
        boolean collidesXZ = BlockProperties.collides(blockCache, aaBBCopy[0]+xDistance, aaBBCopy[1]+yBelow, aaBBCopy[2]+zDistance, aaBBCopy[3]+xDistance, aaBBCopy[4]+yBelow, aaBBCopy[5]+zDistance, BlockFlags.SOLID_GROUND);
        while (xDistance != 0.0 && zDistance != 0.0 && !collidesXZ) {
            if (xDistance < 0.05 && xDistance >= -0.05) {
                xDistance = 0.0;
            } 
            else if (xDistance > 0.0) {
                xDistance -= 0.05;
            } 
            else xDistance += 0.05;

            if (zDistance < 0.05 && zDistance >= -0.05) {
                zDistance = 0.0;
            } 
            else if (zDistance > 0.0) {
                zDistance -= 0.05;
            } 
            else zDistance += 0.05;
        }
        vector = new Vector(xDistance, 0.0, zDistance);
        return vector;
    }
    
    /**
     * How Minecraft calculates liquid pushing speed.
     * Can be found in: Entity.java, updateFluidHeightAndDoFluidPushing()
     * 
     * @param xDistance 
     * @param zDistance
     * @param liquidTypeFlag The flags F_LAVA or F_WATER to use.
     * @return A vector representing the pushing force (read as: speed) of the liquid.
     */
    public Vector getLiquidPushingVector(final double xDistance, final double zDistance, final long liquidTypeFlag) {
        final Player p = (Player) entity;
        if (p == null) {
            return new Vector();
        }
        final IPlayerData pData = DataManager.getPlayerData(p);
        if (isInLava() && pData.getClientVersion().isOlderThan(ClientVersion.V_1_16)) {
            // Lava pushes entities starting from the nether update (1.16+)
            return new Vector();
        }
        // No Location#locToBlock() here (!)
        // Contract bounding box.
        double extraContraction = pData.getClientVersion().isOlderThan(ClientVersion.V_1_13) ? 0.4 : 0.0;
        final int iMinX = MathUtil.floor(minX + 0.001);
        final int iMaxX = MathUtil.ceil(maxX - 0.001);
        final int iMinY = MathUtil.floor(minY + 0.001 + extraContraction);
        final int iMaxY = MathUtil.ceil(maxY - 0.001 - extraContraction);
        final int iMinZ = MathUtil.floor(minZ + 0.001);
        final int iMaxZ = MathUtil.ceil(maxZ - 0.001);
        double d2 = 0.0;
        Vector pushingVector = new Vector();
        int k1 = 0;
        // NMS collision method. We need to check for a second collision because of how Minecraft handles fluid pushing
        // (And we need the exact speed for predictions)
        for (int x = iMinX; x < iMaxX; x++) {
            for (int y = iMinY; y < iMaxY; y++) {
                for (int z = iMinZ; z < iMaxZ; z++) {
                    // LEGACY 1.13-
                    if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
                        double liquidHeight = BlockProperties.getLiquidHeight(blockCache, x, y, z, liquidTypeFlag);
                        if (liquidHeight != 0.0) {
                            double d0 = (float) (y + 1) - liquidHeight;
                            if (!p.isFlying() && iMaxY >= d0) {
                                // Collided
                                Vector flowVector = getFlowForceVector(x, y, z, liquidTypeFlag);
                                pushingVector.add(flowVector);
                            }
                        }
                    }
                    // MODERN 1.13+
                    else {
                        double liquidHeight = BlockProperties.getLiquidHeight(blockCache, x, y, z, liquidTypeFlag);
                        double liquidHeightToWorld = y + liquidHeight;
                        if (liquidHeightToWorld >= minY && liquidHeight != 0.0 && !p.isFlying()) {
                            // Collided.
                            d2 = Math.max(liquidHeightToWorld - minY, d2); // 0.001 is the Magic number the game uses to expand the box with newer versions.
                            // Determine pushing speed by using the current flow of the liquid.
                            Vector flowVector = getFlowForceVector(x, y, z, liquidTypeFlag);
                            if (d2 < 0.4) {
                                flowVector = flowVector.multiply(d2);
                            }
                            pushingVector = pushingVector.add(flowVector);
                            k1++ ;
                        }
                    }
                }
            }
        }
        // LEGACY
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
            if (isInWater() && pushingVector.lengthSquared() > 0.0) {
                pushingVector.normalize();
                pushingVector.multiply(0.014);
            }
        }
        // MODERN
        else {
            // In Entity.java:
            // LAVA: 0.0023333333333333335 if in any other world that isn't nether, 0.007 otherwise.
            // WATER: 0.014
            // NOTE: Water first then Lava (fixes issue with the player's box being both in water and in lava)
            double flowSpeedMultiplier = isInWater() ? 0.014 : (world.getEnvironment() == World.Environment.NETHER ? 0.007 : 0.0023333333333333335);
            if (pushingVector.lengthSquared() > 0.0) {
                if (k1 > 0) {
                   pushingVector = pushingVector.multiply(1.0 / k1);
                }
                if (p.isInsideVehicle()) {
                    // Normalize the vector anyway if inside liquid on a vehicle... (ease some work with the (future) vehicle rework)
                    pushingVector = pushingVector.normalize();
                }
                pushingVector = pushingVector.multiply(flowSpeedMultiplier); 
                if (Math.abs(xDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD 
                    && Math.abs(zDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD
                    && pushingVector.length() < 0.0045000000000000005) {
                    pushingVector = pushingVector.normalize().multiply(0.0045000000000000005);
                }
            }
        }
        return pushingVector;
    }
    
    // (Taken from Grim :p)
    private boolean affectsFlow(final BlockCache access, int x, int y, int z, int x1, int y1, int z1, final long liquidTypeFlag) {
        return BlockProperties.getLiquidHeight(access, x, y, z, liquidTypeFlag) == 0
               || BlockProperties.getLiquidHeight(access, x, y, z, liquidTypeFlag) > 0 
               && BlockProperties.getLiquidHeight(access, x1, y1, z1, liquidTypeFlag) > 0; 
    }
    
    // (Taken from Grim :p)
    private Vector normalizedVectorWithoutNaN(Vector vector) {
        double var0 = vector.length();
        return var0 < 1.0E-4 ? new Vector() : vector.multiply(1 / var0);
    }
    
    /**
     * Minecraft's function to calculate the liquid's flow force.
     * FlowingFluid.java, getFlow()
     * 
     * @param access
     * @param x
     * @param y
     * @param z
     * @return the vector, representing the liquid's flowing force.
     */
    public Vector getFlowForceVector(int x, int y, int z, final long liquidTypeFlag) {
        final Player p = (Player) entity;
        if (p == null) {
            return new Vector();
        }
        double xModifier = 0.0D;
        double zModifier = 0.0D;
        float liquidLevel = (float) BlockProperties.getLiquidHeight(blockCache, x, y, z, liquidTypeFlag); 
        for (BlockFace hDirection : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modX = x + hDirection.getModX();
            int modZ = z + hDirection.getModZ();
            if (affectsFlow(blockCache, x, y, z, modX, y, modZ, liquidTypeFlag)) {  
                float modLiquidHeight = (float) BlockProperties.getLiquidHeight(blockCache, modX, y, modZ, liquidTypeFlag); 
                float flowForce = 0.0F;
                if (modLiquidHeight == 0.0F) {
                    final IBlockCacheNode node = blockCache.getOrCreateBlockCacheNode(modX, y, modZ, false);
                    final Material matAtThisLoc = node.getType();
                    //          if (!var1.getBlockState(var8).getMaterial().blocksMotion()) { 
                    // NCP: Assumption: blocks that can block motion need to be considered ground and should be solid (with some exceptions)
                    if (!matAtThisLoc.isSolid()) { 
                        if (affectsFlow(blockCache, x, y, z, modX, y - 1, modZ, liquidTypeFlag)) {
                            modLiquidHeight = (float) BlockProperties.getLiquidHeight(blockCache, modX, y - 1, modZ, liquidTypeFlag); 
                            if (modLiquidHeight > 0.0F) {
                                flowForce = liquidLevel - (modLiquidHeight - 0.8888889f);
                            }
                        }
                    }
                } 
                else if (modLiquidHeight > 0.0f) {
                    flowForce = liquidLevel - modLiquidHeight;
                }
                if (flowForce != 0.0F) {
                    xModifier += (float) hDirection.getModX() * flowForce;
                    zModifier += (float) hDirection.getModZ() * flowForce;
                }
            }
        }
        // Compose the speed vector
        Vector flowingVector = new Vector(xModifier, 0.0, zModifier);
        /*IBlockCacheNode originalNode = blockCache.getOrCreateBlockCacheNode(x, y, z, false);
        if (BlockProperties.isLiquid(originalNode.getType()) && originalNode.getData(blockCache, x, y, z) >= 8) { // 8-15 - falling liquid
            for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isSolidFace((Player) entity, x, y, z, direction, liquidTypeFlag) || isSolidFace((Player) entity, x, y + 1, z, direction, liquidTypeFlag)) {
                    flowingVector = normalizedVectorWithoutNaN(flowingVector).add(new Vector(0.0D, -6.0D, 0.0D));
                    break;
                }
            }
        }*/
        return normalizedVectorWithoutNaN(flowingVector);
    }

    /**
     * Check if a player may climb upwards.<br>
     * Assuming this gets called after isOnClimbable returned true (with the player not moving from/to ground).<br>
     * Does not check for motion.
     *
     * @param jumpHeight
     *            Height the player is allowed to have jumped.
     * @return true, if successful
     */
    public boolean canClimbUp(double jumpHeight) {
        final Player p = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(p);
        if (pData != null && pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            // Since 1.14, all climbable blocks are climbable upwards, always.
            return true;
        }
        // Force legacy clients to behave with legacy mechanics.
        if (BlockProperties.needsToBeAttachedToABlock(getTypeId())) {
            // Check if vine is attached to something solid
            if (BlockProperties.canClimbUp(blockCache, blockX, blockY, blockZ)) {
                return true;
            }
            // Check the block at head height.
            final int headY = Location.locToBlock(maxY);
            if (headY > blockY) {
                for (int cy = blockY + 1; cy <= headY; cy ++) {
                    if (BlockProperties.canClimbUp(blockCache, blockX, cy, blockZ)) {
                        return true;
                    }
                }
            }
            // Finally check possible jump height.
            // TODO: This too is inaccurate.
            if (isOnGround(jumpHeight)) {
                // Here ladders are ok.
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Test if something solid/ground-like collides within the given margin
     * above the eye height of the player.
     *
     * @param marginAboveEyeHeight
     *            the margin above eye height
     * @return true, if is head obstructed
     */
    public boolean isHeadObstructed(double marginAboveEyeHeight) {
        return isHeadObstructed(marginAboveEyeHeight, true);
    }

    /**
     * Test if something solid/ground-like collides within the given margin
     * above the eye height of the player.
     *
     * @param marginAboveEyeHeight
     *            Must be greater than or equal zero.
     * @param stepCorrection
     *            If set to true, a correction method is used for leniency.
     * @return true, if is head obstructed
     * @throws IllegalArgumentException
     *             If marginAboveEyeHeight is smaller than 0.
     */
    public boolean isHeadObstructed(double marginAboveEyeHeight, boolean stepCorrection) {
        if (marginAboveEyeHeight < 0.0) {
            throw new IllegalArgumentException("marginAboveEyeHeight must be greater than 0.");
        }
        // Step correction: see https://github.com/NoCheatPlus/NoCheatPlus/commit/f22bf88824372de2207e6dca5e1c264f3d251897
        if (stepCorrection) {
            double obstrDistance = maxY + marginAboveEyeHeight;
            obstrDistance = obstrDistance - (double) Location.locToBlock(obstrDistance) + 0.35;
            for (double bound = 1.0; bound > 0.0; bound -= 0.25) {
                if (obstrDistance >= bound) {
                    // Use this level for correction.
                    marginAboveEyeHeight += bound + 0.35 - obstrDistance;
                    break;
                }
            }
        }
        return  BlockProperties.collides(blockCache, minX, maxY, minZ, maxX, maxY + marginAboveEyeHeight, maxZ, BlockFlags.F_GROUND | BlockFlags.F_SOLID)
                // (Is there a more elegant way to do this?)
                /* 
                 * Powder snow: hack around NCP not having per-player blocks / dynamic removal or addition of flags.
                 * This would always return true due to the block having the GROUND flag, but for the purpose of THIS check, powder snow cannot obstruct a player's head/jump (jumping with powder snow above will simply let the player go through it).
                 */
                && !BlockProperties.collides(blockCache, minX, maxY, minZ, maxX, maxY + marginAboveEyeHeight, maxZ, BlockFlags.F_POWDERSNOW)
                // Here the player's AABB would be INSIDE the block sideways(thus, the maxY's AABB would result as hitting the honey block above)
                && !isNextToBlock(0.01, BlockFlags.F_STICKY);
    }

    /**
     * Test if something solid/ground-like collides within a default
     * margin/estimation above the eye height of the player.
     *
     * @return true, if is head obstructed
     */
    public boolean isHeadObstructed() {
        return isHeadObstructed(0.0, true);
    }

    /**
     * Convenience constructor for using the maximum of mcAccess.getHeight() and
     * eye height for fullHeight.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Entity entity, final double yOnGround) {
        final MCAccess mcAccess = this.mcAccess.getHandle();
        doSet(location, entity, mcAccess.getWidth(entity), mcAccess.getHeight(entity), yOnGround);
    }

    /**
     * 
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullHeight
     *            Allows to specify eyeHeight here. Currently might be
     *            overridden by eyeHeight, if that is greater.
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Entity entity, double fullHeight, final double yOnGround) {
        final MCAccess mcAccess = this.mcAccess.getHandle();
        doSet(location, entity, mcAccess.getWidth(entity), fullHeight, yOnGround);
    }

    /**
     * 
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullWidth
     *            Override the bounding box width (full width).
     * @param fullHeight
     *            Allows to specify eyeHeight here. Currently might be
     *            overridden by eyeHeight, if that is greater.
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Entity entity, final double fullWidth, double fullHeight, final double yOnGround) {
        doSet(location, entity, fullWidth, fullHeight, yOnGround);
    }

    /**
     * Do set.<br>
     * For the bounding box height, the maximum of given fullHeight, eyeHeight
     * with sneaking ignored and entity height is used. Sets isLiving and
     * eyeHeight.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullWidth
     *            the full width
     * @param fullHeight
     *            the full height
     * @param yOnGround
     *            the y on ground
     */
    protected void doSet(final Location location, final Entity entity, final double fullWidth, double fullHeight, final double yOnGround) {
        final double eyeHeight;
        final boolean isLiving;
        if (entity instanceof LivingEntity) {
            isLiving = true;
            final LivingEntity living = (LivingEntity) entity;
            final IPlayerData pData = DataManager.getPlayerData((Player) entity);
            eyeHeight = living.getEyeHeight();
            // Mojang added a new mechanic in 1.14: crouching will now actually contract the bounding box of the player.
            // On 1.13 and 1.9 the player's bounding box can change as well due to swimming and gliding
            // 0.179999?
            // TODO: What's the 0.179 magic value?
            fullHeight = pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) || Bridge1_9.isGliding(living) ? eyeHeight + 0.179 : Math.max(Math.max(fullHeight, eyeHeight), living.getEyeHeight(true));
        }
        else {
            isLiving = false;
            eyeHeight = fullHeight;
        }
        doSetExactHeight(location, entity, isLiving, fullWidth, eyeHeight, fullHeight, fullHeight, yOnGround);
    }

    /**
     * 
     * @param location
     * @param entity
     * @param isLiving
     * @param fullWidth
     * @param eyeHeight
     * @param height
     *            Set as height (as in entity.height).
     * @param fullHeight
     *            Bounding box height.
     * @param yOnGround
     */
    protected void doSetExactHeight(final Location location, final Entity entity, final boolean isLiving, 
                                    final double fullWidth, final double eyeHeight, final double height, 
                                    final double fullHeight, final double yOnGround) {
        this.entity = entity;
        this.isLiving = isLiving;
        final MCAccess mcAccess = this.mcAccess.getHandle();
        this.width = mcAccess.getWidth(entity);
        this.eyeHeight = eyeHeight;
        this.height = mcAccess.getHeight(entity);
        standsOnEntity = false;
        super.set(location, fullWidth, fullHeight, yOnGround);
    }

    /**
     * Not supported.
     *
     * @param location
     *            the location
     * @param fullWidth
     *            the full width
     * @param fullHeight
     *            the full height
     * @param yOnGround
     *            the y on ground
     */
    @Override
    public void set(Location location, double fullWidth, double fullHeight, double yOnGround) {
        throw new UnsupportedOperationException("Set must specify an instance of Entity.");
    }

    /**
     * Set cached info according to other.<br>
     * Minimal optimizations: take block flags directly, on-ground max/min
     * bounds, only set stairs if not on ground and not reset-condition.
     *
     * @param other
     *            the other
     */
    public void prepare(final RichEntityLocation other) {
        super.prepare(other);
        this.standsOnEntity = other.standsOnEntity;
    }

    /**
     * Set some references to null.
     */
    public void cleanup() {
        super.cleanup();
        entity = null;
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.utilities.RichBoundsLocation#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(128);
        builder.append("RichEntityLocation(");
        builder.append(world == null ? "null" : world.getName());
        builder.append('/');
        builder.append(Double.toString(x));
        builder.append(", ");
        builder.append(Double.toString(y));
        builder.append(", ");
        builder.append(Double.toString(z));
        builder.append(')');
        return builder.toString();
    }

}
