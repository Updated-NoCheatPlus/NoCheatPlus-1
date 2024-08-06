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
package fr.neatmonster.nocheatplus.checks.moving;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.checks.moving.location.setback.DefaultSetBackStorage;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.TraceEntryPool;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.MoveConsistency;
import fr.neatmonster.nocheatplus.checks.moving.model.MoveTrace;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.VehicleMoveData;
import fr.neatmonster.nocheatplus.checks.moving.velocity.AccountEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.FrictionAxisVelocity;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleAxisVelocity;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.checks.workaround.WRPT;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeReference;
import fr.neatmonster.nocheatplus.components.data.IDataOnReload;
import fr.neatmonster.nocheatplus.components.data.IDataOnRemoveSubCheckData;
import fr.neatmonster.nocheatplus.components.data.IDataOnWorldUnload;
import fr.neatmonster.nocheatplus.components.entity.IEntityAccessDimensions;
import fr.neatmonster.nocheatplus.components.location.IGetPosition;
import fr.neatmonster.nocheatplus.components.location.IPositionWithLook;
import fr.neatmonster.nocheatplus.components.registry.IGetGenericInstance;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.workaround.IWorkaroundRegistry.WorkaroundSet;

/**
 * Player specific data for the moving checks.
 */
public class MovingData extends ACheckData implements IDataOnRemoveSubCheckData, IDataOnReload, IDataOnWorldUnload {

    //////////////////////////////////////////////
    // Violation levels                         //
    //////////////////////////////////////////////
    public double creativeFlyVL = 0.0;
    public double morePacketsVL = 0.0;
    public double noFallVL = 0.0;
    public double survivalFlyVL = 0.0;
    public double vehicleMorePacketsVL = 0.0;
    public double vehicleEnvelopeVL = 0.0;
    public double passableVL = 0.0;





    //////////////////////////////////////////////
    // Data shared between the moving checks    //
    //////////////////////////////////////////////
    /** Has leather boot on*/
    public boolean hasLeatherBoots = false;
    public double lastY = -64.0;
    /** Delay (in ticks) from jump to back on ground */
    public int jumpDelay;
    public double lastLevitationLevel;
    /** Count set back (re-) setting. */
    private int playerMoveCount = 0;
    /** setBackResetCount (incremented) at the time of (re-) setting the ordinary set back. */
    private int setBackResetTime = 0;
    /** setBackResetCount (incremented) at the time of (re-) setting the morepackets set back. */
    private int morePacketsSetBackResetTime = 0;
    /** Tick at which walk/fly speeds got changed last time. */
    public int speedTick = 0;
    /** Walk speed */
    public float walkSpeed = 0.0f;
    public float nextWalkSpeed = 0.0f;
    /** Fly speed */
    public float flySpeed = 0.0f;
    /** Keep track of the amplifier given by the jump potion. */
    public double jumpAmplifier = 0;
    /** Last time the player was riptiding */
    public long timeRiptiding = 0;
    /** Represents how long a vehicle has been tossed up by a bubble column */
    // TODO: Deprecate and use the blockChangeTracker, rather.
    public long timeVehicletoss = 0;
    /** Used as a workaround for boats leaving ice while still having velocity from ice */
    public int boatIceVelocityTicks = 0;
    public long timeCamelDash = 0;
    /** Last used block change id (BlockChangeTracker). */
    public final BlockChangeReference blockChangeRef = new BlockChangeReference();
    
    // *----------Speed/Friction factors (hor/ver)----------*
    /** Horizontal friction factor from NMS.*/
    // TODO: Might not good to set those at 0.0f, can cause NaN when /ncp removeplayer command execute
    public float lastFrictionHorizontal = 0.6f;
    /** Horizontal friction factor from NMS.*/
    public float nextFrictionHorizontal = 0.0f;
    /** Inertia: friction * 0.91 */
    public float lastInertia = 0.0f;
    /** Inertia: friction * 0.91 */
    public float nextInertia = 0.0f;
    /** Speed multiplier for blocks that can make the player stick/stuck to/into it (such as webs).*/
    public double lastStuckInBlockHorizontal = 1.0; 
    /** Speed multiplier for blocks that can make the player stick/stuck to/into it (such as webs).*/
    public double nextStuckInBlockHorizontal = 1.0; 
    /** Single block-speed multiplier.*/
    public float lastBlockSpeedMultiplier = 1.0f;
    /** Single block-speed multiplier.*/
    public float nextBlockSpeedMultiplier = 1.0f;
    /** Stuck-in-block vertical speed factor */
    public double nextStuckInBlockVertical = 0.0;
    /** Stuck-in-block vertical speed factor */
    public double lastStuckInBlockVertical = 0.0;
    /** Used during processing, no resetting necessary.*/
    public double nextFrictionVertical = 0.0;
    /** Ordinary vertical friction factor (lava, water, air) */
    public double lastFrictionVertical = 0.0;
    public double lastNonVanillaFrictionVertical = 0.0;
    public double nextNonVanillaFrictionVertical = 0.0;

    // *----------Move / Vehicle move tracking----------*
    /** Keep track of currently processed (if) and past moves for player moving. Stored moves can be altered by modifying the int. */
    public final MoveTrace <PlayerMoveData> playerMoves = new MoveTrace<PlayerMoveData>(new Callable<PlayerMoveData>() {
        @Override
        public PlayerMoveData call() throws Exception {
            return new PlayerMoveData();
        }
    }, 15); 
    /** Keep track of currently processed (if) and past moves for vehicle moving. Stored moves can be altered by modifying the int. */
    // TODO: There may be need to store such data with vehicles, or detect tandem abuse in a different way.
    public final MoveTrace <VehicleMoveData> vehicleMoves = new MoveTrace<VehicleMoveData>(new Callable<VehicleMoveData>() {
        @Override
        public VehicleMoveData call() throws Exception {
            return new VehicleMoveData();
        }
    }, 2);

