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
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.checks.moving.location.setback.DefaultSetBackStorage;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.TraceEntryPool;
import fr.neatmonster.nocheatplus.checks.moving.magic.Magic;
import fr.neatmonster.nocheatplus.checks.moving.model.LocationData;
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
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionAccumulator;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.workaround.IWorkaroundRegistry.WorkaroundSet;

/**
 * Player specific data for the moving checks.
 */
public class MovingData extends ACheckData implements IDataOnRemoveSubCheckData, IDataOnReload, IDataOnWorldUnload {

    //private static final long IGNORE_SETBACK_Y = BlockProperties.F_SOLID | BlockProperties.F_GROUND | BlockProperties.F_CLIMBABLE | BlockProperties.F_LIQUID;

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
    /** Tick counter for how long a player has been in water 0= out of water, 10= fully in water. */
    public int liqtick = 0;
    /** ID for players leaving a liquid in order to patch waterwalk exploits: 0= no checking, 1= enforce speed restriction until ground is touched or the player sinks back in water/lava. */
    public int surfaceId = 0;
    /** Tick counter used to workaround certain transitions with repeated or high motion (e.g.: gliding->normal, riptiding->normal). */
    // TODO: Rename -> motionTransitionTick/(...)
    public int keepfrictiontick = 0;
    /** Countdown for the next bunnyhop. 10(maximum) represents a just performed bunnyhop. */
    public int bunnyhopDelay;
    /** bunnyHopDelay phase before applying a LostGround case (set in SurvivalFly.bunnyHop()) */ 
    public int lastbunnyhopDelay = 0;
    /** Ticks after landing on ground (InAir->Ground). Mainly used in SurvivalFly. */
    public int bunnyhopTick = 0;
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
    /** Fly speed */
    public float flySpeed = 0.0f;
    /** Keep track of the amplifier given by the jump potion. */
    public double jumpAmplifier = 0;
    /** Multiplier at the last time sprinting. */
    public double multSprinting = 1.30000002; 
    /** Used in workaroundFlyCheckTransition() in the MovingListener for velocity. */
    public long delayWorkaround = 0;
    /** Last time the player was actually sprinting. */
    public long timeSprinting = 0;
    /** Last time the player was riptiding */
    public long timeRiptiding = 0;
    /** Represents how long a vehicle has been tossed up by a bubble column */
    // TODO: Deprecate and use the blockChangeTracker, rather.
    public long timeVehicletoss = 0;
    /** Used as a workaround for boats leaving ice while still having velocity from ice */
    public int boatIceVelocityTicks = 0;
    /** Moving half on 15/16 height block and half on water. Set in Survivalfly.check. */
    public boolean isHalfGroundHalfWater = false;
    /** If is Bedrock Player. This is set if CompatNoCheatPlus is present. */
    public boolean bedrockPlayer = false;
    /** Temporary snow fix flag */
    // TODO: remove.
    public boolean snowFix = false;
    /** Whether or not this horizontal movement is leading downstream. */
    public boolean isdownstream = false;
    /** Last used block change id (BlockChangeTracker). */
    public final BlockChangeReference blockChangeRef = new BlockChangeReference();
    
    // *----------Friction (hor/ver)----------*
    /** Rough friction factor estimate, 0.0 is the reset value (maximum with lift-off/burst speed is used). */
    public double lastFrictionHorizontal = 0.0;
    /** Rough friction factor estimate, 0.0 is the reset value (maximum with lift-off/burst speed is used). */
    public double lastFrictionVertical = 0.0;
    /** Used during processing, no resetting necessary.*/
    public double nextFrictionHorizontal = 0.0;
    /** Used during processing, no resetting necessary.*/
    public double nextFrictionVertical = 0.0;
    
    // *----------No slowdown related data----------*
    /** Whether the player is using an item */
    public boolean isusingitem = false;
    /** TODO: */
    public boolean offhanduse = false;
    /** TODO: */
    public int olditemslot = 0;
    /** TODO: */
    public boolean changeslot = false;
    /** TODO: */
    public long time_rl_item = 0;
    /** Detection: the player is cheating for sure */
    public boolean isHackingRI = false;
    /** Keep track of hopping while using items */
    public int noslowhop = 0;

