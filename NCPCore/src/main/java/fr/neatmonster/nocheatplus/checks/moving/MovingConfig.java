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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckConfig;
import fr.neatmonster.nocheatplus.checks.moving.model.ModelFlying;
import fr.neatmonster.nocheatplus.checks.moving.player.PlayerSetBackMethod;
import fr.neatmonster.nocheatplus.command.CommandUtil;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.versions.Bugs;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.config.value.OverrideType;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.utilities.ColorUtil;
import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.SimpleCharPrefixTree;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.IWorldData;

/**
 * Configurations specific for the moving checks. Every world gets one of these
 * assigned to it.
 */
public class MovingConfig extends ACheckConfig {

    // Model flying ids.
    public static final String ID_JETPACK_ELYTRA = "jetpack.elytra";

    // INSTANCE
    public final boolean ignoreCreative;
    public final boolean ignoreAllowFlight;

    private final Map<GameMode, ModelFlying> flyingModelGameMode = new HashMap<GameMode, ModelFlying>();
    private final ModelFlying flyingModelElytra;
    public final ActionList creativeFlyActions;

    /** Assumed number of packets per second under ideal conditions. */
    public final float morePacketsEPSIdeal;
    /** The maximum number of packets per second that we accept. */
    public final float morePacketsEPSMax;
    public final int morePacketsEPSBuckets;
    public final float morePacketsBurstPackets;
    public final double morePacketsBurstDirect;
    public final double morePacketsBurstEPM;
    public final int morePacketsSetBackAge;
    public final ActionList morePacketsActions;

    /**
     * Deal damage instead of Minecraft, whenever a player is judged to be on
     * ground.
     */
    public final boolean noFallDealDamage;
    public final boolean noFallSkipAllowFlight;
    /**
     * Reset data on violation, i.e. a player taking fall damage without being
     * on ground.
     */
    public final boolean noFallViolationReset;
    /** Reset data on tp. */
    public final boolean noFallTpReset;
    /** Reset if in vehicle. */
    public final boolean noFallVehicleReset;
    /** Reset fd to 0  if on ground (dealdamage only). */
    public final boolean noFallAntiCriticals;
    public final ActionList noFallActions;

    // TODO: passableAccuracy: also use if not using ray-tracing
    public final ActionList passableActions;
    public final double passableHorizontalMargins;
    public final double passableVerticalMargins;
    public final boolean passableUntrackedTeleportCheck;
    public final boolean passableUntrackedCommandCheck;
    public final boolean passableUntrackedCommandTryTeleport;
    public final SimpleCharPrefixTree passableUntrackedCommandPrefixes = new SimpleCharPrefixTree();
   
    public final double sfStepHeight;
    public final boolean survivalFlyResetItem;
    // Leniency settings.
    public final long survivalFlyVLFreezeCount;
    public final boolean survivalFlyVLFreezeInAir;
    // Set back policy.
    public final boolean sfSetBackPolicyVoid;
    public final boolean sfSetBackPolicyFallDamage;
    public final ActionList survivalFlyActions;

    public final boolean sfHoverCheck; // TODO: Sub check ?
    public final int sfHoverTicks;
    public final int sfHoverLoginTicks;
    public final boolean sfHoverFallDamage;
    public final double sfHoverViolation;

    // Special tolerance values:
    /**
     * Number of moving packets until which a velocity entry must be activated,
     * in order to not be removed.
     */
    public final int velocityActivationCounter;
    /** Server ticks until invalidating queues velocity. */
    public final int velocityActivationTicks;
    public final double noFallyOnGround;
    public final double yOnGround;

    // General things.
    public final boolean ignoreStance;
    public final boolean tempKickIllegal;
    public final boolean loadChunksOnJoin;
    public final boolean loadChunksOnMove;
    public final boolean loadChunksOnTeleport;
    public final boolean loadChunksOnWorldChange;
    public final int speedGrace;
    public final boolean enforceLocation;
    public final boolean trackBlockMove;
    public final PlayerSetBackMethod playerSetBackMethod;
    public final boolean resetFwOnground;
    public final boolean elytraStrict;