    // *----------Velocity handling----------* 
    /** Tolerance value for using vertical velocity (the client sends different values than received with fight damage). */
    private static final double TOL_VVEL = 0.0625; // Result of minimum gravity + 0.0001
    /** Vertical velocity modeled as an axis (positive and negative possible) */
    private final SimpleAxisVelocity verVel = new SimpleAxisVelocity();
    /** Horizontal velocity modeled as an axis (always positive) */
    private final FrictionAxisVelocity horVel = new FrictionAxisVelocity();
    /** Whether the calculated explosion velocity should be applied. */
    public boolean shouldApplyExplosionVelocity = false;
    /** Velocity explosion counter (X). */
    public double explosionVelAxisX = 0.0;
    /** Velocity explosion counter (Y). */
    public double explosionVelAxisY = 0.0;
    /** Velocity explosion counter (Z). */
    public double explosionVelAxisZ = 0.0;
    /** Compatibility entry for bouncing off of slime blocks and the like. */
    public SimpleEntry verticalBounce = null;

    // *----------Coordinates----------*
    /** Moving trace (to-positions, use ms as time). This is initialized on "playerJoins, i.e. MONITOR, and set to null on playerLeaves." */
    private final LocationTrace trace;
    /** Setback location, shared between fly checks */
    private Location setBack = null;
    /** Telepot location, shared between fly checks */
    private Location teleported = null;
    /** Workaround for Folia servers missing the PlayerChangeWorld event: world is set on PlayerMoveEvents, lowest priority */
    public World fromMissedWorldChange = null;





    //////////////////////////////////////////////
    // Check specific data                      //
    //////////////////////////////////////////////
    // *----------Data of the CreativeFly check----------*
    /** Duration of the boost effect in ticks. Set in the BlockInteractListener. */
    public int fireworksBoostDuration = 0;
    /** This firework boost tick needs to be checked. Aimed at solving vanilla bugs when boosting with elytra. */
    public int fireworksBoostTickNeedCheck = 0;
    /** Expire at this tick. */
    public int fireworksBoostTickExpire = 0;

    // *----------Data of the MorePackets check----------*
    /** Packet frequency count. */
    public final ActionFrequency morePacketsFreq;
    /** Burst  count. */
    public final ActionFrequency morePacketsBurstFreq;
    /** Setback for MP. */
    private Location morePacketsSetback = null;

    // *----------Data of the NoFall check----------*
    /** The fall distance calculated by NCP */
    public float noFallFallDistance = 0;
    /** Last y coordinate from when the player was on ground. */
    public double noFallMaxY = 0;
    /** Indicate that NoFall is not to use next damage event for checking on-ground properties. */ 
    public boolean noFallSkipAirCheck = false;
    /** Last coordinate from when the player was affected wind charge explosion */
    public Location noFallCurrentLocOnWindChargeHit = null;

    // *----------Data of the SurvivalFly check----------*
    /** Default lift-off envelope, used after resetting. <br> TODO: Test, might be better ground. */
    private static final LiftOffEnvelope defaultLiftOffEnvelope = LiftOffEnvelope.NORMAL;
    /** playerMoveCount at the time of the last sf violation. */
    public int sfVLMoveCount = 0;
    /** Count in air events for this jumping phase, resets when landing on ground, with set-backs and similar. */
    public int sfJumpPhase = 0;
    /** "Dirty" flag, for receiving velocity and similar while in air. */
    @Deprecated
    private boolean sfDirty = false;
    /** Basic envelope constraints/presets for lifting off ground. */
    public LiftOffEnvelope liftOffEnvelope = defaultLiftOffEnvelope;
    /** Count how many moves have been made inside a medium (other than air). */
    public int insideMediumCount = 0;
    /** Counting while the player is not on ground and not moving. A value < 0 means not hovering at all. */
    public int sfHoverTicks = -1;
    /** First count these down before incrementing sfHoverTicks. Set on join, if configured so. */
    public int sfHoverLoginTicks = 0;
    /** Fake in air flag: set with any violation, reset once on ground. */
    public boolean sfVLInAir = false;
    /** Workarounds (AirWorkarounds,LiquidWorkarounds). */
    public final WorkaroundSet ws;
    /** Will be set to true on BedEnterEvent, then checked for on BedLeaveEvent. */
    public boolean wasInBed = false;

    // *----------Data of the vehicles checks----------*
    /** Default value for the VehicleMP buffer. */
    public static final int vehicleMorePacketsBufferDefault = 50;
    /** The buffer used for VehicleMP violations. */
    public int vehicleMorePacketsBuffer = vehicleMorePacketsBufferDefault;
    /** TODO: */
    public long vehicleMorePacketsLastTime;
    /** Task id of the vehicle set back task. */ 
    public Object vehicleSetBackTaskId = null;
    /** Task id of the passenger set back task. */ 
    public Object vehicleSetPassengerTaskId = null;
    




    //////////////////////////////////////////////
    // HOT FIX / WORKAROUNDS                    //
    //////////////////////////////////////////////
    /** 
     * Set to true after login/respawn, only if the set back is reset there. Reset in MovingListener after handling PlayerMoveEvent.
     * For more details see: https://github.com/Updated-NoCheatPlus/NoCheatPlus/commit/6d6f908512543f6289b51bb4c60a1940bcea9d4d 
     */
    public boolean joinOrRespawn = false;
    /** Number of (player/vehicle) move events since set.back. Update after running standard checks on that EventPriority level (not MONITOR). */
    public int timeSinceSetBack = 0;
    /** Location hash value of the last (player/vehicle) set back, for checking independently of which set back location had been used. */
    public int lastSetBackHash = 0;
    /** Position teleported from into another world. Only used for certain contexts for workarounds. */
    public IPositionWithLook crossWorldFrom = null;
    /** Indicates that this PlayerMoveEvent has no movement change (FROM and TO have the same position), because of the client re-sending a position packet on right-clicking (effectively duplicating the move). */
    public boolean lastMoveNoMove = false;