    // *----------Move / Vehicle move tracking----------*
    /** Keep track of currently processed (if) and past moves for player moving. Stored moves can be altered by modifying the int. */
    public final MoveTrace <PlayerMoveData> playerMoves = new MoveTrace<PlayerMoveData>(new Callable<PlayerMoveData>() {
        @Override
        public PlayerMoveData call() throws Exception {
            return new PlayerMoveData();
        }
    }, 8); // Too many moves, perhaps... :p 
    /** Keep track of currently processed (if) and past moves for vehicle moving. Stored moves can be altered by modifying the int. */
    // TODO: There may be need to store such data with vehicles, or detect tandem abuse in a different way.
    public final MoveTrace <VehicleMoveData> vehicleMoves = new MoveTrace<VehicleMoveData>(new Callable<VehicleMoveData>() {
        @Override
        public VehicleMoveData call() throws Exception {
            return new VehicleMoveData();
        }
    }, 4);

    // *----------Velocity handling----------* 
    /** Tolerance value for using vertical velocity (the client sends different values than received with fight damage). */
    private static final double TOL_VVEL = 0.0625;
    /** Vertical velocity modeled as an axis (positive and negative possible) */
    private final SimpleAxisVelocity verVel = new SimpleAxisVelocity();
    /** Horizontal velocity modeled as an axis (always positive) */
    private final FrictionAxisVelocity horVel = new FrictionAxisVelocity();
    /** Whether or not the calculated explosion velocity should be applied. */
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
    /** Burst count. */
    public final ActionFrequency morePacketsBurstFreq;
    /** Setback for MP. */
    private Location morePacketsSetback = null;

    // *----------Data of the NoFall check----------*
    /** Our calculated fall distance */
    public float noFallFallDistance = 0;
    /** Last y coordinate from when the player was on ground. */
    public double noFallMaxY = 0;
    /** Indicate that NoFall is not to use next damage event for checking on-ground properties. */ 
    public boolean noFallSkipAirCheck = false;

    // *----------Data of the SurvivalFly check----------*
    /** Default lift-off envelope, used after resetting. <br> TODO: Test, might be better ground. */
    private static final LiftOffEnvelope defaultLiftOffEnvelope = LiftOffEnvelope.UNKNOWN;
    /** playerMoveCount at the time of the last sf violation. */
    public int sfVLTime = 0;
    /** The current horizontal buffer value. Horizontal moving VLs get compensated with emptying the buffer. */
    public double sfHorizontalBuffer = 0.0;
    /** Event-counter to cover up for sprinting resetting server side only. Set in the FighListener. */
    public int lostSprintCount = 0;
    /** Count how long the player has been in the air, resets when landing on ground. */
    public int sfJumpPhase = 0;
    /** Count how many times in a row yDistance has been zero, only for in-air moves, updated on not cancelled moves (aimed at in-air workarounds) */
    public int sfZeroVdistRepeat = 0;
    /** "Dirty" flag, for receiving velocity and similar while in air. */
    private boolean sfDirty = false;
    /** Indicate low jumping descending phase (likely cheating). */
    public boolean sfLowJump = false;
    /** Hacky way to indicate that this movement cannot be a lowjump. */
    public boolean sfNoLowJump = false; 
    /** Basic envelope constraints for lifting off ground. */
    public LiftOffEnvelope liftOffEnvelope = defaultLiftOffEnvelope;
    /** Count how many moves have been made inside a medium (other than air). */
    public int insideMediumCount = 0;
    // TODO: Does combinedMedium stuff need resetting on join/teleport/special?
    /** Number of moves for horizontal moving within air + certain medium. */
    public int combinedMediumHCount = 0;
    /** Sum of actual speed / base speed for horizontal moving within air + certain medium. */
    public double combinedMediumHValue = 0.0;
    /** Counting while the player is not on ground and not moving. A value < 0 means not hovering at all. */
    public int sfHoverTicks = -1;
    /** First count these down before incrementing sfHoverTicks. Set on join, if configured so. */
    public int sfHoverLoginTicks = 0;
    /** Ticks influenced by ice friction after sptintjumping, used in survivalFly.setAllowedHDist(). */
    public int sfOnIce = 0; 
    /** Ticks influenced by bounce friction after sprintjumping, used in survivalFly.setAllowedHDist(). */
    public int sfBounceTick = 0;
    /** Fake in air flag: set with any violation, reset once on ground. */
    public boolean  sfVLInAir = false;
    /** Vertical accounting info */
    public final ActionAccumulator vDistAcc = new ActionAccumulator(3, 3);
    /** Workarounds (InAirRules,LiquidWorkarounds). */
    public final WorkaroundSet ws;