    // Vehicles
    public final boolean vehicleEnforceLocation;
    public final boolean vehiclePreventDestroyOwn;
    public final boolean scheduleVehicleSetBacks;
    public final boolean schedulevehicleSetPassenger;

    public final Set<EntityType> ignoredVehicles = new HashSet<EntityType>();

    public final ActionList vehicleMorePacketsActions;

    public final HashMap<EntityType, Double> vehicleEnvelopeHorizontalSpeedCap = new HashMap<EntityType, Double>();
    public final ActionList vehicleEnvelopeActions;

    // Trace
    public final int traceMaxAge;
    public final int traceMaxSize;

    // Messages.
    public final String msgKickIllegalMove;
    public final String msgKickIllegalVehicleMove;

    /**
     * Instantiates a new moving configuration.
     * 
     * @param config
     *            the data
     */
    public MovingConfig(final IWorldData worldData) {
        super(worldData);
        final ConfigFile config = worldData.getRawConfiguration();

        ignoreCreative = config.getBoolean(ConfPaths.MOVING_CREATIVEFLY_IGNORECREATIVE);
        ignoreAllowFlight = config.getBoolean(ConfPaths.MOVING_CREATIVEFLY_IGNOREALLOWFLIGHT);

        final ModelFlying defaultModel = new ModelFlying("gamemode.creative", config, ConfPaths.MOVING_CREATIVEFLY_MODEL + "creative.", new ModelFlying().lock());
        for (final GameMode gameMode : GameMode.values()) {
            flyingModelGameMode.put(gameMode, new ModelFlying("gamemode." + gameMode.name().toLowerCase(), config, 
                    ConfPaths.MOVING_CREATIVEFLY_MODEL + (gameMode.name().toLowerCase()) + ".", defaultModel).lock());
        }
        flyingModelElytra = new ModelFlying(ID_JETPACK_ELYTRA, config, ConfPaths.MOVING_CREATIVEFLY_MODEL + "elytra.", 
                                            new ModelFlying(null, defaultModel).verticalAscendGliding(true).lock());

        resetFwOnground = config.getBoolean(ConfPaths.MOVING_CREATIVEFLY_EYTRA_FWRESET);
        elytraStrict = config.getBoolean(ConfPaths.MOVING_CREATIVEFLY_EYTRA_STRICT);
        creativeFlyActions = config.getOptimizedActionList(ConfPaths.MOVING_CREATIVEFLY_ACTIONS, Permissions.MOVING_CREATIVEFLY);

        morePacketsEPSIdeal = config.getInt(ConfPaths.MOVING_MOREPACKETS_EPSIDEAL);
        morePacketsEPSMax = Math.max(morePacketsEPSIdeal, config.getInt(ConfPaths.MOVING_MOREPACKETS_EPSMAX));
        morePacketsEPSBuckets = 2 * Math.max(1, Math.min(60, config.getInt(ConfPaths.MOVING_MOREPACKETS_SECONDS)));
        morePacketsBurstPackets = config.getInt(ConfPaths.MOVING_MOREPACKETS_BURST_EPM);
        morePacketsBurstDirect = config.getInt(ConfPaths.MOVING_MOREPACKETS_BURST_DIRECT);
        morePacketsBurstEPM = config.getInt(ConfPaths.MOVING_MOREPACKETS_BURST_EPM);
        morePacketsSetBackAge = config.getInt(ConfPaths.MOVING_MOREPACKETS_SETBACKAGE);
        morePacketsActions = config.getOptimizedActionList(ConfPaths.MOVING_MOREPACKETS_ACTIONS, Permissions.MOVING_MOREPACKETS);

        noFallDealDamage = config.getBoolean(ConfPaths.MOVING_NOFALL_DEALDAMAGE);
        noFallSkipAllowFlight = config.getBoolean(ConfPaths.MOVING_NOFALL_SKIPALLOWFLIGHT);
        noFallViolationReset = config.getBoolean(ConfPaths.MOVING_NOFALL_RESETONVL);
        noFallTpReset = config.getBoolean(ConfPaths.MOVING_NOFALL_RESETONTP);
        noFallVehicleReset = config.getBoolean(ConfPaths.MOVING_NOFALL_RESETONVEHICLE);
        noFallAntiCriticals = config.getBoolean(ConfPaths.MOVING_NOFALL_ANTICRITICALS);
        noFallActions = config.getOptimizedActionList(ConfPaths.MOVING_NOFALL_ACTIONS, Permissions.MOVING_NOFALL);

        passableActions = config.getOptimizedActionList(ConfPaths.MOVING_PASSABLE_ACTIONS, Permissions.MOVING_PASSABLE);
        passableHorizontalMargins = config.getDouble(ConfPaths.MOVING_PASSABLE_RT_XZ_FACTOR, 0.1, 1.0, 0.999999);
        passableVerticalMargins = config.getDouble(ConfPaths.MOVING_PASSABLE_RT_Y_FACTOR, 0.1, 1.0, 0.999999);
        passableUntrackedTeleportCheck = config.getBoolean(ConfPaths.MOVING_PASSABLE_UNTRACKED_TELEPORT_ACTIVE);
        passableUntrackedCommandCheck = config.getBoolean(ConfPaths.MOVING_PASSABLE_UNTRACKED_CMD_ACTIVE);
        passableUntrackedCommandTryTeleport = config.getBoolean(ConfPaths.MOVING_PASSABLE_UNTRACKED_CMD_TRYTELEPORT);
        CommandUtil.feedCommands(passableUntrackedCommandPrefixes, config, ConfPaths.MOVING_PASSABLE_UNTRACKED_CMD_PREFIXES, true);

        survivalFlyResetItem = config.getBoolean(ConfPaths.MOVING_SURVIVALFLY_EXTENDED_RESETITEM);
        sfSetBackPolicyFallDamage = config.getBoolean(ConfPaths.MOVING_SURVIVALFLY_SETBACKPOLICY_FALLDAMAGE);
        sfSetBackPolicyVoid = config.getBoolean(ConfPaths.MOVING_SURVIVALFLY_SETBACKPOLICY_VOIDTOVOID);
        final double sfStepHeight = config.getDouble(ConfPaths.MOVING_SURVIVALFLY_STEPHEIGHT, Double.MAX_VALUE);
        if (sfStepHeight == Double.MAX_VALUE) {
            final String ref;
            if (Bukkit.getVersion().toLowerCase().indexOf("spigot") != -1) {
                // Assume 1.8 clients being supported.
                ref = "1.7.10";
            } 
            else ref = "1.8";
            this.sfStepHeight = ServerVersion.select(ref, 0.5, 0.6, 0.6, 0.5).doubleValue();
        } 
        else this.sfStepHeight = sfStepHeight;
        survivalFlyVLFreezeCount = config.getInt(ConfPaths.MOVING_SURVIVALFLY_LENIENCY_FREEZECOUNT);
        survivalFlyVLFreezeInAir = config.getBoolean(ConfPaths.MOVING_SURVIVALFLY_LENIENCY_FREEZEINAIR);
        survivalFlyActions = config.getOptimizedActionList(ConfPaths.MOVING_SURVIVALFLY_ACTIONS, Permissions.MOVING_SURVIVALFLY);

        sfHoverCheck = config.getBoolean(ConfPaths.MOVING_SURVIVALFLY_HOVER_CHECK);
        sfHoverTicks = config.getInt(ConfPaths.MOVING_SURVIVALFLY_HOVER_TICKS);
        sfHoverLoginTicks = Math.max(0, config.getInt(ConfPaths.MOVING_SURVIVALFLY_HOVER_LOGINTICKS));
        sfHoverFallDamage = config.getBoolean(ConfPaths.MOVING_SURVIVALFLY_HOVER_FALLDAMAGE);
        sfHoverViolation = config.getDouble(ConfPaths.MOVING_SURVIVALFLY_HOVER_SFVIOLATION);

        velocityActivationCounter = config.getInt(ConfPaths.MOVING_VELOCITY_ACTIVATIONCOUNTER);
        velocityActivationTicks = config.getInt(ConfPaths.MOVING_VELOCITY_ACTIVATIONTICKS);
        yOnGround = config.getDouble(ConfPaths.MOVING_YONGROUND, Magic.Y_ON_GROUND_MIN, Magic.Y_ON_GROUND_MAX, Magic.Y_ON_GROUND_DEFAULT); // sqrt(1/256), see: NetServerHandler.
        noFallyOnGround = config.getDouble(ConfPaths.MOVING_NOFALL_YONGROUND, Magic.Y_ON_GROUND_MIN, Magic.Y_ON_GROUND_MAX, yOnGround);

        // TODO: Ignore the stance, once it is known that the server catches such.
        AlmostBoolean refIgnoreStance = config.getAlmostBoolean(ConfPaths.MOVING_IGNORESTANCE, AlmostBoolean.MAYBE);
        ignoreStance = refIgnoreStance == AlmostBoolean.MAYBE ? ServerVersion.compareMinecraftVersion("1.8") >= 0 : refIgnoreStance.decide();
        tempKickIllegal = config.getBoolean(ConfPaths.MOVING_TEMPKICKILLEGAL);
        loadChunksOnJoin = config.getBoolean(ConfPaths.MOVING_LOADCHUNKS_JOIN);
        loadChunksOnMove = config.getBoolean(ConfPaths.MOVING_LOADCHUNKS_MOVE);
        loadChunksOnTeleport = config.getBoolean(ConfPaths.MOVING_LOADCHUNKS_TELEPORT);
        loadChunksOnWorldChange = config.getBoolean(ConfPaths.MOVING_LOADCHUNKS_WORLDCHANGE);
        speedGrace = Math.max(0, (int) Math.round(config.getDouble(ConfPaths.MOVING_SPEEDGRACE) * 20.0)); // Config: seconds
        AlmostBoolean ref = config.getAlmostBoolean(ConfPaths.MOVING_ENFORCELOCATION, AlmostBoolean.MAYBE);
        enforceLocation = ref == AlmostBoolean.MAYBE ? Bugs.shouldEnforceLocation() : ref.decide();
        // TODO: Rename overall flag to trackBlockChanges. Create a sub-config rather.
        trackBlockMove = config.getBoolean(ConfPaths.COMPATIBILITY_BLOCKS_CHANGETRACKER_ACTIVE) 
                && (config.getBoolean(ConfPaths.COMPATIBILITY_BLOCKS_CHANGETRACKER_PISTONS
                        // TODO: || other activation flags.
                        ));
        final PlayerSetBackMethod playerSetBackMethod = PlayerSetBackMethod.fromString("extern.fromconfig", config.getString(ConfPaths.MOVING_SETBACK_METHOD));
        if (playerSetBackMethod.doesThisMakeSense()) {
            // (Might info/warn if legacy is used without setTo and without SCHEDULE and similar?)
            this.playerSetBackMethod = playerSetBackMethod;
        }
        else if (ServerVersion.compareMinecraftVersion("1.9") < 0) {
            this.playerSetBackMethod = PlayerSetBackMethod.LEGACY;
        }
        // Latest.
        else this.playerSetBackMethod = PlayerSetBackMethod.MODERN;

        traceMaxAge = config.getInt(ConfPaths.MOVING_TRACE_MAXAGE, 30);
        traceMaxSize = config.getInt(ConfPaths.MOVING_TRACE_MAXSIZE, 30);

        ref = config.getAlmostBoolean(ConfPaths.MOVING_VEHICLE_ENFORCELOCATION, AlmostBoolean.MAYBE);
        vehicleEnforceLocation = ref.decideOptimistically(); // Currently rather enabled.
        vehiclePreventDestroyOwn = config.getBoolean(ConfPaths.MOVING_VEHICLE_PREVENTDESTROYOWN);
        scheduleVehicleSetBacks = config.getAlmostBoolean(ConfPaths.MOVING_VEHICLE_SCHEDULESETBACKS, AlmostBoolean.MAYBE).decide();
        vehicleMorePacketsActions = config.getOptimizedActionList(ConfPaths.MOVING_VEHICLE_MOREPACKETS_ACTIONS, Permissions.MOVING_MOREPACKETS);
        schedulevehicleSetPassenger = config.getAlmostBoolean(ConfPaths.MOVING_VEHICLE_DELAYADDPASSENGER, AlmostBoolean.MAYBE).decideOptimistically();
        ref = config.getAlmostBoolean(ConfPaths.MOVING_VEHICLE_ENVELOPE_ACTIVE, AlmostBoolean.MAYBE);
        if (ServerVersion.compareMinecraftVersion("1.9") < 0) {
            worldData.overrideCheckActivation(CheckType.MOVING_VEHICLE_ENVELOPE, AlmostBoolean.NO, OverrideType.PERMANENT, true);
        }
        config.readDoubleValuesForEntityTypes(ConfPaths.MOVING_VEHICLE_ENVELOPE_HSPEEDCAP, vehicleEnvelopeHorizontalSpeedCap, 4.0, true);
        vehicleEnvelopeActions = config.getOptimizedActionList(ConfPaths.MOVING_VEHICLE_ENVELOPE_ACTIONS, Permissions.MOVING_VEHICLE_ENVELOPE);
        // Ignored vehicle types (ignore mostly, no checks run).
        List<String> types;
        if (config.get(ConfPaths.MOVING_VEHICLE_IGNOREDVEHICLES) == null) { // Hidden setting for now.
            // Use defaults.
            types = Arrays.asList("arrow", "spectral_arrow", "tipped_arrow");
        }
        else {
            types = config.getStringList(ConfPaths.MOVING_VEHICLE_IGNOREDVEHICLES);
        }
        for (String stype : types) {
            try {
                EntityType type = EntityType.valueOf(stype.toUpperCase());
                if (type != null) {
                    ignoredVehicles.add(type);
                }
            }
            catch (IllegalArgumentException e) {}
        }

        // Messages.
        msgKickIllegalMove = ColorUtil.replaceColors(config.getString(ConfPaths.MOVING_MESSAGE_ILLEGALPLAYERMOVE));
        msgKickIllegalVehicleMove = ColorUtil.replaceColors(config.getString(ConfPaths.MOVING_MESSAGE_ILLEGALVEHICLEMOVE));
    }