    // *----------Vehicles----------*
    /** Inconsistency-flag. Set on moving inside of vehicles, reset on exiting properly. Workaround for VehicleLeaveEvent missing. */
    public boolean wasInVehicle = false; 
    /** on 1.19.4+, skip the first PlayerMoveEvent fired after exiting a minecart. */
    public boolean vehicleLeave = false;
    /** TODO: */
    public EntityType lastVehicleType = null;
    /** Set to indicate that events happen during a vehicle set back. Allows skipping some resetting. */
    public boolean isVehicleSetBack = false;
    /** TODO:  */
    public MoveConsistency vehicleConsistency = MoveConsistency.INCONSISTENT;
    /** TODO: */
    public final DefaultSetBackStorage vehicleSetBacks = new DefaultSetBackStorage();


    private final IPlayerData pData;
    public MovingData(final MovingConfig config, final IPlayerData pData) {
        this.pData = pData;
        morePacketsFreq = new ActionFrequency(config.morePacketsEPSBuckets, 500);
        morePacketsBurstFreq = new ActionFrequency(12, 5000);
        // Location trace.
        trace = new LocationTrace(config.traceMaxAge, config.traceMaxSize, NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(TraceEntryPool.class));
        // A new set of workaround conters.
        ws = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(WRPT.class).getWorkaroundSet(WRPT.WS_MOVING);
    }


    /**
     * Clear fly and more packets check data for both vehicles and players.
     */
    public void clearMostMovingCheckData() {
        clearFlyData();
        clearVehicleData();
        clearAllMorePacketsData();
    }


    /**
     * Clear vehicle related data, except more packets.
     */
    public void clearVehicleData() {
        // TODO: Not entirely sure what to do here.
        vehicleMoves.invalidate();
        vehicleSetBacks.invalidateAll();
    }


    /**
     * Clear the data of the fly checks (not more-packets).
     */
    public void clearFlyData() {
        playerMoves.invalidate();
        jumpDelay = 0;
        sfJumpPhase = 0;
        jumpAmplifier = 0;
        setBack = null;
        clearNoFallData();
        removeAllPlayerSpeedModifiers();
        clearWindChargeImpulse();
        sfHoverTicks = sfHoverLoginTicks = -1;
        sfDirty = false;
        liftOffEnvelope = defaultLiftOffEnvelope;
        insideMediumCount = 0;
        vehicleConsistency = MoveConsistency.INCONSISTENT;
        verticalBounce = null;
        blockChangeRef.valid = false;
        // Set to 1.0 to prevent any unintentional / by 0. These are mainly used within multiplication or division operations anyway.
        lastFrictionVertical = lastStuckInBlockVertical = lastStuckInBlockHorizontal = 1.0;
        lastFrictionHorizontal = lastBlockSpeedMultiplier = 1.0f;
        lastInertia = 0.0f;
    }


    /**
     * On confirming a set back (teleport monitor / move start point): Mildly
     * reset the flying data without losing any important information. Past move
     * is adjusted to the given setBack, internal setBack is only updated, if
     * none is set.
     * 
     * @param setBack
     */
    public void onSetBack(final PlayerLocation setBack, final Location loc, final MovingConfig cc, final Player player) {
        // Reset positions (a teleport should follow, though).
        this.morePacketsSetback = null;
        // Keep no-fall data.
        // Fly data: problem is we don't remember the settings for the set back location.
        // Assume the player to start falling from there rather, or be on ground.
        // TODO: Check if to adjust some counters to state before setback? 
        // Keep jump amplifier
        // keep jump phase.
        sfHoverTicks = -1; // 0 ?
        sfDirty = false;
        liftOffEnvelope = defaultLiftOffEnvelope;
        insideMediumCount = 0;
        removeAllPlayerSpeedModifiers();
        vehicleConsistency = MoveConsistency.INCONSISTENT; // Not entirely sure here.
        verticalBounce = null;
        timeSinceSetBack = 0;
        lastFrictionVertical = lastStuckInBlockVertical = lastStuckInBlockHorizontal = 1.0;
        lastFrictionHorizontal = lastBlockSpeedMultiplier =  1.0f;
        lastInertia = 0.0f;
        lastSetBackHash = setBack == null ? 0 : setBack.hashCode();
        // Reset to setBack.
        resetPlayerPositions(setBack);
        adjustLiftOffEnvelope(setBack);
        adjustMediumProperties(loc, cc, player, playerMoves.getCurrentMove());
        // Only setSetBack if no set back location is there.
        if (setBack == null) {
            setSetBack(setBack);
        }
        // vehicleSetBacks.resetAllLazily(setBack); // Not good: Overrides older set back locations.
    }


    /**
     * Move event: Mildly reset some data, prepare setting a new to-Location.
     */
    public void prepareSetBack(final Location loc) {
        playerMoves.invalidate();
        vehicleMoves.invalidate();
        sfJumpPhase = 0;
        verticalBounce = null;
        // Remember where we send the player to.
        setTeleported(loc);
        // TODO: sfHoverTicks ?
    }


    /**
     * Adjust medium properties according to the medium.
     * @param loc
     * @param cc
     * @param player
     */
    public void adjustMediumProperties(final Location loc, final MovingConfig cc, final Player player, final PlayerMoveData thisMove) {
        nextFrictionHorizontal = BlockProperties.getBlockFrictionFactor(player, loc, cc.yOnGround, thisMove);
        nextStuckInBlockHorizontal = BlockProperties.getStuckInBlockHorizontalFactor(player, loc, cc.yOnGround, thisMove);
        nextBlockSpeedMultiplier = BlockProperties.getBlockSpeedFactor(player, loc, cc.yOnGround, thisMove);
        nextFrictionVertical = BlockProperties.getVerticalFrictionFactor(player, loc, cc.yOnGround, thisMove);
        nextStuckInBlockVertical = BlockProperties.getStuckInBlockVerticalFactor(player, loc, cc.yOnGround, thisMove);
        nextNonVanillaFrictionVertical = BlockProperties.getNonVanillaVerticalFrictionFactor(player, loc, cc.yOnGround, thisMove);
    }