    // *----------Data of the vehicles checks----------*
    /** Default value for the VehicleMP buffer. */
    public static final int vehicleMorePacketsBufferDefault = 50;
    /** The buffer used for VehicleMP violations. */
    public int vehicleMorePacketsBuffer = vehicleMorePacketsBufferDefault;
    /** TODO: */
    public long vehicleMorePacketsLastTime;
    /** Task id of the vehicle set back task. */ 
    public int vehicleSetBackTaskId = -1;
    /** Task id of the passenger set back task. */ 
    public int vehicleSetPassengerTaskId = -1;
    




    //////////////////////////////////////////////
    // HOT FIX / WORKAROUNDS                    //
    //////////////////////////////////////////////
    /** Set to true after login/respawn, only if the set back is reset there. Reset in MovingListener after handling PlayerMoveEvent */
    public boolean joinOrRespawn = false;
    /** Number of (player/vehicle) move events since set.back. Update after running standard checks on that EventPriority level (not MONITOR). */
    public int timeSinceSetBack = 0;
    /** Location hash value of the last (player/vehicle) set back, for checking independently of which set back location had been used. */
    public int lastSetBackHash = 0;
    /** Position teleported from into another world. Only used for certain contexts for workarounds. */
    public IPositionWithLook crossWorldFrom = null;