   /**
    * Retrieve the CreativeFly model to use in thisMove (Set in the MovingListener).
    * Note that the name is somewhat anachronistic. (Should be renamed to CreativeFlyModel/MovementModel/(...))
    * @param player
    * @param fromLocation
    * @param data
    * @param cc
    * 
    */
    public ModelFlying getModelFlying(final Player player, final PlayerLocation fromLocation, final MovingData data, final MovingConfig cc) {

        final GameMode gameMode = player.getGameMode();
        final ModelFlying modelGameMode = flyingModelGameMode.get(gameMode);
        final boolean isGlidingWithElytra = Bridge1_9.isGlidingWithElytra(player) && MovingUtil.isGlidingWithElytraValid(player, fromLocation, data, cc);
        final long now = System.currentTimeMillis();
        switch(gameMode) {
            case SURVIVAL:
            case ADVENTURE:
            case CREATIVE:
                // Specific checks.
                break;
            default:
                // Default by game mode (spectator, yet unknown).
                return modelGameMode;
        }
        // Actual flying (ignoreAllowFlight is a legacy option for rocket boots like flying).

        if (player.isFlying() 
            || !isGlidingWithElytra && !ignoreAllowFlight && player.getAllowFlight()) {
            return modelGameMode;
        }
        // Elytra.
        if (isGlidingWithElytra) { // Defensive: don't demand isGliding.
            return flyingModelElytra;
        }
        // Default by game mode.
        return modelGameMode;
    }

}