    /**
     * Adjust lift off envelope for the player, called on set back and
     * similar. <br>
     * @param loc
     */
    public void adjustLiftOffEnvelope(final PlayerLocation loc) {
        // Ensure block flags have been collected.
        loc.collectBlockFlags();
        // Simplified.
        if (loc.isInWeb()) {
            liftOffEnvelope = LiftOffEnvelope.LIMIT_WEBS;
        }
        else if (loc.isInBerryBush()) {
            liftOffEnvelope = LiftOffEnvelope.LIMIT_SWEET_BERRY;
        }
        else if (loc.isInPowderSnow()) {
            liftOffEnvelope = LiftOffEnvelope.LIMIT_POWDER_SNOW;
        }
        else if (loc.isOnHoneyBlock()) {
            liftOffEnvelope = LiftOffEnvelope.LIMIT_HONEY_BLOCK;
        }
        else if (loc.isInLiquid()) {
            liftOffEnvelope = LiftOffEnvelope.LIMIT_LIQUID;
        }
        else if (loc.isOnGround()) {
            liftOffEnvelope = LiftOffEnvelope.NORMAL;
        }
        else liftOffEnvelope = LiftOffEnvelope.UNKNOWN;
        insideMediumCount = 0;
    }


    /**
     * Called when a player leaves the server.
     */
    public void onPlayerLeave() {
        removeAllPlayerSpeedModifiers();
        trace.reset();
        playerMoves.invalidate();
        vehicleMoves.invalidate();
    }


    /**
     * Invalidate all past player moves data and set last position if not null.
     * 
     * @param loc
     */
    public void resetPlayerPositions(final PlayerLocation loc) {
        resetPlayerPositions();
        if (loc != null) {
            final PlayerMoveData lastMove = playerMoves.getFirstPastMove();
            // Always set with extra properties.
            lastMove.setWithExtraProperties(loc);
        }
    }


    /**
     * Invalidate all past moves data (player).
     */
    private void resetPlayerPositions() {
        playerMoves.invalidate();
        sfDirty = false;
        liftOffEnvelope = defaultLiftOffEnvelope;
        insideMediumCount = 0;
        verticalBounce = null;
        blockChangeRef.valid = false;
        lastFrictionVertical = lastStuckInBlockVertical = lastStuckInBlockHorizontal = 1.0;
        lastFrictionHorizontal = 0.6f;
        lastBlockSpeedMultiplier = 1.0f;
        lastInertia = 0.0f;
        lastNonVanillaFrictionVertical = 0.0;
        // TODO: other buffers ?
        // No reset of vehicleConsistency.
    }


    /**
     * Invalidate all past vehicle moves data and set last position if not null.
     * 
     * @param loc
     */
    public void resetVehiclePositions(final RichEntityLocation loc) {
        // TODO: Other properties (convenience, e.g. set back?) ?
        vehicleMoves.invalidate();
        if (loc != null) {
            final VehicleMoveData lastMove = vehicleMoves.getFirstPastMove();
            // Always set with extra properties.
            lastMove.setWithExtraProperties(loc);
            final Entity entity = loc.getEntity();
            lastMove.vehicleId = entity.getUniqueId();
            lastMove.vehicleType = entity.getType();
        }
    }


    /**
     * Clear the data of the more packets checks, both for players and vehicles.
     */
    public void clearAllMorePacketsData() {
        clearPlayerMorePacketsData();
        clearVehicleMorePacketsData();
    }


    public void clearPlayerMorePacketsData() {
        morePacketsSetback = null;
        final long now = System.currentTimeMillis();
        morePacketsFreq.clear(now);
        morePacketsBurstFreq.clear(now);
        // TODO: Also reset other data ?
    }


    /**
     * Reduce the morepackets frequency counters by the given amount, capped at
     * a minimum of 0.
     * 
     * @param amount
     */
    public void reducePlayerMorePacketsData(final float amount) {
        ActionFrequency.reduce(System.currentTimeMillis(), amount, morePacketsFreq, morePacketsBurstFreq);
    }


    public void clearVehicleMorePacketsData() {
        vehicleMorePacketsLastTime = 0;
        vehicleMorePacketsBuffer = vehicleMorePacketsBufferDefault;
        vehicleSetBacks.getMidTermEntry().setValid(false); // TODO: Will have other resetting conditions later on.
        // TODO: Also reset other data ?
    }


    /**
     * Clear the data of the NoFall check.
     */
    public void clearNoFallData() {
        noFallFallDistance = 0;
        noFallMaxY = BlockProperties.getMinWorldY();
        noFallSkipAirCheck = false;
    }
    
    public void clearWindChargeImpulse() {
        noFallCurrentLocOnWindChargeHit = null;
    }


    /**
     * Set the set back location, this will also adjust the y-coordinate for some block types (at least air).
     * @param loc
     */
    public void setSetBack(final PlayerLocation loc) {
        if (setBack == null) {
            setBack = loc.getLocation();
        }
        else {
            LocUtil.set(setBack, loc);
        }
        // TODO: Consider adjusting the set back-y here. Problem: Need to take into account for bounding box (collect max-ground-height needed).
        setBackResetTime = playerMoveCount;
    }


    /**
     * Convenience method.
     * 
     * @param loc
     */
    public void setSetBack(final Location loc) {
        if (setBack == null) {
            setBack = LocUtil.clone(loc);
        }
        else {
            LocUtil.set(setBack, loc);
        }
        setBackResetTime = playerMoveCount;
    }


    /**
     * Get the set back location with yaw and pitch set form ref.
     * 
     * @param ref
     * @return
     */
    public Location getSetBack(final Location ref) {
        return LocUtil.clone(setBack, ref);
    }


    /**
     * Get the set back location with yaw and pitch set from ref.
     * 
     * @param ref
     * @return
     */
    public Location getSetBack(final PlayerLocation ref) {
        return LocUtil.clone(setBack, ref);
    }


    /**
     * Get the set back location with yaw and pitch set from the given
     * arguments.
     * 
     * @param refYaw
     * @param refPitch
     * @return
     */
    public Location getSetBack(final float yaw, final float pitch) {
        return LocUtil.clone(setBack, yaw, pitch);
    }


    public boolean hasSetBack() {
        return setBack != null;
    }