    // *----------Vehicles----------*
    /** Inconsistency-flag. Set on moving inside of vehicles, reset on exiting properly. Workaround for VehicleLeaveEvent missing. */
    public boolean wasInVehicle = false; 
    /** TODO: */
    public boolean waspreInVehicle = false;
    /** Set to indicate that events happen during a vehicle set back. Allows skipping some resetting. */
    public boolean isVehicleSetBack = false;
    /** TODO: Movement consistency */
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
        bunnyhopDelay = 0;
        sfJumpPhase = 0;
        jumpAmplifier = 0;
        sfOnIce = 0;
        sfBounceTick = 0;
        setBack = null;
        sfZeroVdistRepeat = 0;
        clearAccounting();
        clearNoFallData();
        removeAllPlayerSpeedModifiers();
        lostSprintCount = 0;
        sfHoverTicks = sfHoverLoginTicks = -1;
        sfDirty = false;
        sfLowJump = false;
        liftOffEnvelope = defaultLiftOffEnvelope;
        insideMediumCount = 0;
        vehicleConsistency = MoveConsistency.INCONSISTENT;
        lastFrictionHorizontal = lastFrictionVertical = 0.0;
        verticalBounce = null;
        blockChangeRef.valid = false;
        bunnyhopTick = 0;
        liqtick = 0;
        combinedMediumHCount = 0;
        combinedMediumHValue = 0.0;
        // More resets? (bunnyhopTick, liqtick...)
    }


    /**
     * On confirming a set back (teleport monitor / move start point): Mildly
     * reset the flying data without losing any important information. Past move
     * is adjusted to the given setBack, internal setBack is only updated, if
     * none is set.
     * 
     * @param setBack
     */
    public void onSetBack(final PlayerLocation setBack) {
        // Reset positions (a teleport should follow, though).
        this.morePacketsSetback = null;
        clearAccounting(); // Might be more safe to do this.
        // Keep no-fall data.
        // Fly data: problem is we don't remember the settings for the set back location.
        // Assume the player to start falling from there rather, or be on ground.
        // TODO: Check if to adjust some counters to state before setback? 
        // Keep jump amplifier
        // Keep bunny-hop delay (?)
        // keep jump phase.
        lostSprintCount = 0;
        sfHoverTicks = -1; // 0 ?
        sfDirty = false;
        sfLowJump = false;
        liftOffEnvelope = defaultLiftOffEnvelope;
        insideMediumCount = 0;
        removeAllPlayerSpeedModifiers();
        vehicleConsistency = MoveConsistency.INCONSISTENT; // Not entirely sure here.
        lastFrictionHorizontal = lastFrictionVertical = 0.0;
        verticalBounce = null;
        timeSinceSetBack = 0;
        lastSetBackHash = setBack == null ? 0 : setBack.hashCode();
        // Reset to setBack.
        resetPlayerPositions(setBack);
        adjustMediumProperties(setBack);
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
        clearAccounting();
        sfJumpPhase = 0;
        sfZeroVdistRepeat = 0;
        verticalBounce = null;
        // Remember where we send the player to.
        setTeleported(loc);
        // TODO: sfHoverTicks ?
    }


    /**
     * Set data.nextFriction according to media.
     * @param from
     * @param to
     * @param data
     * @param cc
     */
    public void setNextFriction(final PlayerMoveData thisMove, final MovingConfig cc) {

        // NOTE: Other methods might still override nextFriction to 1.0 due to burst/lift-off envelope.
        // TODO: Other media / medium transitions / friction by block.
        final LocationData from = thisMove.from;
        final LocationData to = thisMove.to;

        if (from.inWeb || to.inWeb) {
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        else if (from.inPowderSnow || to.inPowderSnow) {
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        // No from#onClimbable check to fix vines fps casue by medium counts, probably wrong place! 
        else if (to.onClimbable) {
            // TODO: Not sure about horizontal (!).
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        else if (to.onHoneyBlock || to.onHoneyBlock) {
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        else if (from.inBerryBush || to.inBerryBush) {
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        else if (from.inLiquid) {
            // TODO: Exact conditions ?!
            if (from.inLava) {
                nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_LAVA;
            }
            else {
                nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_WATER;
            }
        }
        // TODO: consider setting minimum friction last (air), do add ground friction.
        else if (!from.onGround && !to.onGround) {
            nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_AIR;
        }
        else {
            nextFrictionHorizontal = 0.0; // TODO: Friction for walking on blocks (!).
            nextFrictionVertical = Magic.FRICTION_MEDIUM_AIR;
        }
    }


    /**
     * Adjust properties that relate to the medium, called on set back and
     * similar. <br>
     * Currently: liftOffEnvelope, nextFriction.
     * 
     * @param loc
     */
    public void adjustMediumProperties(final PlayerLocation loc) {
        // Ensure block flags have been collected.
        loc.collectBlockFlags();
        // Simplified.
        if (loc.isInWeb()) {
            liftOffEnvelope = LiftOffEnvelope.NO_JUMP;
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        // Actually, here some friction may apply (Vertical)...
        else if (loc.isInBerryBush()) {
            liftOffEnvelope = LiftOffEnvelope.BERRY_JUMP;
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        else if (loc.isOnHoneyBlock()) {
            liftOffEnvelope = LiftOffEnvelope.HALF_JUMP;
            nextFrictionHorizontal = nextFrictionVertical = 0.0;
        }
        else if (loc.isInLiquid()) {
            // TODO: Distinguish strong limit.
            liftOffEnvelope = LiftOffEnvelope.LIMIT_LIQUID;
            if (loc.isInLava()) {
                nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_LAVA;
            } 
            else {
                nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_WATER;
            }
        }
        else if (loc.isOnGround()) {
            liftOffEnvelope = LiftOffEnvelope.NORMAL;
            nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_AIR;
        }
        else {
            liftOffEnvelope = LiftOffEnvelope.UNKNOWN;
            nextFrictionHorizontal = nextFrictionVertical = Magic.FRICTION_MEDIUM_AIR;
        }
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
        bedrockPlayer = false;
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
        sfZeroVdistRepeat = 0;
        sfDirty = false;
        sfLowJump = false;
        liftOffEnvelope = defaultLiftOffEnvelope;
        insideMediumCount = 0;
        lastFrictionHorizontal = lastFrictionVertical = 0.0;
        verticalBounce = null;
        blockChangeRef.valid = false;
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
     * Clear accounting data.
     */
    public void clearAccounting() {
        vDistAcc.clear();
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
     * Clear the data of the new fall check.
     */
    public void clearNoFallData() {
        noFallFallDistance = 0;
        noFallMaxY = BlockProperties.getMinWorldY();
        noFallSkipAirCheck = false;
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
     * Add velocity to internal book-keeping.
     * 
     * @param player
     * @param data
     * @param cc
     * @param vx
     * @param vy
     * @param vz
     */
    public void addVelocity(final Player player, final MovingConfig cc, 
            final double vx, final double vy, final double vz) {
        addVelocity(player, cc, vx, vy, vz, 0L);
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
     * @param flags
     *            Flags to use with velocity entries.
     */
    public void addVelocity(final Player player, final MovingConfig cc, final double vx, final double vy, final double vz, final long flags) {
        final int tick = TickTask.getTick();
        // TODO: Slightly odd to call this each time, might switch to a counter-strategy (move - remove). 
        removeInvalidVelocity(tick  - cc.velocityActivationTicks);

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
        sfNoLowJump = true; // TODO: Set on using the velocity, due to latency !

    }


    /**
     * Std. value counter for horizontal velocity, based on the vlaue.
     * 
     * @param velocity
     * @return
     */
    public static int getHorVelValCount(double velocity) {
        // TODO: Configable max cap
        return Math.max(30, 1 + (int) Math.round(velocity * 10.0));
    }


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


    public void addVerticalVelocity(final SimpleEntry entry) {
        verVel.add(entry);
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
     * Remove/reset all speed modifier tracking, like vertical and horizontal
     * velocity, elytra boost, buffer.
     */
    private void removeAllPlayerSpeedModifiers() {
        // Velocity
        removeAllVelocity();
        // Elytra boost best fits velocity / effects.
        fireworksBoostDuration = 0; 
        fireworksBoostTickExpire = 0;
        // Horizontal buffer.
        sfHorizontalBuffer = 0.0;
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
     * Remove all velocity entries that are invalid. Checks both active and queued.
     * <br>(This does not catch invalidation by speed / direction changing.)
     * @param tick All velocity added before this tick gets removed.
     */
    public void removeInvalidVelocity(final int tick) {
        horVel.removeInvalid(tick);
        verVel.removeInvalid(tick);
    }


    /**
     * Clear only active horizontal velocity.
     */
    public void clearActiveHorVel() {
        horVel.clearActive();
    }


    public boolean hasActiveHorVel() {
        return horVel.hasActive();
    }


    public boolean hasQueuedHorVel() {
        return horVel.hasQueued();
    }


    /**
     * Active or queued.
     * @return
     */
    public boolean hasAnyHorVel() {
        return horVel.hasAny();
    }


    /**
     * Queued velocity only.
     * @return
     */
    public boolean hasAnyVerVel() {
        return verVel.hasQueued();
    }

    //    public boolean hasActiveVerVel() {
    //        return verVel.hasActive();
    //    }

    //    public boolean hasQueuedVerVel() {
    //        return verVel.hasQueued();
    //    }


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
            // TODO: Consider sfNoLowJump = true;
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
     * Debugging.
     * @param builder
     */
    public void addVerticalVelocity(final StringBuilder builder) {
        if (verVel.hasQueued()) {
            builder.append("\n" + " Vertical velocity (queued):");
            verVel.addQueued(builder);
        }
    }


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
        if (walkSpeed > this.walkSpeed) {
            this.walkSpeed = walkSpeed;
            this.speedTick = tick;
        } 
        else if (walkSpeed < this.walkSpeed) {
            if (tick - this.speedTick > speedGrace) {
                this.walkSpeed = walkSpeed;
                this.speedTick = tick;
            }
        } 
        else {
            this.speedTick = tick;
        }
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
     * @return
     */
    public boolean isVelocityJumpPhase() {
        return sfDirty;
    }


    /**
     * Refactoring stage: Test which value sfDirty should have and set
     * accordingly. This should only be called, if the player reached ground.
     * 
     * @return If the velocity jump phase is still active (sfDirty).
     */
    public boolean resetVelocityJumpPhase() {
        return resetVelocityJumpPhase(null);
    }


    /**
     * See {@link #resetVelocityJumpPhase()}.
     * @param tags
     * @return
     */
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
     */
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
        timeSprinting = Math.min(timeSprinting, time);
        vehicleMorePacketsLastTime = Math.min(vehicleMorePacketsLastTime, time);
        clearAccounting(); // Not sure: adding up might not be nice.
        removeAllPlayerSpeedModifiers(); // TODO: This likely leads to problems.
        // (ActionFrequency can handle this.)
    }


    /**
     * Get the y-axis velocity tracker. Rather for testing purposes.
     * 
     * @return
     */
    public SimpleAxisVelocity getVerticalVelocityTracker() {
        return verVel;
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
            sfVLTime = 0;
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


    /**
     * Remove from start while the flag is present.
     * @param originBlockBounce
     */
    public void removeLeadingQueuedVerticalVelocityByFlag(final long flag) {
        verVel.removeLeadingQueuedVerticalVelocityByFlag(flag);
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
        trace.adjustSettings(cc.traceMaxAge, cc.traceMaxSize, System.currentTimeMillis());
        return false;
    }

}