    public boolean hasSetBackWorldChanged(final Location loc) {
        if (setBack == null) {
            return true;
        }
        else {
            return setBack.getWorld().equals(loc.getWorld());
        }
    }


    public double getSetBackX() {
        return setBack.getX();
    }


    public double getSetBackY() {
        return hasSetBack() ? setBack.getY() : 0.0;
    }


    public double getSetBackZ() {
        return setBack.getZ();
    }


    public void setSetBackY(final double y) {
        setBack.setY(y);
        // (Skip setting/increasing the reset count.)
    }


    /**
     * Test, if the 'teleported' location is set, e.g. on a scheduled set back.
     * 
     * @return
     */
    public boolean hasTeleported() {
        return teleported != null;
    }


    /**
     * Return a copy of the teleported-to Location.
     * @return
     */
    public final Location getTeleported() {
        // TODO: here a reference might do.
        return teleported == null ? teleported : LocUtil.clone(teleported);
    }


    /**
     * Check if the given location equals to the 'teleported' (set back)
     * location.
     * 
     * @param loc
     * @return In case of either loc or teleported being null, false is
     *         returned, otherwise teleported.equals(loc).
     */
    public boolean isTeleported(final Location loc) {
        return loc != null && teleported != null && teleported.equals(loc);
    }


    /**
     * Check if the given location has the same coordinates like the
     * 'teleported' (set back) location. This is more light-weight and more
     * lenient than isTeleported, because world and yaw and pitch are all
     * ignored.
     * 
     * @param loc
     * @return In case of either loc or teleported being null, false is
     *         returned, otherwise TrigUtil.isSamePos(teleported, loc).
     */
    public boolean isTeleportedPosition(final Location loc) {
        return loc != null && teleported != null && TrigUtil.isSamePos(teleported, loc);
    }


    /**
     * Check if the given location has the same coordinates like the
     * 'teleported' (set back) location. This is more light-weight and more
     * lenient than isTeleported, because world and yaw and pitch are all
     * ignored.
     * 
     * @param loc
     * @return In case of either loc or teleported being null, false is
     *         returned, otherwise TrigUtil.isSamePos(pos, teleported).
     */
    public boolean isTeleportedPosition(final IGetPosition pos) {
        return pos != null && teleported != null && TrigUtil.isSamePos(pos, teleported);
    }


    /**
     * Set teleport-to location to recognize NCP set backs. This copies the coordinates and world.
     * @param loc
     */
    public final void setTeleported(final Location loc) {
        teleported = LocUtil.clone(loc); // Always overwrite.
    }


    public boolean hasMorePacketsSetBack() {
        return morePacketsSetback != null;
    }


    /**
     * Test if the morepackets set back is older than the ordinary set back.
     * Does not check for existence of either.
     * 
     * @return
     */
    public boolean isMorePacketsSetBackOldest() {
        return morePacketsSetBackResetTime < setBackResetTime;
    }


    public void setMorePacketsSetBackFromSurvivalfly() {
        setMorePacketsSetBack(setBack);
    }


    public final void setMorePacketsSetBack(final PlayerLocation loc) {
        if (morePacketsSetback == null) {
            morePacketsSetback = loc.getLocation();
        }
        else {
            LocUtil.set(morePacketsSetback, loc);
        }
        morePacketsSetBackResetTime = playerMoveCount;
    }


    public final void setMorePacketsSetBack(final Location loc) {
        if (morePacketsSetback == null) {
            morePacketsSetback = LocUtil.clone(loc);
        }
        else {
            LocUtil.set(morePacketsSetback, loc);
        }
        morePacketsSetBackResetTime = playerMoveCount;
    }


    public Location getMorePacketsSetBack() {
        return LocUtil.clone(morePacketsSetback);
    }


    public final void resetTeleported() {
        teleported = null;
    }


    /**
     * Set set back location to null.
     */
    public final void resetSetBack() {
        setBack = null;
    }


    /**
     * Remove/reset all speed modifier tracking, like vertical and horizontal
     * velocity, elytra boost, buffer.
     */
    private void removeAllPlayerSpeedModifiers() {
        // Velocity
        removeAllVelocity();
        // Elytra boost best fits velocity / effects.
        fireworksBoostDuration = 0; 
        fireworksBoostTickExpire = 0;
    }
    


    ///////////////////////////////////////
    // Velocity 
    ///////////////////////////////////////
    /**
     * Add velocity to internal book-keeping.
     * 
     * @param player
     * @param data
     * @param cc
     * @param vx
     * @param vy
     * @param vz
     * @param flags
     *            Flags to use with velocity entries.
     */
    public void addVelocity(final Player player, final MovingConfig cc, final double vx, final double vy, final double vz, final long flags) {
        final int tick = TickTask.getTick();
        // TODO: Slightly odd to call this each time, might switch to a counter-strategy (move - remove). 
        removeInvalidVelocity(tick - cc.velocityActivationTicks);

        if (pData.isDebugActive(CheckType.MOVING)) {
            CheckUtils.debug(player, CheckType.MOVING, " New velocity: " + vx + ", " + vy + ", " + vz);
        }

        // Always add vertical velocity.
        verVel.add(new SimpleEntry(tick, vy, flags, cc.velocityActivationCounter));

        // TODO: Should also switch to adding always.
        if (vx != 0.0 || vz != 0.0) {
            final double newVal = Math.sqrt(vx * vx + vz * vz);
            horVel.add(new AccountEntry(tick, newVal, cc.velocityActivationCounter, getHorVelValCount(newVal)));
        }

        // Set dirty flag here.
        sfDirty = true; // TODO: Set on using the velocity, due to latency !
    }


    /**
     * Reset velocity tracking (h+v).
     */
    public void removeAllVelocity() {
        horVel.clear();
        verVel.clear();
        sfDirty = false;
    }


    /**
     * Add velocity to internal book-keeping.
     * 
     * @param player
     * @param data
     * @param cc
     * @param vx
     * @param vy
     * @param vz
     */
    public void addVelocity(final Player player, final MovingConfig cc, final double vx, final double vy, final double vz) {
        addVelocity(player, cc, vx, vy, vz, 0L);
    }


    /**
     * Remove all velocity entries that are invalid. Checks both active and queued.
     * <br>(This does not catch invalidation by speed / direction changing.)
     * @param tick All velocity that was added before this tick gets removed.
     */
    public void removeInvalidVelocity(final int tick) {
        horVel.removeInvalid(tick);
        verVel.removeInvalid(tick);
    }


    /**
     * Called for moving events. Remove invalid entries, increase age of velocity, decrease amounts, check which entries are invalid. Both horizontal and vertical.
     */
    public void velocityTick(final int invalidateBeforeTick) {
        // Remove invalid velocity.
        removeInvalidVelocity(invalidateBeforeTick);

        // Horizontal velocity (intermediate concept).
        horVel.tick();

        // (Vertical velocity does not tick.)

        // Renew the dirty phase.
        if (!sfDirty && (horVel.hasActive() || horVel.hasQueued())) {
            sfDirty = true;
        }
    }


    ///////////////////////////////////////
    // Horizontal velocity 
    ///////////////////////////////////////
    /**
     * Std. validation counter for horizontal velocity, based on the value.
     * 
     * @param velocity
     * @return
     */
    public static int getHorVelValCount(double velocity) {
        // TODO: Configable max cap
        // TODO: Not sure if this is intentional but the cap would force NCP to always pick 30 for velocity entries smaller than 3.0
        // As a workaround/fix simply increase the actual velocity value
        // See: https://github.com/NoCheatPlus/NoCheatPlus/commit/a5ed7805429c73f8f2fec409c1947fb032210833
        return Math.max(30, 1 + (int) Math.round(velocity * 10.0)); // (Revert to 10 due to the hSpeed recode)
    }
    

    /**
     * Add horizontal velocity directly to horizontal-only bookkeeping.
     * 
     * @param vel
     *            Assumes positive values always.
     */
    public void addHorizontalVelocity(final AccountEntry vel) {
        horVel.add(vel);
    }


    /**
     * Clear only active horizontal velocity.
     */
    public void clearActiveHorVel() {
        horVel.clearActive();
    }


    /**
     * Reset velocity tracking (h).
     */
    public void clearAllHorVel() {
        horVel.clear();
    }


   /**
    * Test if the player has active horizontal velocity
    */
    public boolean hasActiveHorVel() {
        return horVel.hasActive();
    }


    /**
     * Test if the player has horizontal velocity entries in queue.
     * @return
     */
    public boolean hasQueuedHorVel() {
        return horVel.hasQueued();
    }


    /**
     * Test if the player has any horizontal velocity entry at all (active and queued)
     */
    public boolean hasAnyHorVel() {
        return horVel.hasAny();
    }


    /**
     * Get effective amount of all used velocity. Non-destructive.
     * @return
     */
    public double getHorizontalFreedom() {
        return horVel.getFreedom();
    }


    /**
     * Use all queued velocity until at least amount is matched.
     * Amount is the horizontal distance that is to be covered by velocity (active has already been checked).
     * <br>
     * If the modeling changes (max instead of sum or similar), then this will be affected.
     * @param amount The amount demanded, must be positive.
     * @return
     */
    public double useHorizontalVelocity(final double amount) {
        final double available = horVel.use(amount);
        if (available >= amount) {
            sfDirty = true;
        }
        return available;
    }


    /**
     * Get the xz-axis velocity tracker. Rather for testing purposes.
     * 
     * @return
     */
    public FrictionAxisVelocity getHorizontalVelocityTracker() {
        return horVel;
    }


    /**
     * Debugging.
     * @param builder
     */
    public void addHorizontalVelocity(final StringBuilder builder) {
        if (horVel.hasActive()) {
            builder.append("\n" + " Horizontal velocity (active):");
            horVel.addActive(builder);
        }
        if (horVel.hasQueued()) {
            builder.append("\n" + " Horizontal velocity (queued):");
            horVel.addQueued(builder);
        }
    }



    //////////////////////////////////
    // Vertical velocity
    //////////////////////////////////
    //  /**
    //   * Clear only active vertical velocity.
    //   */
    //  public void clearActiveVerVel() {
    //      verVel.clearActive();
    //  }


    public void prependVerticalVelocity(final SimpleEntry entry) {
        verVel.addToFront(entry);
    }


    /**
     * Get the first element without using it.
     * @param amount
     * @param minActCount
     * @param maxActCount
     * @return
     */
    public SimpleEntry peekVerticalVelocity(final double amount, final int minActCount, final int maxActCount) {
        return verVel.peek(amount, minActCount, maxActCount, TOL_VVEL);
    }


    /**
     * Add vertical velocity directly to vertical-only bookkeeping.
     * 
     * @param entry
     */
    public void addVerticalVelocity(final SimpleEntry entry) {
        verVel.add(entry);
    }


    /**
     * Test if the player has vertical velocity entries in queue.
     * @return
     */
    public boolean hasQueuedVerVel() {
        return verVel.hasQueued();
    }


    /**
     * Get the first matching velocity entry (invalidate others). Sets
     * verVelUsed if available.
     * 
     * @param amount
     * @return
     */
    public SimpleEntry useVerticalVelocity(final double amount) {
        final SimpleEntry available = verVel.use(amount, TOL_VVEL);
        if (available != null) {
            playerMoves.getCurrentMove().verVelUsed = available;
            sfDirty = true;
        }
        return available;
    }


    /**
     * Use the verVelUsed field, if it matches. Otherwise call
     * useVerticalVelocity(amount).
     * 
     * @param amount
     * @return
     */
    public SimpleEntry getOrUseVerticalVelocity(final double amount) {
        final SimpleEntry verVelUsed = playerMoves.getCurrentMove().verVelUsed;
        if (verVelUsed != null) {
            if (verVel.matchesEntry(verVelUsed, amount, TOL_VVEL)) {
                return verVelUsed;
            }
        }
        return useVerticalVelocity(amount);
    }
    

    /**
     * Remove from start while the flag is present.
     * @param flag
     */
    public void removeLeadingQueuedVerticalVelocityByFlag(final long flag) {
        verVel.removeLeadingQueuedVerticalVelocityByFlag(flag);
    }


    /**
     * Get the y-axis velocity tracker. Rather for testing purposes.
     * @return
     */
    public SimpleAxisVelocity getVerticalVelocityTracker() {
        return verVel;
    }


    /**
     * Debugging.
     * @param builder
     */
    public void addVerticalVelocity(final StringBuilder builder) {
        if (verVel.hasQueued()) {
            builder.append("\n" + " Vertical velocity (queued):");
            verVel.addQueued(builder);
        }
    }
    /////////////////////////////////////////////////////


    /**
     * Test if the location is the same, ignoring pitch and yaw.
     * @param loc
     * @return
     */
    public boolean isSetBack(final Location loc) {
        if (loc == null || setBack == null) {
            return false;
        }
        if (!loc.getWorld().getName().equals(setBack.getWorld().getName())) {
            return false;
        }
        return loc.getX() == setBack.getX() && loc.getY() == setBack.getY() && loc.getZ() == setBack.getZ();
    }


    public void adjustWalkSpeed(final float walkSpeed, final int tick, final int speedGrace) {
        if (this.walkSpeed == 0f) {
            this.walkSpeed = walkSpeed;
        }
        else this.walkSpeed = this.nextWalkSpeed;
        this.nextWalkSpeed = walkSpeed;
        //if (walkSpeed > this.walkSpeed) {
        //    this.walkSpeed = walkSpeed;
        //    this.speedTick = tick;
        //} 
        //else if (walkSpeed < this.walkSpeed) {
        //    if (tick - this.speedTick > speedGrace) {
        //        this.walkSpeed = walkSpeed;
        //        this.speedTick = tick;
        //    }
        //} 
        //else {
        //    this.speedTick = tick;
        //}
    }


    public void adjustFlySpeed(final float flySpeed, final int tick, final int speedGrace) {
        if (flySpeed > this.flySpeed) {
            this.flySpeed = flySpeed;
            this.speedTick = tick;
        } 
        else if (flySpeed < this.flySpeed) {
            if (tick - this.speedTick > speedGrace) {
                this.flySpeed = flySpeed;
                this.speedTick = tick;
            }
        } 
        else {
            this.speedTick = tick;
        }
    }


    /**
     * This tests for a LocationTrace instance being set at all, not for locations having been added.
     * @return
     */
    public boolean hasTrace() {
        return trace != null;
    }


    /**
     * Convenience: Access method to simplify coding, being aware of some plugins using Player implementations as NPCs, leading to traces not being present.
     * @return
     */
    public LocationTrace getTrace(final Player player) {
        return trace;
    }


    /**
     * Ensure to have a LocationTrace instance with the given parameters.
     * 
     * @param maxAge
     * @param maxSize
     * @return
     */
    private LocationTrace getTrace(final int maxAge, final int maxSize) {
        if (trace.getMaxSize() != maxSize || trace.getMaxAge() != maxAge) {
            // TODO: Might want to have tick passed as argument?
            trace.adjustSettings(maxAge, maxSize, TickTask.getTick());
        } 
        return trace;
    }


    /**
     * Convenience method to add a location to the trace, creates the trace if
     * necessary.
     * 
     * @param player
     * @param loc
     * @param time
     * @param iead
     *            If null getEyeHeight and 0.3 are used (assume fake player).
     * @return Updated LocationTrace instance, for convenient use, without
     *         sticking too much to MovingData.
     */
    public LocationTrace updateTrace(final Player player, final Location loc, final long time, final IEntityAccessDimensions iead) {
        final LocationTrace trace = getTrace(player);
        if (iead == null) {
            // TODO: 0.3 from bukkit based default heights (needs extra registered classes).
            trace.addEntry(time, loc.getX(), loc.getY(), loc.getZ(), 0.3, player.getEyeHeight());
        }
        else {
            trace.addEntry(time, loc.getX(), loc.getY(), loc.getZ(), iead.getWidth(player) / 2.0, Math.max(player.getEyeHeight(), iead.getHeight(player)));
        }
        return trace;
    }


    /**
     * Convenience.
     * @param loc
     * @param time
     * @param cc
     */
    public void resetTrace(final Player player, final Location loc, final long time, final IEntityAccessDimensions iead, final MovingConfig cc) {
        resetTrace(player, loc, time, cc.traceMaxAge, cc.traceMaxSize, iead);
    }


    /**
     * Convenience: Create or just reset the trace, add the current location.
     * @param loc 
     * @param size
     * @param mergeDist
     * @param traceMergeDist 
     */
    public void resetTrace(final Player player, final Location loc, final long time, final int maxAge, final int maxSize, final IEntityAccessDimensions iead) {
        if (trace != null) {
            trace.reset();
        }
        getTrace(maxAge, maxSize).addEntry(time, loc.getX(), loc.getY(), loc.getZ(), 
                iead.getWidth(player) / 2.0, Math.max(player.getEyeHeight(), iead.getHeight(player)));
    }


    /**
     * Test if velocity has affected the in-air jumping phase. Keeps set until
     * reset on-ground or otherwise. Use clearActiveVerVel to force end velocity
     * jump phase. Use hasAnyVerVel() to test if active or queued vertical
     * velocity should still be able to influence the in-air jump phase.
     * 
     * TODO: To be phased out by the new prediction modeling
     * 
     * @return
     */
    @Deprecated
    public boolean isVelocityJumpPhase() {
        return sfDirty;
    }


    /**
     * Refactoring stage: Test which value sfDirty should have and set
     * accordingly. This should only be called, if the player reached ground.
     * TODO: To be phased out by the new prediction modeling
     * 
     * @return If the velocity jump phase is still active (sfDirty).
     */
    @Deprecated
    public boolean resetVelocityJumpPhase() {
        return resetVelocityJumpPhase(null);
    }


    /**
     * See {@link #resetVelocityJumpPhase()}.
     * TODO: To be phased out by the new prediction modeling
     * @param tags
     * @return
     */
    @Deprecated
    public boolean resetVelocityJumpPhase(final Collection<String> tags) {
        if (horVel.hasActive() || horVel.hasQueued() 
            || sfDirty && shouldRetainSFDirty(tags)) {
            // TODO: What with vertical ?
            return sfDirty = true;
        }
        else {
            return sfDirty = false;
        }
    }
    
    // TODO: To be phased out by the new prediction modeling
    private final boolean shouldRetainSFDirty(final Collection<String> tags) {
        final PlayerMoveData thisMove = playerMoves.getLatestValidMove();

        if (thisMove == null || !thisMove.toIsValid || thisMove.yDistance >= 0.0) {

            final SimpleEntry entry = verVel.peek(thisMove == null ? 0.05 : thisMove.yDistance, 0, 4, 0.0);
            if (entry != null && entry.hasFlag(VelocityFlags.ORIGIN_BLOCK_BOUNCE)
                || thisMove != null && thisMove.verVelUsed != null 
                && thisMove.verVelUsed.hasFlag(VelocityFlags.ORIGIN_BLOCK_BOUNCE)) {

                // TODO: Strictly, pastground_from/to should rather be skipped instead of this.
                if (tags != null) {
                    tags.add("retain_dirty_bounce"); // +- block/push
                }
                return true;
            }
        }
        return false;
    }


    /**
     * Force set the move to be affected by previous speed. Currently
     * implemented as setting velocity jump phase.
     * TODO: To be phased out by the new prediction modeling
     */
     @Deprecated
    public void setFrictionJumpPhase() {
        // TODO: Better and more reliable modeling.
        sfDirty = true;
    }


    public void useVerticalBounce(final Player player) {
        // CHEATING: Ensure fall distance is reset.
        player.setFallDistance(0f);
        noFallMaxY = BlockProperties.getMinWorldY();
        noFallFallDistance = 0f;
        noFallSkipAirCheck = true;
        prependVerticalVelocity(verticalBounce);
        verticalBounce = null;
    }


    public void handleTimeRanBackwards() {
        final long time = System.currentTimeMillis();
        timeRiptiding = Math.min(timeRiptiding, time);
        vehicleMorePacketsLastTime = Math.min(vehicleMorePacketsLastTime, time);
        removeAllPlayerSpeedModifiers(); // TODO: This likely leads to problems.
        // (ActionFrequency can handle this.)
    }


    /**
     * The number of move events received.
     * 
     * @return
     */
    public int getPlayerMoveCount() {
        return playerMoveCount;
    }


    /**
     * Called with player move events.
     */
    public void increasePlayerMoveCount() {
        playerMoveCount++;
        if (playerMoveCount == Integer.MAX_VALUE) {
            playerMoveCount = 0;
            sfVLMoveCount = 0;
            morePacketsSetBackResetTime = 0;
            setBackResetTime = 0;
        }
    }


    /**
     * Age in move events.
     * @return
     */
    public int getMorePacketsSetBackAge() {
        return playerMoveCount - morePacketsSetBackResetTime;
    }


    @Override
    public boolean dataOnRemoveSubCheckData(Collection<CheckType> checkTypes) {
        // TODO: Detect if it is ok to remove data.
        // TODO: LocationTrace stays (leniency for other players!).
        // TODO: Likely more fields left to change.
        for (final CheckType checkType : checkTypes) {
            switch (checkType) {
                /*
                 * TODO: case MOVING: // Remove all in-place (future: data might
                 * stay as long as the player is online).
                 */
                case MOVING_SURVIVALFLY:
                    survivalFlyVL = 0;
                    clearFlyData(); // TODO: ...
                    resetSetBack(); // TODO: Not sure this is really best for compatibility.
                    wasInBed = false;
                    // TODO: other?
                    break;
                case MOVING_CREATIVEFLY:
                    creativeFlyVL = 0;
                    clearFlyData(); // TODO: ...
                    resetSetBack(); // TODO: Not sure this is really best for compatibility.
                    // TODO: other?
                    break;
                case MOVING_NOFALL:
                    noFallVL = 0;
                    clearNoFallData();
                    break;
                case MOVING_MOREPACKETS:
                    morePacketsVL = 0;
                    clearPlayerMorePacketsData();
                    morePacketsSetback = null;
                    morePacketsSetBackResetTime = 0;
                    break;
                case MOVING_PASSABLE:
                    passableVL = 0;
                    break;
                case MOVING_VEHICLE:
                    vehicleEnvelopeVL = 0;
                    vehicleMorePacketsVL = 0;
                    clearVehicleData();
                    break;
                case MOVING_VEHICLE_ENVELOPE:
                    vehicleEnvelopeVL = 0;
                    vehicleMoves.invalidate();
                    vehicleSetBacks.invalidateAll(); // Also invalidates morepackets set back.
                    break;
                case MOVING_VEHICLE_MOREPACKETS:
                    vehicleMorePacketsVL = 0;
                    clearVehicleMorePacketsData();
                    break;
                case MOVING:
                    clearMostMovingCheckData(); // Just in case.
                    return true;
                default:
                    break;
            }
        }
        return false;
    }


    @Override
    public boolean dataOnWorldUnload(final World world, final IGetGenericInstance dataAccess) {
        // TODO: Unlink world references.
        final String worldName = world.getName();
        if (teleported != null && worldName.equalsIgnoreCase(teleported.getWorld().getName())) {
            resetTeleported();
        }
        if (setBack != null && worldName.equalsIgnoreCase(setBack.getWorld().getName())) {
            clearFlyData();
        }
        if (morePacketsSetback != null && worldName.equalsIgnoreCase(morePacketsSetback.getWorld().getName())) {
            clearPlayerMorePacketsData();
            clearNoFallData(); // just in case.
        }
        // (Assume vehicle data needn't really reset here.)
        vehicleSetBacks.resetByWorldName(worldName);
        return false;
    }


    @Override
    public boolean dataOnReload(final IGetGenericInstance dataAccess) {
        final MovingConfig cc = dataAccess.getGenericInstance(MovingConfig.class);
        trace.adjustSettings(cc.traceMaxAge, cc.traceMaxSize, TickTask.getTick());
        return false;
    }
}