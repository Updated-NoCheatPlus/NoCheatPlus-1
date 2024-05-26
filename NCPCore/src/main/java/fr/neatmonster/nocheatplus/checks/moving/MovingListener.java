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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.combined.Combined;
import fr.neatmonster.nocheatplus.checks.combined.CombinedConfig;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.moving.magic.Magic;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.ModelFlying;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveInfo;
import fr.neatmonster.nocheatplus.checks.moving.player.CreativeFly;
import fr.neatmonster.nocheatplus.checks.moving.player.MorePackets;
import fr.neatmonster.nocheatplus.checks.moving.player.NoFall;
import fr.neatmonster.nocheatplus.checks.moving.player.Passable;
import fr.neatmonster.nocheatplus.checks.moving.player.PlayerSetBackMethod;
import fr.neatmonster.nocheatplus.checks.moving.player.SurvivalFly;
import fr.neatmonster.nocheatplus.checks.moving.util.AuxMoving;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.checks.moving.util.bounce.BounceType;
import fr.neatmonster.nocheatplus.checks.moving.util.bounce.BounceUtil;
import fr.neatmonster.nocheatplus.checks.moving.vehicle.VehicleChecks;
import fr.neatmonster.nocheatplus.checks.moving.velocity.AccountEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.checks.net.NetData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_17;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.BridgePotionEffect;
import fr.neatmonster.nocheatplus.compat.Folia;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.BlockChangeEntry;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.location.SimplePositionWithLook;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.components.registry.feature.IHaveCheckType;
import fr.neatmonster.nocheatplus.components.registry.feature.INeedConfig;
import fr.neatmonster.nocheatplus.components.registry.feature.IRemoveData;
import fr.neatmonster.nocheatplus.components.registry.feature.JoinLeaveListener;
import fr.neatmonster.nocheatplus.components.registry.feature.TickListener;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.logging.debug.DebugUtil;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.PotionUtil;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.MapUtil;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the moving checks.
 * 
 * @see MovingEvent
 */
public class MovingListener extends CheckListener implements TickListener, IRemoveData, IHaveCheckType, INeedConfig, JoinLeaveListener {

    /** The no fall check. **/
    public final NoFall noFall = addCheck(new NoFall());

    /** The creative fly check. */
    private final CreativeFly creativeFly = addCheck(new CreativeFly());

    /** The more packets check. */
    private final MorePackets morePackets = addCheck(new MorePackets());
    
    /** Vehicle checks */
    private final VehicleChecks vehicleChecks = new VehicleChecks();

    /** The survival fly check. */
    private final SurvivalFly survivalFly = addCheck(new SurvivalFly());

    /** The Passable check.*/
    private final Passable passable = addCheck(new Passable());
    
    /** Store events by player name, in order to invalidate moving processing on higher priority level in case of teleports. */
    private final Map<String, PlayerMoveEvent> processingEvents = new HashMap<String, PlayerMoveEvent>();

    /** Player names to check hover for, case insensitive. */
    private final Set<String> hoverTicks = ConcurrentHashMap.newKeySet(30); // TODO: Rename

    /** Player names to check enforcing the location for in onTick, case insensitive. */
    private final Set<String> playersEnforce = ConcurrentHashMap.newKeySet(30);

    private int hoverTicksStep = 5;

    /** Location for temporary use with getLocation(useLoc). Always call setWorld(null) after use. Use LocUtil.clone before passing to other API. */
    final Location useLoc = new Location(null, 0, 0, 0); // TODO: Put to use...
    final Location useBedLeaveLoc = new Location(null, 0, 0, 0);
    final Location useChangeWorldLoc = new Location(null, 0, 0, 0);
    final Location useDeathLoc = new Location(null, 0, 0, 0);
    final Location useFallLoc = new Location(null, 0, 0, 0);
    final Location useJoinLoc = new Location(null, 0, 0, 0);
    final Location useLeaveLoc = new Location(null, 0, 0, 0);
    final Location useToggleFlightLoc = new Location(null, 0, 0, 0);
    final Location useTickLoc = new Location(null, 0, 0, 0);

    /** Auxiliary functionality. */
    private final AuxMoving aux = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class);

    private IGenericInstanceHandle<IAttributeAccess> attributeAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IAttributeAccess.class);

    private final BlockChangeTracker blockChangeTracker;

    /** Statistics / debugging counters. */
    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);

    private final int idMoveEvent = counters.registerKey("event.player.move");

    private final boolean specialMinecart = ServerVersion.compareMinecraftVersion("1.19.4") >= 0;

    @SuppressWarnings("unchecked")
    public MovingListener() {
        super(CheckType.MOVING);
        // Register vehicleChecks.
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        api.addComponent(vehicleChecks);
        blockChangeTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
        if (Bridge1_9.hasEntityToggleGlideEvent()) {
            queuedComponents.add(new Listener() {
                @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
                public void onEntityToggleGlide(final EntityToggleGlideEvent event) {
                    if (handleEntityToggleGlideEvent(event.getEntity(), event.isGliding())) {
                        event.setCancelled(true);
                    }
                }
            });
        }

        // Register config and data.
        // TODO: Should register before creating Check instances ?
        api.register(api.newRegistrationContext()
                // MovingConfig
                .registerConfigWorld(MovingConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, MovingConfig>() {
                    @Override
                    public MovingConfig getNewInstance(
                            final WorldFactoryArgument arg) {
                        return new MovingConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer(CheckType.MOVING, true)
                .context() //
                // MovingData
                .registerDataPlayer(MovingData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, MovingData>() {
                    @Override
                    public MovingData getNewInstance(
                            final PlayerFactoryArgument arg) {
                        return new MovingData(arg.worldData.getGenericInstance(
                                MovingConfig.class), arg.playerData);
                    }
                })
                .addToGroups(CheckType.MOVING, false, IData.class, ICheckData.class)
                .removeSubCheckData(CheckType.MOVING, true)
                .context() //
                );
    }

    /**
     * 
     * @param entity
     * @param isGliding
     * @return True, if the event is to be cancelled.
     */
    private boolean handleEntityToggleGlideEvent(final Entity entity, final boolean isGliding) {

        // Ignore non players.
        if (!(entity instanceof Player)) {
            return false;
        }
        final Player player = (Player) entity;
        if (isGliding && !Bridge1_9.isGlidingWithElytra(player)) { // Includes check for elytra item.
            final PlayerMoveInfo info = aux.usePlayerMoveInfo();
            info.set(player, player.getLocation(info.useLoc), null, 0.001); // Only restrict very near ground.
            final IPlayerData pData = DataManager.getPlayerData(player);
            final MovingData data = pData.getGenericInstance(MovingData.class);
            final boolean res = !MovingUtil.canLiftOffWithElytra(player, info.from, data);
            info.cleanup();
            aux.returnPlayerMoveInfo(info);
            if (res && pData.isDebugActive(checkType)) {
                debug(player, "Prevent toggle glide on.");
            }
            return res;
        }
        return false;
    }


    /**
     * We listen to this event to prevent player from flying by sending bed leaving packets.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedEnter(final PlayerBedEnterEvent event) {
        DataManager.getGenericInstance(event.getPlayer(), MovingData.class).wasInBed = true;
    }


    /**
     * We listen to this event to prevent player from flying by sending bed leaving packets.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(final PlayerBedLeaveEvent event) {

        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
    
        if (pData.isCheckActive(CheckType.MOVING_SURVIVALFLY, player) && survivalFly.checkBed(player, pData, cc, data)) {

            // Check if the player has to be reset.
            // To "cancel" the event, we teleport the player.
            Location newTo = null;
            final Location loc = player.getLocation(useBedLeaveLoc);
            final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
            moveInfo.set(player, loc, null, cc.yOnGround);
            final boolean sfCheck = MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, moveInfo.to, data, cc, pData);
            aux.returnPlayerMoveInfo(moveInfo);
            if (sfCheck) {
                newTo = MovingUtil.getApplicableSetBackLocation(player, loc.getYaw(), loc.getPitch(), moveInfo.from, data, cc);
            }
            if (newTo == null) {
                newTo = LocUtil.clone(loc);
            }

            if (sfCheck && cc.sfSetBackPolicyFallDamage && noFall.isEnabled(player, pData)) {
                // Check if to deal damage.
                double y = loc.getY();
                if (data.hasSetBack()) y = Math.min(y, data.getSetBackY());
                noFall.checkDamage(player, y, data, pData);
            }
            // Cleanup
            useBedLeaveLoc.setWorld(null);
            // Teleport.
            data.prepareSetBack(newTo); // Should be enough. 
            Folia.teleportEntity(player, newTo, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
        }
        // Reset bed ...
        else data.wasInBed = false;
    }


    // Temporary fix "stuck" on boat for 1.14-1.19
    @EventHandler(priority = EventPriority.LOWEST)
    public void onUnknowBoatTeleport(final PlayerTeleportEvent event) {
        if (!Bridge1_13.hasIsSwimming() || specialMinecart) return;
        if (event.getCause() == TeleportCause.UNKNOWN) {
            final Player player = event.getPlayer();
            final IPlayerData pData = DataManager.getPlayerData(player);
            final MovingData data = pData.getGenericInstance(MovingData.class);
            if (data.lastVehicleType != null && standsOnEntity(player, player.getLocation().getY())) {
                event.setCancelled(true);
                player.setSwimming(false);
            }
        }
    }


    private boolean standsOnEntity(final Entity entity, final double minY){
            // TODO: Probably check other ids too before doing this ?
            for (final Entity other : entity.getNearbyEntities(1.5, 1.5, 1.5)){
                final EntityType type = other.getType();
                if (!MaterialUtil.isBoat(type)){
                    continue; 
                }
                final Material m = other.getLocation().getBlock().getType();
                final double locY = other.getLocation().getY();
                return Math.abs(locY - minY) < 0.7 && BlockProperties.isLiquid(m);
            }
        return false;
    }

    /**
     * Just for security, if a player switches between worlds, reset the fly and more packets checks data, because it is
     * definitely invalid now.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {

        // Maybe this helps with people teleporting through Multiverse portals having problems?
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        data.clearMostMovingCheckData();
        final Location loc = player.getLocation(useChangeWorldLoc);
        data.setSetBack(loc);
        if (cc.loadChunksOnWorldChange) MovingUtil.ensureChunksLoaded(player, loc, "world change", data, cc, pData);
        aux.resetPositionsAndMediumProperties(player, loc, data, cc);
        data.resetTrace(player, loc, TickTask.getTick(), mcAccess.getHandle(), cc);
        // Just in case.
        if (cc.enforceLocation) playersEnforce.add(player.getName());
        useChangeWorldLoc.setWorld(null);
    }


    /**
     * When a player changes their gamemode, all information related to the moving checks becomes invalid.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {

        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.CREATIVE) {
            final MovingData data = DataManager.getGenericInstance(player, MovingData.class);
            data.clearFlyData();
            data.clearPlayerMorePacketsData();
            // TODO: Set new set back if any fly check is activated.
            // (Keep vehicle data as is.)
        }
    }


    /**
     * When a player moves, they will be checked for various suspicious behaviors.<br>
     * (lowest priority)
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerMove(final PlayerMoveEvent event) {

        counters.add(idMoveEvent, 1);
        final Player player = event.getPlayer();
        // Store the event for monitor level checks.
        processingEvents.put(player.getName(), event);
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final boolean debug = pData.isDebugActive(checkType);
        data.increasePlayerMoveCount();
        final Location from = event.getFrom().clone();
        final Location to = event.getTo().clone();
        Location newTo = null;


        //////////////////////////////////////////  
        // Check problematic yaw/pitch values.  //
        //////////////////////////////////////////
        if (LocUtil.needsDirectionCorrection2(from.getYaw(), from.getPitch())) {
            from.setYaw(LocUtil.correctYaw2(from.getYaw()));
            from.setPitch(LocUtil.correctPitch(from.getPitch()));
        }

        if (LocUtil.needsDirectionCorrection2(to.getYaw(), to.getPitch())) {
            to.setYaw(LocUtil.correctYaw2(to.getYaw()));
            to.setPitch(LocUtil.correctPitch(to.getPitch()));
        }
        

        ////////////////////////////////////////////////////
        // Early return checks (no full processing).      //
        ////////////////////////////////////////////////////
        // TODO: Check illegal moves here anyway (!).
        // TODO: Check if vehicle move logs correctly (fake).
        final boolean earlyReturn;
        final String token;
        if (player.isInsideVehicle()) {
            // No full processing for players in vehicles.
            newTo = vehicleChecks.onPlayerMoveVehicle(player, from, to, data, pData);
            earlyReturn = true;
            token = "vehicle";
        }
        else if (data.lastVehicleType == EntityType.MINECART && specialMinecart
            // The setback comes from VehicleChecks#onPlayerVehicleLeave
            // Don't be confuse with data.getSetBack(from) here, the location "from" is used when the stored setback is null 
            && to.distance(data.getSetBack(from)) < 3) {
            earlyReturn = true;
            token = "minecart-total";
            data.lastVehicleType = null;
            // Some changes were made in 1.19.4 make PlayerMoveEvent no longer fire while on Minecart
            // So when the player leave the vehicle will make PlayerMoveEvent 
            // work normal again and update the position so result in the location player start to ride minecart and location player left it
        }
        else if (data.vehicleLeave && to.distance(from) > 3) {
        	earlyReturn = true;
            token = "vehicle-leave-sb";
            // Set-back for sure
            newTo = data.getSetBack(from);
            data.vehicleLeave = false;
        }
        else if (player.isDead()) {
            // Ignore dead players.
            data.sfHoverTicks = -1;
            earlyReturn = true;
            token = "dead";
        }
        else if (player.isSleeping()) {
            // Ignore sleeping players.
            data.sfHoverTicks = -1;
            earlyReturn = true;
            token = "sleeping";
        }
        else if (!from.getWorld().equals(to.getWorld())) {
            // Keep hover ticks.
            // Ignore changing worlds.
            earlyReturn = true;
            token = "worldchange";
        }
        else if (data.hasTeleported()) {
            earlyReturn = handleTeleportedOnMove(player, event, data, cc, pData);
            token = "awaitsetback";
        }
        else if (TrigUtil.isSamePos(from, to) && !data.lastMoveNoMove
                // && ServerVersion.compareMinecraftVersion("1.17") >= 0
                ) { 
            //if (data.sfHoverTicks > 0) data.sfHoverTicks += hoverTicksStep;
            earlyReturn = data.lastMoveNoMove = true;
            token = "duplicate";
            // Ignore 1.17+ duplicate position packets.
            // Context: Mojang attempted to fix a bucket placement desync issue by re-sending the previous position on right clicking...
            // On the server-side, this translates in a duplicate move which we need to ignore (i.e.: players can have 0 distance in air, MorePackets will trigger due to the extra packet if the button is pressed for long enough etc...)
            // You would think that this would AT LEAST fix the issue, but it doesn't. However it surely does complicate things on our side.
            // Thanks Mojang as always.
            // TODO: Micro moves can be detected as duplicate !
            // NOTE: on ground status does not seem to change
        }
        else {
            earlyReturn = false;
            token = null;
        }

        // Reset duplicate move flag.
        if (!TrigUtil.isSamePos(from, to)) {
            data.lastMoveNoMove = false;
        }

        if (earlyReturn) {
            if (debug) {
                debug(player, "Early return" + (token == null ? "" : (" (" + token + ")")) +  " on PlayerMoveEvent: from: " + from + " , to: " + to);
            }
            if (newTo != null) {
                // Illegal Yaw/Pitch.
                if (LocUtil.needsYawCorrection(newTo.getYaw())) {
                    newTo.setYaw(LocUtil.correctYaw(newTo.getYaw()));
                }

                if (LocUtil.needsPitchCorrection(newTo.getPitch())) {
                    newTo.setPitch(LocUtil.correctPitch(newTo.getPitch()));
                }
                // Set.
                prepareSetBack(player, event, newTo, data, cc, pData); // Logs set back details.
            }
            data.joinOrRespawn = false;
            return;
        }


        //////////////////////////////////////////////////////////////
        // Fire one or two moves here (Split move handling).        //
        //////////////////////////////////////////////////////////////
        // (newTo should be null here)
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final Location loc = player.getLocation(moveInfo.useLoc);
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (cc.loadChunksOnMove) MovingUtil.ensureChunksLoaded(player, from, to, lastMove, "move", cc, pData);
        
        // Ordinary: Fire move from -> to
        // This was reported and supposedely fixed by the Spigot team, however asofold decided to keep this active anyway. No reason is given from the commit.
        // @See: https://github.com/NoCheatPlus/NoCheatPlus/commit/7d2c1ce1f8b40fac554cdef8040576d9f88503ef
        // @See: https://hub.spigotmc.org/jira/browse/SPIGOT-1646
        if (
                // Handling split moves has been disabled.
                !cc.splitMoves ||
                // The usual case: no micro move happened.
                TrigUtil.isSamePos(from, loc)
                // Special case / bug? TODO: Which/why, which version of MC/spigot?
                || lastMove.valid && TrigUtil.isSamePos(loc, lastMove.from.getX(), lastMove.from.getY(), lastMove.from.getZ())
                // TODO: On pistons pulling the player back: -1.15 yDistance for split move 1 (untracked position > 0.5 yDistance!).
                // (Could also be other envelopes (0.9 velocity upwards), too tedious to research.)
            ) {
            // 0: Fire move from -> to
            moveInfo.set(player, from, to, cc.yOnGround);
            checkPlayerMove(player, from, to, 0, moveInfo, debug, data, cc, pData, event);
        }
        else {
            // (Special case: Location has not been updated last moving event.)
            // Split into two moves.
            // 1. Process from -> loc.
            if (debug) debug(player, "Split move 1 (from -> loc):");
            moveInfo.set(player, from, loc, cc.yOnGround);
            if (!checkPlayerMove(player, from, loc, 1, moveInfo, debug, data, cc, pData, event) && processingEvents.containsKey(player.getName())) {

                // Between -> set data accordingly (compare: onPlayerMoveMonitor).
                onMoveMonitorNotCancelled(player, from, loc, System.currentTimeMillis(), TickTask.getTick(), pData.getGenericInstance(CombinedData.class), data, cc, pData);
                data.joinOrRespawn = false;
                // 2. Process loc -> to.
                if (debug) debug(player, "Split move 2 (loc -> to):");
                moveInfo.set(player, loc, to, cc.yOnGround);
                checkPlayerMove(player, loc, to, 2, moveInfo, debug, data, cc, pData, event);
            }
        }
        // Cleanup.
        data.joinOrRespawn = false;
        aux.returnPlayerMoveInfo(moveInfo);
    }


    /**
     * During early player move handling: data.hasTeleported() returned true.
     * 
     * @param player
     * @param event
     * @param data
     * @param cc 
     * 
     * @return
     */
    private boolean handleTeleportedOnMove(final Player player, final PlayerMoveEvent event, final MovingData data, 
                                           final MovingConfig cc, final IPlayerData pData) {

        // This could also happen with a packet based set back such as with cancelling move events.
        final boolean debug = pData.isDebugActive(checkType);
        if (data.isTeleportedPosition(event.getFrom())) {
            // Treat as ACK (!).
            // Adjust.
            confirmSetBack(player, false, data, cc, pData, event.getFrom());
            // Log.
            if (debug) debug(player, "Implicitly confirm set back with the start point of a move.");
            return false;
        }
        else if (DataManager.getPlayerData(player).isPlayerSetBackScheduled()) {
            // A set back has been scheduled, but the player is moving randomly.
            // TODO: Instead alter the move from location and let it get through? +- when
            event.setCancelled(true);
            if (debug) debug(player, "Cancel move, due to a scheduled teleport (set back).");
            return true;
        }
        else {
            // Left-over (Demand: schedule or teleport before moving events arrive).
            if (debug) debug(player, "Invalidate left-over teleported (set back) location: " + data.getTeleported());
            data.resetTeleported();
            // TODO: More to do?
            return false;
        }
    }


    /**
     * Core move checks.
     * @param player
     * @param from
     * @param to
     * @param multiMoveCount
     *            0: An ordinary move, not split. 1/2: first/second of a split
     *            move.
     * @param moveInfo
     * @param data
     * @param cc
     * @param event
     * @return If cancelled/done, i.e. not to process further split moves.
     */
    private boolean checkPlayerMove(final Player player, final Location from, final Location to, final int multiMoveCount, 
                                    final PlayerMoveInfo moveInfo, final boolean debug, final MovingData data, 
                                    final MovingConfig cc, final IPlayerData pData, final PlayerMoveEvent event) {

        Location newTo = null;
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final double xDistance = to.getX() - from.getX();
        final double zDistance = to.getZ() - from.getZ();
        final String playerName = player.getName(); // TODO: Could switch to UUID here (needs more changes).
        final long time = System.currentTimeMillis();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        // TODO: Order this to above "early return"?
        // Set up data / caching.
        // TODO: Data resetting above ?
        data.resetTeleported();
        ////////////////////////////
        // Debug.                 //
        ////////////////////////////
        if (debug) {
            outputMoveDebug(player, moveInfo.from, moveInfo.to, Math.max(cc.noFallyOnGround, cc.yOnGround), mcAccess.getHandle());
        }
        

        ////////////////////////////////////////////////////
        // Check for illegal move and bounding box etc.   //
        ///////////////////////////////////////////////////
        if ((moveInfo.from.hasIllegalCoords() || moveInfo.to.hasIllegalCoords()) ||
            !cc.ignoreStance && (moveInfo.from.hasIllegalStance() || moveInfo.to.hasIllegalStance())) {
            MovingUtil.handleIllegalMove(event, player, data, cc);
            return true;
        }
        

        /////////////////////////////////////
        // Check for location consistency. //
        /////////////////////////////////////
        if (cc.enforceLocation && playersEnforce.contains(playerName)) {
            // NOTE: The setback should not be set before this, even if not yet set.
            // Last to vs. from.
            newTo = enforceLocation(player, from, data);
            playersEnforce.remove(playerName);
        }


        //////////////////////////////////////////////
        // Check for sprinting (assumeSprint)       //
        //////////////////////////////////////////////
        if (player.isSprinting() || cc.assumeSprint) {
            // TODO: Collect all these properties within a context object (abstraction + avoid re-fetching). 
            if (player.getFoodLevel() > 5 || player.getAllowFlight() || player.isFlying()) {
                data.timeSprinting = time;
                data.multSprinting = attributeAccess.getHandle().getSprintAttributeMultiplier(player);

                if (data.multSprinting == Double.MAX_VALUE) {
                    data.multSprinting = 1.30000002;
                }
                else if (cc.assumeSprint && data.multSprinting == 1.0) {
                    // Server side can be inconsistent, so the multiplier might be plain wrong (1.0).
                    // TODO: Could be more/less than actual, but "infinite" latency would not work either.
                    data.multSprinting = 1.30000002;
                }
            }
            else if (time < data.timeSprinting) data.timeSprinting = 0;
        }
        else if (time < data.timeSprinting) data.timeSprinting = 0;
        

        /////////////////////////////////////
        // Prepare locations for use.      //
        /////////////////////////////////////
        // TODO: Block flags might not be needed if neither sf nor passable get checked.
        final PlayerLocation pFrom, pTo;
        pFrom = moveInfo.from;
        pTo = moveInfo.to;
        

        ////////////////////////////////////
        // Powder snow handling 1.17+     //
        ////////////////////////////////////
        if (Bridge1_17.hasIsFrozen()) {
            boolean hasboots = Bridge1_17.hasLeatherBootsOn(player);
            if (pTo.isOnGround() && !hasboots 
                && pTo.adjustOnGround(!pTo.isOnGroundDueToStandingOnAnEntity() && !pTo.isOnGround(cc.yOnGround, BlockFlags.F_POWDERSNOW)) && debug) {
                debug(player, "Collide ground surface but not actually on ground. Adjusting To location.");
            }
            if (pFrom.isOnGround() && !hasboots 
                && pFrom.adjustOnGround(!pFrom.isOnGroundDueToStandingOnAnEntity() && !pFrom.isOnGround(cc.yOnGround, BlockFlags.F_POWDERSNOW)) && debug) {
                debug(player, "Collide ground surface but not actually on ground. Adjusting From location");
            }
        }


        //////////////////////////////////////////////
        // HOT FIX - for VehicleLeaveEvent missing. //
        //////////////////////////////////////////////
        if (data.wasInVehicle) {
            vehicleChecks.onVehicleLeaveMiss(player, data, cc, pData);
        }


        ////////////////////////////////////
        // Set some data for this move.   //
        ////////////////////////////////////
        thisMove.set(pFrom, pTo);
        if (multiMoveCount > 0) {
            thisMove.multiMoveCount = multiMoveCount;
        }
    

        ////////////////////////////
        // Potion effect "Jump".  //
        ////////////////////////////
        // TODO: Jump amplifier should be set in PlayerMoveData, and/or only get updated for lift off (?).
        // TODO: same for speed (once medium is introduced).
        final double jumpAmplifier = aux.getJumpAmplifier(player);
        if (jumpAmplifier > data.jumpAmplifier) {
            data.jumpAmplifier = jumpAmplifier;
        }


        ////////////////////////////////////////////////
        // Velocity tick (decrease + invalidation).   //
        ///////////////////////////////////////////////
        // TODO: Rework to generic (?) queued velocity entries: activation + invalidation
        final int tick = TickTask.getTick();
        data.velocityTick(tick - cc.velocityActivationTicks);


        ////////////////////////////////////
        // Check which fly check to use.  //
        ////////////////////////////////////
        boolean checkCf;
        boolean checkSf;
        if (MovingUtil.shouldCheckSurvivalFly(player, pFrom, pTo, data, cc, pData)) {
            checkCf = false;
            checkSf = true;
            data.adjustWalkSpeed(player.getWalkSpeed(), tick, cc.speedGrace);
        }
        else if (pData.isCheckActive(CheckType.MOVING_CREATIVEFLY, player)) {
            checkCf = true;
            checkSf = false;
            prepareCreativeFlyCheck(player, from, to, moveInfo, thisMove, multiMoveCount, tick, data, cc);
        }
        // (thisMove.flyCheck stays null.)
        else checkCf = checkSf = false;


        ////////////////////////////////////////////////////////////////////////
        // Pre-check checks (hum), either for cf or for sf.                   //
        ////////////////////////////////////////////////////////////////////////
        boolean checkNf = true;
        BounceType verticalBounce = BounceType.NO_BOUNCE;
        final boolean useBlockChangeTracker;
        final double previousSetBackY;
        final boolean checkPassable = pData.isCheckActive(CheckType.MOVING_PASSABLE, player);

        // 1: Hot fix: Entering end portal from bottom.
        if (lastMove.to.getWorldName() != null && !lastMove.to.getWorldName().equals(thisMove.from.getWorldName())) {

            if (TrigUtil.distance(pFrom, pTo) > 5.5) {
                newTo = data.getSetBack(from);
                checkNf = false;
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.STATUS, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) + " Player move end point seems to be set wrongly.");
            }
        }
        
        // 2: Proactive reset of elytraBoost (MC 1.11.2).
        if (data.fireworksBoostDuration > 0) {
            if (!lastMove.valid 
                || (cc.resetFwOnground && (lastMove.flyCheck != CheckType.MOVING_CREATIVEFLY || lastMove.modelFlying != thisMove.modelFlying))
                || data.fireworksBoostTickExpire < tick) {
                data.fireworksBoostDuration = 0;
            }
            else data.fireworksBoostDuration --;
        }
        
        // 3: Set time resolutions and various counters.
        // 3.1: Liquid tick time.
        if (pFrom.isInLiquid()) data.liqtick = data.liqtick < 10 ? data.liqtick + 1 : data.liqtick > 0 ? data.liqtick - 1 : 0; 
        else data.liqtick = data.liqtick > 0 ? data.liqtick - 2 : 0;

        // 3.2: Set riptiding time.
        if (Bridge1_13.isRiptiding(player)) data.timeRiptiding = System.currentTimeMillis();

        // 3.3: Set bubble stream counter
        // Count how long one is getting pushed by a bubble stream (cap at 50)
        if (!pFrom.isDraggedByBubbleStream() && !pTo.isDraggedByBubbleStream() 
            && (pFrom.isInBubbleStream() || pTo.isInBubbleStream()) 
            && thisMove.yDistance > 0.0 && data.insideBubbleStreamCount <= 50) {
            data.insideBubbleStreamCount++ ;
        }
        // Decrease counter
        if (data.insideBubbleStreamCount > 0) {
            // Invalidate
            if (!lastMove.valid || lastMove.flyCheck != CheckType.MOVING_SURVIVALFLY
                || !data.liftOffEnvelope.name().startsWith("LIMIT")) {
                data.insideBubbleStreamCount = 0;
            }
            // Left stream or player is descending, decrease.
            else if (!pFrom.isInBubbleStream() && !pTo.isInBubbleStream() || thisMove.yDistance < 0.0) {
                data.insideBubbleStreamCount-- ; 
            }
            // (else keep)
        }
        
        // 4: Workaround for 1.14+ exiting vehicles.
        if (data.lastVehicleType != null && thisMove.distanceSquared < 5) {
            data.setSetBack(from);
            data.addHorizontalVelocity(new AccountEntry(thisMove.hDistance, 1, 1));
            data.addVerticalVelocity(new SimpleEntry(thisMove.yDistance, 1));
            //data.addVerticalVelocity(new SimpleEntry(-0.16, 2));
            data.lastVehicleType = null;
        }
        
        // 5: Pre-checks relevant to Sf or Cf.
        if (checkSf || checkCf) {
            previousSetBackY = data.hasSetBack() ? data.getSetBackY() : Double.NEGATIVE_INFINITY;
            MovingUtil.checkSetBack(player, pFrom, data, pData, this); // Ensure we have a set back set.

            // 5.1: Check for special cross world teleportation issues with the end.
            if (data.crossWorldFrom != null) {
                if (!TrigUtil.isSamePosAndLook(pFrom, pTo) && TrigUtil.isSamePosAndLook(pTo, data.crossWorldFrom)) {
                    // Assume to (and possibly the player location) to be set to the location the player teleported from within the other world.
                    newTo = data.getSetBack(from); // (OK, cross-world)
                    checkNf = false;
                    NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.STATUS, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) + " Player move end point seems to be set wrongly.");
                }
                // Always reset.
                data.crossWorldFrom = null;
            }

            // 5.2: Extreme move check (sf or cf is precondition, should have their own config/actions later).
            if (newTo == null && ((Math.abs(thisMove.yDistance) > Magic.EXTREME_MOVE_DIST_VERTICAL) || thisMove.hDistance > Magic.EXTREME_MOVE_DIST_HORIZONTAL)) {
                // Test for friction and velocity.
                newTo = checkExtremeMove(player, pFrom, pTo, data, cc);
                if (newTo != null) {
                    thisMove.flyCheck = checkSf ? CheckType.MOVING_SURVIVALFLY : CheckType.MOVING_CREATIVEFLY;
                }
            }
            
            // 5.3: Set BCT
            // NOTE: Block change activity has to be checked *after* the extreme move checks run.
            useBlockChangeTracker = newTo == null && cc.trackBlockMove && (checkPassable || checkSf || checkCf) && blockChangeTracker.hasActivityShuffled(from.getWorld().getUID(), pFrom, pTo, 1.5625);

            // 5.4: Check jumping on things like slime blocks.
            // Detect bounce type / use prepared bounce.
            if (newTo == null) {
                // TODO: Mixed ground (e.g. slime blocks + slabs), specifically on pushing.
                // TODO: More on fall damage. What with sneaking + past states?
                // TODO: With past states: What does jump effects do here?
                if (thisMove.yDistance < 0.0) {
                    // Prepare bounce: The center of the player must be above the block.
                    // Common pre-conditions.
                    // TODO: Check if really leads to calling the method for pistons (checkBounceEnvelope vs. push).
                    if (!survivalFly.isReallySneaking(player) && BounceUtil.checkBounceEnvelope(player, pFrom, pTo, data, cc, pData)) {
                        // TODO: Check other side conditions (fluids, web, max. distance to the block top (!))
                        // Classic static bounce.
                        if ((pTo.getBlockFlags() & BlockFlags.F_BOUNCE25) != 0L) {
                            /* TODO: May need to adapt within this method, if "push up" happened and the trigger had been ordinary */
                            verticalBounce = BounceType.STATIC;
                            checkNf = false; // Skip NoFall.
                        }
                        
                        if (verticalBounce == BounceType.NO_BOUNCE && useBlockChangeTracker) { 
                            if (BounceUtil.checkPastStateBounceDescend(player, pFrom, pTo, thisMove, lastMove, tick, data, cc, blockChangeTracker) != BounceType.NO_BOUNCE) {
                                // Not set verticalBounce, as this is ascending and it's already force used.
                                checkNf = false; // Skip NoFall.
                            }
                        }
                    }
                }
                else {
                    if (
                            // Prepared bounce support.
                            data.verticalBounce != null && BounceUtil.onPreparedBounceSupport(player, from, to, thisMove, lastMove, tick, data)
                            // Past state bounce (includes prepending velocity / special calls).
                            || useBlockChangeTracker 
                            // 0-dist moves count in: && thisMove.yDistance >= 0.415 
                            && thisMove.yDistance <= 1.515
                        ) {
                        verticalBounce = BounceUtil.checkPastStateBounceAscend(player, pFrom, pTo, thisMove, lastMove, tick, pData, this, data, cc, blockChangeTracker);
                        if (verticalBounce != BounceType.NO_BOUNCE) checkNf = false;
                    }
                }

                // Might a bit tricky when it use to ensure no bounce check is active, not noFall checking here
                if (useBlockChangeTracker && checkNf && !checkPastStateVerticalPush(player, pFrom, pTo, thisMove, lastMove, tick, debug, data, cc)) {
                    checkPastStateHorizontalPush(player, pFrom, pTo, thisMove, lastMove, tick, debug, data, cc);
                }
            }
        }
        // No Sf or Cf check
        else {
            // TODO: Might still allow block change tracker with only passable enabled.
            useBlockChangeTracker = false;
            previousSetBackY = Double.NEGATIVE_INFINITY;
        }

        // 6: Check passable first to prevent set back override.
        // Passable is checked first to get the original set back locations from the other checks, if needed. 
        // TODO: Redesign to set set backs later (queue + invalidate).
        boolean mightSkipNoFall = false; // If to skip nofall check (mainly on violation of other checks).
        if (newTo == null && checkPassable && player.getGameMode() != BridgeMisc.GAME_MODE_SPECTATOR) {
            newTo = passable.check(player, pFrom, pTo, data, cc, pData, tick, useBlockChangeTracker);
            // Check if to skip the nofall check.
            if (newTo != null) mightSkipNoFall = true;
        }
        
        // 7: Recalculate explosion velocity as PlayerVelocityEvent can't handle well on 1.13+
        // TODO: Merge with velocity entries that were added at the same time with this one!
        if (data.shouldApplyExplosionVelocity) {
            data.shouldApplyExplosionVelocity = false;
            double xLastDistance = 0.0; double zLastDistance = 0.0; double yLastDistance = 0.0;

            if (lastMove.toIsValid) {
                xLastDistance = lastMove.to.getX() - lastMove.from.getX();
                zLastDistance = lastMove.to.getZ() - lastMove.from.getZ();
                yLastDistance = lastMove.to.onGround ? 0 : lastMove.yDistance; 
            }
            boolean addHorizontalVelocity = true;
            
            // Process the distances after the explosion
            final double xDistance2 = data.explosionVelAxisX + xLastDistance;
            final double zDistance2 = data.explosionVelAxisZ + zLastDistance;
            final double hDistance = Math.sqrt(xDistance2*xDistance2 + zDistance2*zDistance2);

            // Prevent duplicate entry come from PlayerVelocityEvent
            if (data.hasActiveHorVel() && data.getHorizontalFreedom() < hDistance 
                || data.hasQueuedHorVel() && data.useHorizontalVelocity(hDistance) < hDistance
                || !data.hasAnyHorVel()) {
                data.getHorizontalVelocityTracker().clear();
                if (debug) {
                    debug(player, "Prevent velocity duplication by removing entries that are smaller than the re-calculated hDistance (explosion).");
                }
            } 
            else addHorizontalVelocity = false;

            if (addHorizontalVelocity) {
                data.addVelocity(player, cc, xDistance2, data.explosionVelAxisY + yLastDistance - Magic.GRAVITY_ODD, zDistance2);
                if (debug) {
                    debug(player, "Fake use of explosion velocity (h/v).");
                }
            }
            else {
                data.addVerticalVelocity(new SimpleEntry(data.explosionVelAxisY + yLastDistance - Magic.GRAVITY_ODD, cc.velocityActivationCounter));
                if (debug) {
                    debug(player, "Fake use of vertical explosion velocity only. Horizontal velocity entries bigger than the re-calculated hDistance are present.");
                }
            }
            
            // Always reset once used
            data.explosionVelAxisX = 0.0;
            data.explosionVelAxisY = 0.0;
            data.explosionVelAxisZ = 0.0;
        }

        // 8: Handle the launch effect of bubble columns, coarse.
        // (NOTE: Bubble columns are possible only with source blocks, not flowing water.)
        if (checkSf && data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID && !lastMove.headObstructed
            && !pFrom.isDraggedByBubbleStream()) {
            
            if (!thisMove.from.inBubbleStream && lastMove.from.inBubbleStream
                && !data.hasQueuedVerVel() && data.insideBubbleStreamCount > 1 && thisMove.yDistance > 0.0
                && lastMove.yDistance > 0.0) {
                final double velocity = (0.05 * data.insideBubbleStreamCount) * data.lastFrictionVertical;
                data.addVerticalVelocity(new SimpleEntry(tick, velocity, data.insideBubbleStreamCount));
                data.setFrictionJumpPhase();
                if (debug) {
                    debug(player, "Add vertical velocity for bubble stream launch effect.");
                }
            }
            if (!data.hasQueuedHorVel() && thisMove.yDistance != 0.0
                && thisMove.hDistance > 0.1 && thisMove.hDistance < thisMove.walkSpeed && thisMove.from.inBubbleStream
                && BlockProperties.isAir(pFrom.getTypeIdAbove()) && data.insideBubbleStreamCount < 7
                && !Magic.inAir(thisMove)) {
                data.addHorizontalVelocity(new AccountEntry(0.5, 0, MovingData.getHorVelValCount(0.5)));
                data.addHorizontalVelocity(new AccountEntry(0.7, 1, MovingData.getHorVelValCount(0.7)));
                if (debug) {
                    debug(player, "Add horizontal velocity for bubble stream bounce effect on the surface.");
                }
            }
        }

        

        ////////////////////////////////////////////////////////////////////////
        // Run through the moving checks (Passable is checked above).         //
        ////////////////////////////////////////////////////////////////////////
        // 1: SurvivalFly first
        if (checkSf) {
            // 1.1: Prepare from, to, thisMove for full checking.
            // TODO: Could further differentiate if really needed to (newTo / NoFall).
            MovingUtil.prepareFullCheck(pFrom, pTo, thisMove, Math.max(cc.noFallyOnGround, cc.yOnGround));

            // 1.2: HACK: Add velocity for transitions between creativefly and survivalfly.
            if (lastMove.toIsValid && lastMove.flyCheck == CheckType.MOVING_CREATIVEFLY) { 
                final long tickHasLag = data.delayWorkaround + Math.round(200 / TickTask.getLag(200, true));
                if (data.delayWorkaround > time || tickHasLag < time) {
                    workaroundFlyCheckTransition(player, tick, debug, data, cc);
                    data.delayWorkaround = time;
                }
            }

            // 1.3: Actual check.
            // Only check if passable has not already set back.
            if (newTo == null) {
                thisMove.flyCheck = CheckType.MOVING_SURVIVALFLY;
                newTo = survivalFly.check(player, pFrom, pTo, multiMoveCount, data, cc, pData, tick, time, useBlockChangeTracker);
            }

            // 1.4: Only check NoFall, if not already vetoed.
            if (checkNf) {
                checkNf = noFall.isEnabled(player, pData);
            }
            
            // 1.5: Hover subcheck.
            if (newTo == null) {
                // TODO: Could reset for from-on-ground as well, for not too big moves.
                if (cc.sfHoverCheck && !(lastMove.toIsValid && lastMove.to.extraPropertiesValid && lastMove.to.onGroundOrResetCond) && !pTo.isOnGround()) {
                    // Start counting ticks.
                    hoverTicks.add(playerName);
                    data.sfHoverTicks = 0;
                }
                else data.sfHoverTicks = -1;

                // Still check for NoFall.
                if (checkNf) {
                    noFall.check(player, pFrom, pTo, previousSetBackY, data, cc, pData);
                }
            }
            else {
                if (checkNf && cc.sfSetBackPolicyFallDamage) {
                    if (!noFall.willDealFallDamage(player, from.getY(), previousSetBackY, data)) {
                        mightSkipNoFall = true;
                    }
                    // Check if to really skip.
                    else if (mightSkipNoFall) {
                        if (!pFrom.isOnGround() && !pFrom.isResetCond()) {
                            mightSkipNoFall = false;
                        }
                    }

                    // (Don't deal damage where no fall damage is possible.)
                    if (!mightSkipNoFall && (!pTo.isResetCond() || !pFrom.isResetCond())) {
                        noFall.checkDamage(player, Math.min(from.getY(), to.getY()), data, pData);
                    }
                }
            }
        }
        // 2: Then creativefly
        else if (checkCf) {
            if (newTo == null) {
                thisMove.flyCheck = CheckType.MOVING_CREATIVEFLY;
                newTo = creativeFly.check(player, pFrom, pTo, data, cc, pData, time, tick, useBlockChangeTracker);

                // Check for NoFall
                if (checkNf && noFall.isEnabled(player, pData)) {
                    noFall.check(player, pFrom, pTo, previousSetBackY, data, cc, pData);
                }
            }
            data.sfHoverTicks = -1;
            data.sfLowJump = false;
        }
        // No fly checking :(.
        else data.clearFlyData();

        // 3: Morepackets.
        if (pData.isCheckActive(CheckType.MOVING_MOREPACKETS, player) && (newTo == null || data.isMorePacketsSetBackOldest())) {
            /* Always check morepackets, if there is a chance that setting/overriding newTo is appropriate, to avoid packet speeding using micro-violations. */
            final Location mpNewTo = morePackets.check(player, pFrom, pTo, newTo == null, data, cc, pData);
            if (mpNewTo != null) {
                // Only override set back, if the morepackets set back location is older/-est. 
                if (newTo != null && debug) debug(player, "Override set back by the older morepackets set back.");
                newTo = mpNewTo;
            }
        }
        // Otherwise we need to clear their data.
        else data.clearPlayerMorePacketsData();
        

        ////////////////////////////////////////////
        // Reset jump amplifier if needed.        //
        ////////////////////////////////////////////
        if ((checkSf || checkCf) && jumpAmplifier != data.jumpAmplifier) {
            // TODO: General cool-down for latency?
            if (thisMove.touchedGround || !checkSf && (pFrom.isOnGround() || pTo.isOnGround())) {
                // (No need to check from/to for onGround, if SurvivalFly is to be checked.)
                data.jumpAmplifier = jumpAmplifier;
            }
        }

        
        ////////////////////////////////////////
        // Update BlockChangeTracker          //
        ////////////////////////////////////////
        if (useBlockChangeTracker && data.blockChangeRef.firstSpanEntry != null) {
            if (debug) debug(player, "BlockChangeReference: " + data.blockChangeRef.firstSpanEntry.tick + " .. " + data.blockChangeRef.lastSpanEntry.tick + " / " + tick);
            data.blockChangeRef.updateFinal(pTo);
        }
        
        
        //////////////////////////////////////////////
        // Check if the move is to be allowed       //
        //////////////////////////////////////////////
        // No check has requested a new to-Location (or actions are set not to cancel)
        if (newTo == null) {

            // 1: Ignore this one.
            if (data.hasTeleported()) {
                data.resetTeleported();
                if (debug) debug(player, "Ignore hook-induced set-back: actions not set to cancel.");
            }

            // 2: Process the bounce effect if the move is allowed.
            if (verticalBounce != BounceType.NO_BOUNCE) {
                BounceUtil.processBounce(player, pFrom.getY(), pTo.getY(), verticalBounce, tick, this, data, cc, pData);
            }

            // 3: Finish processing the current move, move it to past ones
            // TODO: More simple: UUID keys or a data flag instead?
            if (processingEvents.containsKey(playerName)) {
                data.playerMoves.finishCurrentMove();
            }
            // Teleport during violation processing, just invalidate thisMove.
            else thisMove.invalidate();
            // Increase time since set back.
            data.timeSinceSetBack ++;
            return false;
        }
        // A check has requested a new to-location.
        else {

            // 1: Setback override, adjust newTo.
            if (data.hasTeleported()) {
                if (debug) debug(player, "The set back has been overridden from (" + newTo + ") to: " + data.getTeleported());
                newTo = data.getTeleported();
            }
            if (debug) { // TODO: Remove, if not relevant (doesn't look like it was :p).
                if (verticalBounce != BounceType.NO_BOUNCE) debug(player, "Bounce effect not processed: " + verticalBounce);
                if (data.verticalBounce != null) debug(player, "Bounce effect not used: " + data.verticalBounce);  
            }

            // 2: Set back handling.
            prepareSetBack(player, event, newTo, data, cc, pData);

            // 3: Prevent freezing (e.g. ascending with gliding set in water, but moving normally).
            if ((thisMove.flyCheck == CheckType.MOVING_SURVIVALFLY || thisMove.flyCheck == CheckType.MOVING_CREATIVEFLY
                && pFrom.isInLiquid()) && Bridge1_9.isGlidingWithElytra(player)) {
                player.setGliding(false);            
            }
            return true;
        }
    }


    private void prepareCreativeFlyCheck(final Player player, final Location from, final Location to, 
                                         final PlayerMoveInfo moveInfo, final PlayerMoveData thisMove, final int multiMoveCount, 
                                         final int tick, final MovingData data, final MovingConfig cc) {

        data.adjustFlySpeed(player.getFlySpeed(), tick, cc.speedGrace);
        data.adjustWalkSpeed(player.getWalkSpeed(), tick, cc.speedGrace);
        // TODO: Adjust height of PlayerLocation more efficiently / fetch model early.
        final ModelFlying model = cc.getModelFlying(player, moveInfo.from, data, cc);
        if (MovingConfig.ID_JETPACK_ELYTRA.equals(model.getId())) {
            final MCAccess mcAccess = this.mcAccess.getHandle();
            MovingUtil.setElytraProperties(player, moveInfo.from, from, cc.yOnGround, mcAccess);
            MovingUtil.setElytraProperties(player, moveInfo.to, to, cc.yOnGround, mcAccess);
            thisMove.set(moveInfo.from, moveInfo.to);
            if (multiMoveCount > 0) thisMove.multiMoveCount = multiMoveCount;
        }
        thisMove.modelFlying = model;
    }

    
    /**
     * Vertical block push
     * @param player
     * @param from
     * @param to
     * @param thisMove
     * @param lastMove
     * @param debug
     * @param data
     * @param cc
     * @return
     */
    private boolean checkPastStateVerticalPush(final Player player, final PlayerLocation from, final PlayerLocation to,
                                               final PlayerMoveData thisMove, final PlayerMoveData lastMove, final int tick, 
                                               final boolean debug, final MovingData data, final MovingConfig cc) {

        final UUID worldId = from.getWorld().getUID();
        double amount = -1.0;
        boolean addvel = false;
        final BlockChangeEntry entryBelowY_POS = BlockChangeSearch(from, tick, Direction.Y_POS, debug, data, cc, worldId, true);

        if (
                // Center push..
                entryBelowY_POS != null
                // Off center push.
                //|| thisMove.yDistance < 0.6 && from.matchBlockChange(blockChangeTracker, data.blockChangeRef, Direction.Y_POS, Math.min(.415, thisMove.yDistance))
                ) {

            if (debug) {
                final StringBuilder builder = new StringBuilder(150);
                builder.append("Direct block push at (");
                builder.append("x:" + entryBelowY_POS.x);
                builder.append(" y:" + entryBelowY_POS.y);
                builder.append(" z:" + entryBelowY_POS.z);
                builder.append(" direction:" + entryBelowY_POS.direction.name());
                builder.append(")");
                debug(player, builder.toString());
            }
            /*
             * TODO: One case left still not covered, double ascend motions (0.25, 0.649)
             * Mostly happen while jumping into block and piston push!
             */
            if (lastMove.valid && thisMove.yDistance >= 0.0) {
                if ((from.isOnGroundOrResetCond() || thisMove.touchedGroundWorkaround) && from.isOnGround(1.0)) {
                    amount = Math.min(thisMove.yDistance, 0.5625);
                }
                else if (lastMove.yDistance < -Magic.GRAVITY_MAX) {
                    amount = Math.min(thisMove.yDistance, 0.34);
                }
                if (thisMove.yDistance == 0.0) amount = 0.0;
            }
            if (lastMove.toIsValid && amount < 0.0 && thisMove.yDistance < 0.0 
                && thisMove.yDistance > -1.515 && lastMove.yDistance >= 0.0) {
                amount = thisMove.yDistance;
                addvel = true;
            }
            if (entryBelowY_POS != null) {
                data.blockChangeRef.updateSpan(entryBelowY_POS);
            }
        }
        // Finally add velocity if set.
        if (amount >= 0.0 || addvel) {
            data.removeLeadingQueuedVerticalVelocityByFlag(VelocityFlags.ORIGIN_BLOCK_MOVE);
            /*
             * TODO: Concepts for limiting... max amount based on side
             * conditions such as block height+1.5, max coordinate, max
             * amount per use, ALLOW_ZERO flag/boolean and set in
             * constructor, demand max. 1 zero dist during validity. Bind
             * use to initial xz coordinates... Too precise = better with
             * past move tracking, or a sub-class of SimpleEntry with better
             * access signatures including thisMove.
             */
            final SimpleEntry vel = new SimpleEntry(tick, amount, VelocityFlags.ORIGIN_BLOCK_MOVE, 1);
            data.verticalBounce = vel;
            data.useVerticalBounce(player);
            data.useVerticalVelocity(thisMove.yDistance);
            if (debug) {
                debug(player, "checkPastStateVerticalPush: set velocity: " + vel);
            }
            return true;
        }
        return false;
    }


    /**
     * Search for blockchange entries.
     * @param from
     * @param tick
     * @param direction
     * @param debug
     * @param data
     * @param cc
     * @param worldId
     * @param searchBelow
     * @return
     */
    private BlockChangeEntry BlockChangeSearch(final PlayerLocation from, final int tick, Direction direction,
                                               final boolean debug, final MovingData data, final MovingConfig cc, 
                                               final UUID worldId, final boolean searchBelow) {

        final int iMinX = Location.locToBlock(from.getMinX());
        final int iMaxX = Location.locToBlock(from.getMaxX());
        final int iMinZ = Location.locToBlock(from.getMinZ());
        final int iMaxZ = Location.locToBlock(from.getMaxZ());
        final int belowY = from.getBlockY() - (searchBelow ? 1 : 0);
        for (int x = iMinX; x <= iMaxX; x++) {
            for (int z = iMinZ; z <= iMaxZ; z++) {
                for (int y = belowY; y <= belowY + 1; y++) {
                    BlockChangeEntry entryBelowY_POS = blockChangeTracker.getBlockChangeEntry(
                    data.blockChangeRef, tick, worldId, x, y, z, 
                    direction);
                    if (entryBelowY_POS != null) return entryBelowY_POS;
                }
            }
        }
        return null;
    }
    

    /**
     * Horizontal block push
     * @param player
     * @param from
     * @param to
     * @param thisMove
     * @param lastMove
     * @param debug
     * @param data
     * @param cc
     * @return
     */
    private boolean checkPastStateHorizontalPush(final Player player, final PlayerLocation from, final PlayerLocation to,
                                                 final PlayerMoveData thisMove, final PlayerMoveData lastMove, final int tick, 
                                                 final boolean debug, final MovingData data, final MovingConfig cc) {

        final UUID worldId = from.getWorld().getUID();
        final double xDistance = to.getX() - from.getX();
        final double zDistance = to.getZ() - from.getZ();
        final Direction dir;
        if (Math.abs(xDistance) > Math.abs(zDistance)) dir = xDistance > 0.0 ? Direction.X_POS : Direction.X_NEG;
        else  dir = zDistance > 0.0 ? Direction.Z_POS : Direction.Z_NEG;
    
        final BlockChangeEntry entry = BlockChangeSearch(from, tick, dir, debug, data, cc, worldId, false);
        if (entry != null) {
            final int count = MovingData.getHorVelValCount(0.6);
            // TODO: Clear active horizontal velocity?
            data.clearActiveHorVel();
            data.addHorizontalVelocity(new AccountEntry(tick, 0.6, count, count));
            // Stuck in block, Hack
            data.addVerticalVelocity(new SimpleEntry(-0.35, 6));
            data.blockChangeRef.updateSpan(entry);

            if (debug) {
                final StringBuilder builder = new StringBuilder(150);
                builder.append("Direct block push at (");
                builder.append("x:" + entry.x);
                builder.append(" y:" + entry.y);
                builder.append(" z:" + entry.z);
                builder.append(" direction:" + entry.direction.name());
                builder.append(")");
                debug(player, builder.toString());
                debug(player, "checkPastStateHorizontalPush: set velocity: " + 0.6);
            }
            return true;
        }
        return false;
    }


    /**
     * Check for extremely large moves. Initial intention is to prevent cheaters
     * from creating extreme load. SurvivalFly or CreativeFly is needed.
     * 
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     * @return
     */
    @SuppressWarnings("unused")
    private Location checkExtremeMove(final Player player, final PlayerLocation from, final PlayerLocation to, 
                                      final MovingData data, final MovingConfig cc) {

        // TODO: Find out why only CreativeFly does actually trigger.
        // TODO: Recent Minecraft versions allow a lot of unhealthy moves. Observed so far:
        // - Riptide + gliding (+ firework !?)
        // - Really high levitation levels
        // - Bouncing while riptiding
        // (- High jump amplifiers might be a problem as well)
        // One could also let Extreme_move block this stuff anyway, reason being: 
        // 1) we cannot support vanilla Minecraft features to arbitrary depths.
        // 2) Why would one let players perform such moves anyway (which is usually bad news for server-sided chunk loading)? 
        // [In fact, even Mojang attempted to mitigate this: https://www.minecraft.net/en-us/article/new-world-generation-java-available-testing]
        // Thinkble: configurable option to block such moves (cc.blockUnealthyMove / cc.blockLegitExtremeMoves)

        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove(); 
        final boolean riptideBounce = Bridge1_13.isRiptiding(player) && data.verticalBounce != null 
                                      && thisMove.yDistance < 8.0 && thisMove.yDistance > Magic.EXTREME_MOVE_DIST_HORIZONTAL; // At least ensure that cheaters cannot go any higher than a legit player.
        final boolean ripglide = Bridge1_9.isGlidingWithElytra(player) && Bridge1_13.isRiptiding(player) && thisMove.yDistance > Magic.EXTREME_MOVE_DIST_VERTICAL * 1.7;
        final boolean levitationHighLevel = !Double.isInfinite(Bridge1_9.getLevitationAmplifier(player)) && Bridge1_9.getLevitationAmplifier(player) >= 89 && Bridge1_9.getLevitationAmplifier(player) <= 127;
        // TODO: Latency effects.
        double violation = 0.0; // h + v violation (full move).
        // Vertical move.
        final boolean allowVerticalVelocity = false; // TODO: Configurable
        if (Math.abs(thisMove.yDistance) > Magic.EXTREME_MOVE_DIST_VERTICAL * (Bridge1_13.isRiptiding(player) ? 1.7 : 1.0)) {
            // Exclude valid moves first.
            // About 3.9 seems to be the positive maximum for velocity use in survival mode, regardless jump effect.
            // About -1.85 seems to be the negative maximum for velocity use in survival mode. Falling can result in slightly less than -3.
            if (lastMove.toIsValid && Math.abs(thisMove.yDistance) < Math.abs(lastMove.yDistance)
                && (thisMove.yDistance > 0.0 && lastMove.yDistance > 0.0 || thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0) 
                || allowVerticalVelocity && data.getOrUseVerticalVelocity(thisMove.yDistance) != null
                || riptideBounce || ripglide || levitationHighLevel) {
                // Speed decreased or velocity is present.
            }
            else violation += thisMove.yDistance; // Could subtract lastMove.yDistance.
        }

        // Horizontal move.
        if (thisMove.hDistance > Magic.EXTREME_MOVE_DIST_HORIZONTAL) {
            // Exclude valid moves first.
            // TODO: Attributes might allow unhealthy moves as well.
            // Observed maximum use so far: 5.515
            // TODO: Velocity flag too (if combined with configurable distances)?
            final double amount = thisMove.hDistance - data.getHorizontalFreedom(); // Will change with model change.
            if (amount < 0.0 || lastMove.toIsValid && thisMove.hDistance - lastMove.hDistance <= 0.0 
                || data.useHorizontalVelocity(amount) >= amount) {
                // Speed decreased or velocity is present.
            }
            else violation += thisMove.hDistance; // Could subtract lastMove.hDistance.
        }

        if (violation > 0.0) {
            // Ensure a set back location is present.
            if (!data.hasSetBack()) data.setSetBack(from);
            // Process violation as sub check of the appropriate fly check.
            violation *= 100.0;
            final Check check;
            final ActionList actions;
            final double vL;

            if (thisMove.flyCheck == CheckType.MOVING_SURVIVALFLY) {
                check = survivalFly;
                actions = cc.survivalFlyActions;
                data.survivalFlyVL += violation;
                vL = data.survivalFlyVL;
            }
            else {
                check = creativeFly;
                actions = cc.creativeFlyActions;
                data.creativeFlyVL += violation;
                vL = data.creativeFlyVL;
            }
            final ViolationData vd = new ViolationData(check, player, vL, violation, actions);
            // TODO: Reduce copy and paste (method to fill in locations, once using exact coords and latering default actions).
            if (vd.needsParameters()) {
                vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", from.getX(), from.getY(), from.getZ()));
                vd.setParameter(ParameterName.LOCATION_TO, String.format(Locale.US, "%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ()));
                vd.setParameter(ParameterName.DISTANCE, String.format(Locale.US, "%.2f", TrigUtil.distance(from, to)));
                vd.setParameter(ParameterName.TAGS, "EXTREME_MOVE");
            }
            // Some resetting is done in MovingListener.
            if (check.executeActions(vd).willCancel()) {
                // Set back + view direction of to (more smooth).
                return MovingUtil.getApplicableSetBackLocation(player, to.getYaw(), to.getPitch(), from, data, cc);
            }
        }
        // No cancel intended.
        return null;
    }


    /**
     * Add velocity, in order to work around issues with transitions between Fly checks.
     * Asserts last distances are set.
     * 
     * @param tick
     * @param data
     */
    private void workaroundFlyCheckTransition(final Player player, final int tick, final boolean debug, 
                                              final MovingData data, final MovingConfig cc) {

        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final double amount = guessVelocityAmount(player, data.playerMoves.getCurrentMove(), lastMove, data, cc);
        data.clearActiveHorVel(); // Clear active velocity due to adding actual speed here.
        data.bunnyhopDelay = 0; // Remove bunny hop due to add velocity 
        if (amount > 0.0) data.addHorizontalVelocity(new AccountEntry(tick, amount, cc.velocityActivationCounter, MovingData.getHorVelValCount(amount)));
        data.addVerticalVelocity(new SimpleEntry(lastMove.yDistance, cc.velocityActivationCounter));
        data.addVerticalVelocity(new SimpleEntry(0.0, cc.velocityActivationCounter));
        data.setFrictionJumpPhase();
        if (debug) debug(player, "*** Transition from CreativeFly to SurvivalFly: Add velocity.");
    }


    private static double guessVelocityAmount(final Player player, final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
                                              final MovingData data, final MovingConfig cc) {

        // Default margin: Allow slightly less than the previous speed.
        final double defaultAmount = lastMove.hDistance * (1.0 + Magic.FRICTION_MEDIUM_AIR) / 2.0;
        // Test for exceptions.
        if (Bridge1_9.isWearingElytra(player) && lastMove.modelFlying != null && lastMove.modelFlying.getId().equals(MovingConfig.ID_JETPACK_ELYTRA)) {
            // Still elytra move, not forcing CreativeFly check, just pass the res to velocity
            final double[] res = CreativeFly.guessElytraVelocityAmount(player, thisMove, lastMove, data);
            //data.addVerticalVelocity(new SimpleEntry(lastMove.yDistance < -0.1034 ? (lastMove.yDistance * Magic.FRICTION_MEDIUM_AIR + 0.1034) 
            //                                        : lastMove.yDistance, cc.velocityActivationCounter));
            data.keepfrictiontick = -15;
            data.addVerticalVelocity(new SimpleEntry(res[1], cc.velocityActivationCounter));
            return res[0];
            //if (thisMove.hDistance > defaultAmount) {
                // Allowing the same speed won't always work on elytra (still increasing, differing modeling on client side with motXYZ).
                // (Doesn't seem to be overly effective.)
            //    if (data.fireworksBoostDuration > 0) {
            //        return 2.0;
            //    } 
            //    else if (lastMove.toIsValid && lastMove.hAllowedDistance > 0.0) return lastMove.hAllowedDistance; // This one might replace below?
            //    return defaultAmount + 0.5;
            //}
        }
        else if (lastMove.modelFlying != null && lastMove.modelFlying.getId().equals(MovingConfig.ID_EFFECT_RIPTIDING)){
            data.addVerticalVelocity(new SimpleEntry(0.0, 10)); // Not using cc.velocityActivationCounter to be less exploitable.
            data.keepfrictiontick = -7;
        }
        return defaultAmount;
    }


    /**
     * Called during PlayerMoveEvent for adjusting to a to-be-done/scheduled set
     * back. <br>
     * NOTE: Meaning differs from data.onSetBack (to be cleaned up).
     * 
     * @param player
     * @param event
     * @param newTo
     *            Must be a cloned or new Location instance, free for whatever
     *            other plugins do with it.
     * @param data
     * @param cc
     */
    private void prepareSetBack(final Player player, final PlayerMoveEvent event, final Location newTo, 
                                final MovingData data, final MovingConfig cc, final IPlayerData pData) {

        // Illegal Yaw/Pitch.
        if (LocUtil.needsYawCorrection(newTo.getYaw())) {
            newTo.setYaw(LocUtil.correctYaw(newTo.getYaw()));
        }
        if (LocUtil.needsPitchCorrection(newTo.getPitch())) {
            newTo.setPitch(LocUtil.correctPitch(newTo.getPitch()));
        }
        // Reset some data.
        data.prepareSetBack(newTo);
        aux.resetPositionsAndMediumProperties(player, newTo, data, cc); // TODO: Might move into prepareSetBack, experimental here.

        // Set new to-location, distinguish method by settings.
        final PlayerSetBackMethod method = cc.playerSetBackMethod;
        if (method.shouldSetTo()) {
            event.setTo(newTo); // LEGACY: pre-2017-03-24
            if (pData.isDebugActive(checkType)) debug(player, "Set back type: SET_TO");
        }
        if (method.shouldCancel()) {
            event.setCancelled(true);
            if (pData.isDebugActive(checkType)) debug(player, "Set back type: CANCEL (schedule:" + method.shouldSchedule() + " updatefrom:" + method.shouldUpdateFrom() + ")");
        } 
        else if (pData.isDebugActive(checkType)) debug(player, "No setback performed!");
        // NOTE: A teleport is scheduled on MONITOR priority, if set so.
        // TODO: enforcelocation?
        // Debug.
        if (pData.isDebugActive(checkType)) {
            debug(player, "Prepare set back to: " + newTo.getWorld().getName() + "/" + LocUtil.simpleFormatPosition(newTo) + " (" + method.getId() + ")");
        }
    }


    /**
     * Monitor level PlayerMoveEvent. Uses useLoc.
     * @param event
     */
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMoveMonitor(final PlayerMoveEvent event) {

        // TODO: Use stored move data to verify if from/to have changed (thus a teleport will result, possibly a minor issue due to the teleport).
        final long now = System.currentTimeMillis();
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        // This means moving data has been reset by a teleport.
        if (processingEvents.remove(player.getName()) == null) return;
        if (player.isDead() || player.isSleeping()) return;
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        // Feed combined check.
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        data.lastMoveTime = now; 
        final Location from = event.getFrom();
        // Feed yawrate and reset moving data positions if necessary.
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        final int tick = TickTask.getTick();
        final MovingConfig mCc = pData.getGenericInstance(MovingConfig.class);

        if (!event.isCancelled()) {
            final Location pLoc = player.getLocation(useLoc);
            onMoveMonitorNotCancelled(player, TrigUtil.isSamePosAndLook(pLoc, from) ? from : pLoc, event.getTo(), now, tick, data, mData, mCc, pData);
            useLoc.setWorld(null);
        }
        else onCancelledMove(player, from, tick, now, mData, mCc, data, pData);
    }


    /**
     * Adjust data for a cancelled move. No teleport event will fire, but an
     * outgoing position is sent. Note that event.getFrom() may be overridden by
     * a plugin, which the server will ignore, but can lead to confusion.
     * 
     * @param player
     * @param from
     * @param tick
     * @param now
     * @param mData
     * @param data
     */
    private void onCancelledMove(final Player player, final Location from, final int tick, final long now, 
                                 final MovingData mData, final MovingConfig mCc, final CombinedData data,
                                 final IPlayerData pData) {

        final boolean debug = pData.isDebugActive(checkType);
        // Detect our own set back, choice of reference location.
        if (mData.hasTeleported()) {
            final Location ref = mData.getTeleported();
            // Initiate further action depending on settings.
            final PlayerSetBackMethod method = mCc.playerSetBackMethod;
            if (method.shouldUpdateFrom()) {
                // Attempt to do without a PlayerTeleportEvent as follow up.
                // TODO: Doing this on MONITOR priority is problematic, despite optimal.
                LocUtil.set(from, ref);
            }
            if (method.shouldSchedule()) {
                // Schedule the teleport, because it might be faster than the next incoming packet.
                final IPlayerData pd = DataManager.getPlayerData(player);
                if (pd.isPlayerSetBackScheduled()) debug(player, "Teleport (set back) already scheduled to: " + ref);
                else if (debug) {
                    pd.requestPlayerSetBack();
                    if (debug)  debug(player, "Schedule teleport (set back) to: " + ref);
                }
            }
            // (Position adaption will happen with the teleport on tick, or with the next move.)
        }

        // Assume the implicit teleport to the from-location (no Bukkit event fires).
        Combined.resetYawRate(player, from.getYaw(), now, false, pData); // Not reset frequency, but do set yaw.
        aux.resetPositionsAndMediumProperties(player, from, mData, mCc); 
        // TODO: Should probably leave this to the teleport event!
        mData.resetTrace(player, from, tick, mcAccess.getHandle(), mCc);

        // Expect a teleport to the from location (packet balance, no Bukkit event will fire).
        if (pData.isCheckActive(CheckType.NET_FLYINGFREQUENCY, player)) { // TODO: A summary method.
            pData.getGenericInstance(NetData.class).teleportQueue.onTeleportEvent(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        }
    }


    /**
     * Uses useLoc if in vehicle.
     * @param player
     * @param from Might use useLoc, but will reset it, if in vehicle.
     * @param to Do not use useLoc for this.
     * @param now
     * @param tick
     * @param data
     * @param mData
     */
    private void onMoveMonitorNotCancelled(final Player player, final Location from, final Location to, 
                                           final long now, final long tick, final CombinedData data, 
                                           final MovingData mData, final MovingConfig mCc, final IPlayerData pData) {

        final String toWorldName = to.getWorld().getName();
        Combined.feedYawRate(player, to.getYaw(), now, toWorldName, data, pData);
        // TODO: maybe even not count vehicles at all ?
        if (player.isInsideVehicle()) {
            // TODO: refine (!).
            final Location ref = player.getVehicle().getLocation(useLoc);
            aux.resetPositionsAndMediumProperties(player, ref, mData, mCc); // TODO: Consider using to and intercept cheat attempts in another way.
            useLoc.setWorld(null);
            mData.updateTrace(player, to, tick, mcAccess.getHandle()); // TODO: Can you become invincible by sending special moves?
        }
        else if (!from.getWorld().getName().equals(toWorldName)) {
            // A teleport event should follow.
            aux.resetPositionsAndMediumProperties(player, to, mData, mCc);
            mData.resetTrace(player, to, tick, mcAccess.getHandle(), mCc);
        }
        else {
            // TODO: Detect differing location (a teleport event would follow).
            final PlayerMoveData lastMove = mData.playerMoves.getFirstPastMove();
            if (!lastMove.toIsValid || !TrigUtil.isSamePos(to, lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ())) {
                // Something odd happened, e.g. a set back.
                aux.resetPositionsAndMediumProperties(player, to, mData, mCc);
            }
            else {
                // Normal move, nothing to do.
            }
            mData.updateTrace(player, to, tick, mcAccess.getHandle());
            if (mData.hasTeleported()) onPlayerMoveMonitorNotCancelledHasTeleported(player, to, mData, pData, pData.isDebugActive(checkType));
        }
    }


    private void onPlayerMoveMonitorNotCancelledHasTeleported(final Player player, final Location to, 
                                                              final MovingData mData, final IPlayerData pData, 
                                                              final boolean debug) {

        if (mData.isTeleportedPosition(to)) {
            // Skip resetting, especially if legacy setTo is enabled.
            // TODO: Might skip this condition, if legacy setTo is not enabled.
            if (debug) debug(player, "Event not cancelled, with teleported (set back) set, assume legacy behavior.");
            return;
        }
        else if (pData.isPlayerSetBackScheduled()) {
            // Skip, because the scheduled teleport has been overridden.
            // TODO: Only do this, if cancel is set, because it is not an un-cancel otherwise.
            if (debug) debug(player, "Event not cancelled, despite a set back has been scheduled. Cancel set back.");
            mData.resetTeleported(); // (PlayerTickListener will notice it's not set.)
        }
        else {
            if (debug) debug(player, "Inconsistent state (move MONITOR): teleported has been set, but no set back is scheduled. Ignore set back.");
            mData.resetTeleported();
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPortalLowest(final PlayerPortalEvent event) {

        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (MovingUtil.hasScheduledPlayerSetBack(player)) {
            if (pData.isDebugActive(checkType)) debug(player, "[PORTAL] Prevent use, due to a scheduled set back.");
            event.setCancelled(true);
        }
    }


    /**
     * When a player uses a portal, all information related to the moving checks becomes invalid.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPortal(final PlayerPortalEvent event) {

        final Location to = event.getTo();
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        final MovingData data = pData.getGenericInstance(MovingData.class);
        if (pData.isDebugActive(checkType)) debug(event.getPlayer(), "[PORTAL] to=" + to);
        // TODO: This should be redundant, might remove anyway.
        // TODO: Rather add something like setLatencyImmunity(...ms / conditions).
        if (to != null) data.clearMostMovingCheckData();
        
    }


    /**
     * Clear fly data on death.
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {

        final Player player = event.getEntity();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        //final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        data.clearMostMovingCheckData();
        data.setSetBack(player.getLocation(useDeathLoc)); // TODO: Monitor this change (!).
        data.isUsingItem = false;
        // Log location.
        if (pData.isDebugActive(checkType)) debug(player, "Death: " + player.getLocation(useDeathLoc));
        useDeathLoc.setWorld(null);
    }


    /**
     * LOWEST: Checks, indicate cancel processing player move.
     * 
     * @param event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerTeleportLowest(final PlayerTeleportEvent event) {

        final Player player = event.getPlayer();
        // Prevent further moving processing for nested events.
        processingEvents.remove(player.getName());
        // Various early return conditions.
        if (event.isCancelled()) return;

        final TeleportCause cause = event.getCause();
        switch(cause) {
            case COMMAND:
            case ENDER_PEARL:
                break;
            default:
                return;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        final boolean debug = pData.isDebugActive(checkType);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final Location to = event.getTo();

        if (to == null) {
            // Better cancel this one.
            if (!event.isCancelled()) {
                if (debug) debugTeleportMessage(player, event, "Cancel event, that has no target location (to) set.");
                event.setCancelled(true);
            }
            return;
        }

        if (data.hasTeleported()) {
            // More lenient: accept the position.
            if (data.isTeleportedPosition(to))  return;
            else {
                if (debug) debugTeleportMessage(player, event, "Prevent teleport, due to a scheduled set back: ", to);
                event.setCancelled(true);
                return;
            }
        }

        // Run checks.
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        boolean cancel = false;
        // Ender pearl into blocks.
        if (cause == TeleportCause.ENDER_PEARL) {
            if (pData.getGenericInstance(CombinedConfig.class).enderPearlCheck && !BlockProperties.isPassable(to) && 
                    blockChangeTracker.getBlockChangeEntry(null, TickTask.getTick(), to.getWorld().getUID(), to.getBlockX(), to.getBlockY(), to.getBlockZ(), null) == null) { // || !BlockProperties.isOnGroundOrResetCond(player, to, 1.0)) {
                // Not check on-ground: Check the second throw.
                // TODO: Bounding box check or onGround as replacement?
                cancel = true;
            }
        }
        // Teleport to untracked locations.
        else if (cause == TeleportCause.COMMAND) { // TODO: TeleportCause.PLUGIN?
            // Attempt to prevent teleporting to players inside of blocks at untracked coordinates.
            if (cc.passableUntrackedTeleportCheck) {
                if (cc.loadChunksOnTeleport) MovingUtil.ensureChunksLoaded(player, to, "teleport", data, cc, pData);
                if (cc.passableUntrackedTeleportCheck && MovingUtil.shouldCheckUntrackedLocation(player, to, pData)) {
                    final Location newTo = MovingUtil.checkUntrackedLocation(to);
                    if (newTo != null) {
                        // Adjust the teleport to go to the last tracked to-location of the other player.
                        event.setTo(newTo);
                        // TODO: Consider console, consider debug.
                        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, player.getName() + " correct untracked teleport destination (" + to + " corrected to " + newTo + ").");
                    }
                }
            }
        }
        // (Here event.setTo might've been called, unless cancel is set.)

        // Handle cancel.
        if (cancel) {
            // NCP actively prevents this teleport.
            event.setCancelled(true);
            if (debug) debug(player, "TP " + cause + " (cancel): " + to);
        }
    }


    /**
     * HIGHEST: Revert cancel on set back.
     * 
     * @param event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        // Only check cancelled events.
        if (event.isCancelled()) checkUndoCancelledSetBack(event);
    }


    /**
     * Called for cancelled events only, before EventPriority.MONITOR.
     * 
     * @param event
     */
    private void checkUndoCancelledSetBack(final PlayerTeleportEvent event) {

        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        // Revert cancel on set back (only precise match).
        // Teleport by NCP.
        // TODO: What if not scheduled.
        if (data.hasTeleported()) undoCancelledSetBack(player, event, data, pData);
    }


    private final void undoCancelledSetBack(final Player player, final PlayerTeleportEvent event,
                                            final MovingData data, final IPlayerData pData) {

        // Prevent cheaters getting rid of flying data (morepackets, other).
        // TODO: even more strict enforcing ?
        event.setCancelled(false); // TODO: Does this make sense? Have it configurable rather?
        if (!data.isTeleported(event.getTo())) {
            final Location teleported = data.getTeleported();
            event.setTo(teleported);
            /*
             * Setting from ... not sure this is relevant. Idea was to avoid
             * subtleties with other plugins, but it probably can't be
             * estimated, if this means more or less 'subtleties' in the end
             * (amortized).
             */
            event.setFrom(teleported);
        }
        if (pData.isDebugActive(checkType)) {
            NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, player.getName() + " TP " + event.getCause()+ " (revert cancel on set back): " + event.getTo());
        }
    }


    /**
     * MONITOR: Adjust data to what happened.
     * 
     * @param event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onPlayerTeleportMonitor(final PlayerTeleportEvent event) {

        // Evaluate result and adjust data.
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        // Invalidate first-move thing.
        // TODO: Might conflict with 'moved wrongly' on join.
        data.joinOrRespawn = false;
        
        // Special cases.
        final Location to = event.getTo();
        if (event.isCancelled()) {
            onPlayerTeleportMonitorCancelled(player, event, to, data, pData);
            return;
        }
        else if (to == null) {
            // Weird event.
            onPlayerTeleportMonitorNullTarget(player, event, to, data, pData);
            return;
        }
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        // Detect our own player set backs.
        if (data.hasTeleported() && onPlayerTeleportMonitorHasTeleported(player, event, to, data, cc, pData)) {
            return;
        }

        boolean skipExtras = false; // Skip extra data adjustments during special teleport, e.g. vehicle set back.
        // Detect our own vehicle set backs (...).
        if (data.isVehicleSetBack) {
            // Uncertain if this is vehicle leave or vehicle enter.
            if (event.getCause() != BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION) {
                // TODO: Unexpected, what now?
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.STATUS, 
                        CheckUtils.getLogMessagePrefix(player, CheckType.MOVING_VEHICLE) 
                        + "Unexpected teleport cause on vehicle set back: " + event.getCause());
            }
            // TODO: Consider to verify, if this is somewhere near the vehicle as expected (might need storing more data for a set back).
            skipExtras = true;
        }

        // Normal teleport
        final double fallDistance = data.noFallFallDistance;
        //        final LiftOffEnvelope oldEnv = data.liftOffEnvelope; // Remember for workarounds.
        data.clearFlyData();
        data.clearPlayerMorePacketsData();
        data.setSetBack(to);
        data.sfHoverTicks = -1; // Important against concurrent modification exception.
        if (cc.loadChunksOnTeleport) MovingUtil.ensureChunksLoaded(player, to, "teleport", data, cc, pData);
        aux.resetPositionsAndMediumProperties(player, to, data, cc);
        // Reset stuff.
        Combined.resetYawRate(player, to.getYaw(), System.currentTimeMillis(), true, pData); // TODO: Not sure.
        data.resetTeleported();

        if (!skipExtras) {
            // Adjust fall distance, if set so.
            // TODO: How to account for plugins that reset the fall distance here?
            // TODO: Detect transition from valid flying that needs resetting the fall distance.
            if (event.getCause() == TeleportCause.UNKNOWN || event.getCause() == TeleportCause.COMMAND) {
                // Always keep fall damage.
                player.setFallDistance((float) fallDistance);
                data.noFallFallDistance = (float) fallDistance;
                
                // TEST: Detect jumping on a just placed fence.
                // 1. Detect the low correction teleport.
                // 2. Detect the fence.
                // 3. Verify the past move.
                // (4. Check for a block change entry or last placed block.)
                // TODO: REMOVE TEST
            }
            else if (fallDistance > 1.0 && fallDistance - player.getFallDistance() > 0.0) {
                // Reset fall distance if set so in the config.
                if (!cc.noFallTpReset) {
                    // (Set fall distance if set to not reset.)
                    player.setFallDistance((float) fallDistance);
                    data.noFallFallDistance = (float) fallDistance;
                }
                else if (fallDistance >= Magic.FALL_DAMAGE_DIST) {
                    data.noFallSkipAirCheck = true;
                }
            }
            if (event.getCause() == TeleportCause.ENDER_PEARL) {
                // Prevent NoFall violations for ender-pearls.
                data.noFallSkipAirCheck = true;
            }
        }

        // Cross world teleportation issues with the end.
        final Location from = event.getFrom();
        if (from != null 
            && event.getCause() == TeleportCause.END_PORTAL // Currently only related to this.
            &&!from.getWorld().getName().equals(to.getWorld().getName())) { // Less java, though.
            // Remember the position teleported from.
            data.crossWorldFrom = new SimplePositionWithLook(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
        }
        else {
            // Reset last cross world position.
            data.crossWorldFrom = null;
        }

        // Log.
        if (pData.isDebugActive(checkType)) {
            debugTeleportMessage(player, event, "(normal)", to);
        }
    }


    /**
     * 
     * @param player
     * @param event
     * @param to
     * @param data
     * @param cc
     * @return True, if processing the teleport event should be aborted, false
     *         otherwise.
     */
    private boolean onPlayerTeleportMonitorHasTeleported(final Player player, final PlayerTeleportEvent event, 
                                                         final Location to, final MovingData data, final MovingConfig cc, 
                                                         final IPlayerData pData) {

        if (data.isTeleportedPosition(to)) {
            // Set back.
            confirmSetBack(player, true, data, cc, pData, to);
            // Reset some more data.
            // TODO: Some more?
            data.reducePlayerMorePacketsData(1);
            // Log.
            if (pData.isDebugActive(checkType)) {
                debugTeleportMessage(player, event, "(set back)", to);
            }
            return true;
        }
        else {
            /*
             * In this case another plugin has prevented NCP cancelling that
             * teleport during before this EventPriority stage, or another
             * plugin has altered the target location (to).
             */
            NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(
                    Streams.TRACE_FILE, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) 
                    + " TP " + event.getCause() + " (set back was overridden): " + to);
            return false;
        }
    }


    /**
     * A set back has been performed, or applying it got through to
     * EventPriority.MONITOR.
     * 
     * @param player
     * @param fakeNews
     *            True, iff it's not really been applied yet (, but should get
     *            applied, due to reaching EventPriority.MONITOR).
     * @param data
     * @param cc
     */
    private void confirmSetBack(final Player player, final boolean fakeNews, final MovingData data, 
                                final MovingConfig cc, final IPlayerData pData, final Location fallbackTeleported) {

        // TODO: Find the reason why it can be null even passed the precondition not null.
        final Location teleported = data.getTeleported();
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        moveInfo.set(player, teleported != null ? teleported : fallbackTeleported, null, cc.yOnGround);
        if (cc.loadChunksOnTeleport) {
            MovingUtil.ensureChunksLoaded(player, teleported != null ? teleported : fallbackTeleported, 
                    "teleport", data, cc, pData);
        }
        data.onSetBack(moveInfo.from);
        aux.returnPlayerMoveInfo(moveInfo);
        // Reset stuff.
        Combined.resetYawRate(player, teleported.getYaw(), System.currentTimeMillis(), true, pData); // TODO: Not sure.
        data.resetTeleported();
    }


    private void onPlayerTeleportMonitorCancelled(final Player player, final PlayerTeleportEvent event, 
                                                  final Location to, final MovingData data, final IPlayerData pData) {

        if (data.isTeleported(to)) {
            // (Only precise match.)
            // TODO: Schedule a teleport to set back with PlayerData (+ failure count)?
            // TODO: Log once per player always?
            NCPAPIProvider.getNoCheatPlusAPI().getLogManager().warning(Streams.TRACE_FILE, CheckUtils.getLogMessagePrefix(player, CheckType.MOVING) 
                                                                       +  " TP " + event.getCause() + " (set back was prevented): " + to);
        }
        else {
            if (pData.isDebugActive(checkType)) {
                debugTeleportMessage(player, event, to);
            }
        }
        data.resetTeleported();
    }


    private void onPlayerTeleportMonitorNullTarget(final Player player, 
                                                   final PlayerTeleportEvent event, final Location to, 
                                                   final MovingData data, final IPlayerData pData) {

        final boolean debug = pData.isDebugActive(checkType);
        if (debug) {
            debugTeleportMessage(player, event, "No target location (to) set.");
        }

        if (data.hasTeleported()) {
            if (DataManager.getPlayerData(player).isPlayerSetBackScheduled()) {
                // Assume set back event following later.
                event.setCancelled(true);
                if (debug) debugTeleportMessage(player, event, "Cancel, due to a scheduled set back.");
                
            }
            else {
                data.resetTeleported();
                if (debug) debugTeleportMessage(player, event, "Skip set back, not being scheduled.");
            }
        }
    }


    /**
     * 
     * @param player
     * @param event
     * @param message
     * @param extra
     *            Added in the end, with a leading space each.
     */
    private void debugTeleportMessage(final Player player, final PlayerTeleportEvent event, 
                                      final Object... extra) {

        final StringBuilder builder = new StringBuilder(128);
        builder.append("TP ");
        builder.append(event.getCause());
        if (event.isCancelled()) {
            builder.append(" (cancelled)");
        }
        if (extra != null && extra.length > 0) {
            for (final Object obj : extra) {
                if (obj != null) {
                    builder.append(' ');
                    if (obj instanceof String) {
                        builder.append((String) obj);
                    }
                    else {
                        builder.append(obj.toString());
                    }
                }
            }
        }
        debug(player, builder.toString());
    }


    /**
     * Player got a velocity packet. The server can't keep track of actual velocity values (by design), so we have to
     * try and do that ourselves. Very rough estimates.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerVelocity(final PlayerVelocityEvent event) {

        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        // Ignore players who are in vehicles.
        if (player.isInsideVehicle()) {
            data.removeAllVelocity();
            return;
        }
        // Process velocity.
        final Vector velocity = event.getVelocity();
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        data.addVelocity(player, cc, velocity.getX(), velocity.getY(), velocity.getZ());
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(final EntityDamageEvent event) {

        if (event.getCause() != DamageCause.FALL) {
            return;
        }
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        checkFallDamageEvent((Player) entity, event);
    }


    private void checkFallDamageEvent(final Player player, final EntityDamageEvent event) {

        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        if (player.isInsideVehicle()) {
            // Ignore vehicles (noFallFallDistance will be inaccurate anyway).
            data.clearNoFallData();
            return;
        }
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final double yOnGround = Math.max(cc.noFallyOnGround, cc.yOnGround);
        final Location loc = player.getLocation(useFallLoc);
        moveInfo.set(player, loc, null, yOnGround);
        final PlayerLocation pLoc = moveInfo.from;
        pLoc.collectBlockFlags(yOnGround);

        if (event.isCancelled() || !MovingUtil.shouldCheckSurvivalFly(player, pLoc, moveInfo.to, data, cc, pData) 
            || !noFall.isEnabled(player, pData)) {
            data.clearNoFallData();
            useFallLoc.setWorld(null);
            aux.returnPlayerMoveInfo(moveInfo);
            return;
        }
        final boolean debug = pData.isDebugActive(CheckType.MOVING_NOFALL);
        boolean allowReset = true;
        float fallDistance = player.getFallDistance();
        final float yDiff = (float) (data.noFallMaxY - loc.getY());
        final double damage = BridgeHealth.getRawDamage(event); // Raw damage.
        if (debug) debug(player, "Damage(FALL/PRE): " + damage + " / mc=" + player.getFallDistance() + " nf=" + data.noFallFallDistance + " yDiff=" + yDiff);

        // NoFall bypass checks.
        // TODO: data.noFallSkipAirCheck is used to skip checking in general, thus move into that block or not?
        // TODO: Could consider skipping accumulated fall distance for NoFall in general as well.
        if (!data.noFallSkipAirCheck) {
            
            // Cheat: let Minecraft gather and deal fall damage.
            final float dataDist = Math.max(yDiff, data.noFallFallDistance);
            final double dataDamage = NoFall.getDamage(dataDist);
            if (damage > dataDamage + 0.5 || dataDamage <= 0.0) {

                // Hot fix: allow fall damage in lava.
                // TODO: Correctly model the half fall distance per in-lava move and taking fall damage in lava. 
                // TODO: Also relate past y-distance(s) to the fall distance (mc).
                // Original issue: https://github.com/NoCheatPlus/Issues/issues/439#issuecomment-299300421
                final PlayerMoveData firstPastMove = data.playerMoves.getFirstPastMove();
                if (pLoc.isOnGround() && pLoc.isInLava() && firstPastMove.toIsValid && firstPastMove.yDistance < 0.0) {
                    if (debug) debug(player, "NoFall/Damage: allow fall damage in lava (hotfix).");
                } 
                // Fix issues when gliding down vines with elytra.
                // Checking for velocity does not work since sometimes it can be applied after this check runs
                // TODO: Actually find out why NoFall is running at all when still gliding, rather.
                else if (moveInfo.from.isOnClimbable() 
                        && (firstPastMove.modelFlying != null && firstPastMove.modelFlying.getVerticalAscendGliding()
                        || firstPastMove.elytrafly || thisMove.modelFlying != null && thisMove.modelFlying.getVerticalAscendGliding()
                        || thisMove.elytrafly)) {
                    if (debug) debug(player, "Ignore fakefall on climbable on elytra move");
                }
                // NOTE: Double violations are possible with the in-air check below.
                // TODO: Differing sub checks, once cancel action...
                else if (noFallVL(player, "fakefall", data, cc)) {
                    player.setFallDistance(dataDist);
                    if (dataDamage <= 0.0) {
                        // Cancel the event.
                        event.setCancelled(true);
                        useFallLoc.setWorld(null);
                        aux.returnPlayerMoveInfo(moveInfo);
                        return;
                    }
                    else {
                        // Adjust and continue.
                        if (debug) debug(player, "NoFall/Damage: override player fall distance and damage (" + fallDistance + " -> " + dataDist + ").");
                        fallDistance = dataDist;
                        BridgeHealth.setRawDamage(event, dataDamage);
                    }
                }
            }
            // Be sure not to lose that block.
            // TODO: What is this and why is it right here?
            // Answer: https://github.com/NoCheatPlus/NoCheatPlus/commit/50ebbb01fb3998f5e4ebba741ed6cbd318de00c5
            data.noFallFallDistance += 1.0; 
            // TODO: Account for liquid too?
            // Cheat: set ground to true in-air. Cancel the event and restore fall distance. NoFall data will not be reset 
            if (!pLoc.isOnGround(1.0, 0.3, 0.1) && !pLoc.isResetCond() && !pLoc.isAboveLadder() && !pLoc.isAboveStairs()) {
                if (noFallVL(player, "fakeground", data, cc) && data.hasSetBack()) {
                    allowReset = false;
                }
            }
            // Legitimate damage: clear accounting data.
            // TODO: Why only reset in case of !data.noFallSkipAirCheck?
            // TODO: Also reset in other cases (moved too quickly)?
            else data.vDistAcc.clear();
        }
        aux.returnPlayerMoveInfo(moveInfo);
        // Fall-back check (skip with jump amplifier).
        final double maxD = data.jumpAmplifier > 0.0 ? NoFall.getDamage((float) NoFall.getApplicableFallHeight(player, loc.getY(), data))
                                                     : NoFall.getDamage(Math.max(yDiff, Math.max(data.noFallFallDistance, fallDistance))) + (allowReset ? 0.0 : Magic.FALL_DAMAGE_DIST);
        if (maxD > damage) {
            // TODO: respect dealDamage ?
            double damageafter = NoFall.calcDamagewithfeatherfalling(player, NoFall.calcReducedDamageByBlock(player, data, maxD), mcAccess.getHandle().dealFallDamageFiresAnEvent().decide());
            BridgeHealth.setRawDamage(event, damageafter);
            if (debug) debug(player, "Adjust fall damage to: " + (damageafter != maxD ? damageafter : maxD));
        }

        if (allowReset) {
            // Normal fall damage, reset data.
            data.clearNoFallData();
            if (debug) debug(player, "Reset NoFall data on fall damage.");
        }
        else {
            // Minecraft/NCP bug or cheating.
            // (Do not cancel the event, otherwise: "moved too quickly exploit".)
            if (cc.noFallViolationReset) {
                data.clearNoFallData();
            }

            // Add player to hover checks.
            if (cc.sfHoverCheck && data.sfHoverTicks < 0) {
                data.sfHoverTicks = 0;
                hoverTicks.add(player.getName());
            }
        }
        // Entity fall-distance should be reset elsewhere.
        // Cleanup.
        useFallLoc.setWorld(null);
    }


    private final boolean noFallVL(final Player player, final String tag, final MovingData data, final MovingConfig cc) {

        data.noFallVL += 1.0;
        final ViolationData vd = new ViolationData(noFall, player, data.noFallVL, 1.0, cc.noFallActions);
        if (tag != null) vd.setParameter(ParameterName.TAGS, tag);
        return noFall.executeActions(vd).willCancel();
    }


    /**
     * When a player respawns, all information related to the moving checks
     * becomes invalid.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {

        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        // TODO: Prevent/cancel scheduled teleport (use PlayerData/task for teleport, or a sequence count).
        data.clearMostMovingCheckData();
        data.resetSetBack(); // To force dataOnJoin to set it to loc.
        // Handle respawn like join.
        dataOnJoin(player, event.getRespawnLocation(), true, data, pData.getGenericInstance(MovingConfig.class), pData.isDebugActive(checkType));
        // Patch up issues.
        if (Bridge1_9.hasGetItemInOffHand() && player.isBlocking()) {
            // Attempt to fix server-side-only blocking after respawn.
            redoShield(player);
        }
    }


    /**
     * Attempt to fix server-side-only blocking after respawn.
     * @param player
     */
    private void redoShield(final Player player) {

        // Does not work: DataManager.getPlayerData(player).requestUpdateInventory();
        if (mcAccess.getHandle().resetActiveItem(player)) return;
        final PlayerInventory inv = player.getInventory();
        ItemStack stack = inv.getItemInOffHand();
        if (stack != null && stack.getType() == Material.SHIELD) {
            // Shield in off-hand.
            inv.setItemInOffHand(stack);
            return;
        }
        stack = inv.getItemInMainHand();
        if (stack != null && stack.getType() == Material.SHIELD) {
            // Shield in off-hand.
            inv.setItemInMainHand(stack);
            return;
        }
    }


    @Override
    public void playerJoins(final Player player) {

        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        dataOnJoin(player, player.getLocation(useJoinLoc), false, pData.getGenericInstance(MovingData.class), 
                  pData.getGenericInstance(MovingConfig.class), pData.isDebugActive(checkType));
        // Cleanup.
        useJoinLoc.setWorld(null);
    }


    /**
     * Alter data for players joining (join, respawn).<br>
     * Do before, if necessary:<br>
     * <li>data.clearFlyData()</li>
     * <li>data.setSetBack(...)</li>
     * @param player
     * @param loc Can be useLoc (!).
     * @param isRespawn
     * @param data
     * @param cc
     * @param debug
     */
    private void dataOnJoin(Player player, Location loc, boolean isRespawn, MovingData data, 
                            MovingConfig cc, final boolean debug) {

        final int tick = TickTask.getTick();
        final String tag = isRespawn ? "Respawn" : "Join";
        // Check loaded chunks.
        if (cc.loadChunksOnJoin) {
            // (Don't use past-move heuristic for skipping here.)
            final int loaded = MapUtil.ensureChunksLoaded(loc.getWorld(), loc.getX(), loc.getZ(), Magic.CHUNK_LOAD_MARGIN_MIN);
            if (loaded > 0 && debug) {
                StaticLog.logInfo("Player " + tag + ": Loaded " + loaded + " chunk" + (loaded == 1 ? "" : "s") + " for the world " + loc.getWorld().getName() +  " for player: " + player.getName());
            }
        }

        // Correct set back on join.
        if (!data.hasSetBack() || !data.hasSetBackWorldChanged(loc)) {
            data.clearFlyData();
            data.setSetBack(loc);
            // (resetPositions is called below)
            data.joinOrRespawn = true; // TODO: Review if to always set (!).
        }
        else {
            // TODO: Check consistency/distance.
            //final Location setBack = data.getSetBack(loc);
            //final double d = loc.distanceSquared(setBack);
            // TODO: If to reset positions: relate to previous ones and set back.
            // (resetPositions is called below)
        }
        // (Note: resetPositions resets lastFlyCheck and other.)
        data.clearVehicleData(); // TODO: Uncertain here, what to check.
        data.clearAllMorePacketsData();
        data.removeAllVelocity();
        data.resetTrace(player, loc, tick, mcAccess.getHandle(), cc); // Might reset to loc instead of set back ?
        // More resetting.
        data.vDistAcc.clear();
        aux.resetPositionsAndMediumProperties(player, loc, data, cc);
        data.sfHorizontalBuffer = cc.hBufMax;

        // Enforcing the location.
        if (cc.enforceLocation) {
            playersEnforce.add(player.getName());
        }

        // Hover.
        initHover(player, data, cc, data.playerMoves.getFirstPastMove().from.onGroundOrResetCond);

        // Check for vehicles.
        // TODO: Order / exclusion of items.
        if (player.isInsideVehicle()) {
            vehicleChecks.onPlayerVehicleEnter(player, player.getVehicle());
        }

        if (debug) {
            debug(player, tag + ": " + loc);
        }
    }


    /**
     * Initialize the hover check for a player (login, respawn). 
     * @param player
     * @param data
     * @param cc
     * @param isOnGroundOrResetCond 
     */
    private void initHover(final Player player, final MovingData data, final MovingConfig cc, final boolean isOnGroundOrResetCond) {
        
        // Reset hover ticks until a better method is used.
        if (!isOnGroundOrResetCond && cc.sfHoverCheck) {
            // Start as if hovering already.
            // Could check shouldCheckSurvivalFly(player, data, cc), but this should be more sharp (gets checked on violation).
            data.sfHoverTicks = 0;
            data.sfHoverLoginTicks = cc.sfHoverLoginTicks;
            hoverTicks.add(player.getName());
        }
        else {
            data.sfHoverLoginTicks = 0;
            data.sfHoverTicks = -1;
        }
    }


    @Override
    public void playerLeaves(final Player player) {

        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final Location loc = player.getLocation(useLeaveLoc);
        // Debug logout.
        if (pData.isDebugActive(checkType)) StaticLog.logInfo("Player " + player.getName() + " leaves at location: " + loc.toString());

        if (!player.isSleeping() && !player.isDead()) {
            // Check for missed moves.
            // TODO: Force-load chunks [log if (!)] ?
            // Check only from the old versions, newer versions don't seem like a problem to account for
            if (!BlockProperties.isPassable(loc) && !Bridge1_13.hasIsSwimming()) {

                final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
                final PlayerMoveData lastMove2 = data.playerMoves.getNumberOfPastMoves() > 1 ? data.playerMoves.getSecondPastMove() : null;
                // Won't use lastMove.toIsValid to prevent players already failed some checks in last move
                if (lastMove.valid) {
                    Location refLoc = lastMove.toIsValid ? new Location(loc.getWorld(), lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ()) 
                                                           : new Location(loc.getWorld(), lastMove.from.getX(), lastMove.from.getY(), lastMove.from.getZ());

                    // More likely lastmove location is same with left location, try to check for second lastmove  
                    if (TrigUtil.isSamePos(loc, refLoc) && !lastMove.toIsValid && lastMove2 != null) {
                        refLoc = lastMove2.toIsValid ? new Location(loc.getWorld(), lastMove2.to.getX(), lastMove2.to.getY(), lastMove2.to.getZ()) 
                                                       : new Location(loc.getWorld(), lastMove2.from.getX(), lastMove2.from.getY(), lastMove2.from.getZ());
                    }
                    // Correct position by scan block up
                    // TODO: what about try to phase upward not downward anymore?
                    if (!BlockProperties.isPassable(refLoc) || refLoc.distanceSquared(loc) > 1.25) {
                        double y = Math.ceil(loc.getY());
                        refLoc = loc.clone();
                        refLoc.setY(y);
                        if (!BlockProperties.isPassable(refLoc)) refLoc = loc;
                    }
                    final double d = refLoc.distanceSquared(loc);
                    if (d > 0.0) {
                        // TODO: Consider to always set back here. Might skip on big distances.
                        if (TrigUtil.manhattan(loc, refLoc) > 0 || BlockProperties.isPassable(refLoc)) {

                            if (passable.isEnabled(player, pData)) {
                                StaticLog.logWarning("Potential exploit: Player " + player.getName() + " leaves, having moved into a block (not tracked by moving checks): " + player.getWorld().getName() + " / " + DebugUtil.formatMove(refLoc, loc));
                                // TODO: Actually trigger a passable violation (+tag).
                                if (d > 1.25) {
                                    StaticLog.logWarning("SKIP set back for " + player.getName() + ", because distance is too high (risk of false positives): " + d);
                                }
                                else {
                                    StaticLog.logInfo("Set back player " + player.getName() + ": " + LocUtil.simpleFormat(refLoc));
                                    data.prepareSetBack(refLoc);
                                    if (!Folia.teleportEntity(player, refLoc, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION)) {
                                        StaticLog.logWarning("FAILED to set back player " + player.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        useLeaveLoc.setWorld(null);
        // Adjust data.
        survivalFly.setReallySneaking(player, false);
        noFall.onLeave(player, data, pData);
        // TODO: Add a method for ordinary presence-change resetting (use in join + leave).
        data.onPlayerLeave();
        if (Folia.isTaskScheduled(data.vehicleSetBackTaskId)) {
            // Reset the id, assume the task will still teleport the vehicle.
            // TODO: Should rather force teleport (needs storing the task + data).
            data.vehicleSetBackTaskId = null;
        }
        data.vehicleSetPassengerTaskId = null;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        survivalFly.setReallySneaking(event.getPlayer(), event.isSneaking());
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSprint(final PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) DataManager.getGenericInstance(event.getPlayer(), MovingData.class).timeSprinting = 0;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerToggleFlight(final PlayerToggleFlightEvent event) {

        // (ignoreCancelled = false: we track the bit of vertical extra momentum/thing).
        final Player player = event.getPlayer();
        if (player.isFlying() || event.isFlying() && !event.isCancelled())  return;
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.MOVING, player)) return;
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final Location loc = player.getLocation(useToggleFlightLoc);
        moveInfo.set(player, loc, null, cc.yOnGround);
        // TODO: data.isVelocityJumpPhase() might be too harsh, but prevents too easy abuse.
        if (!MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, moveInfo.to, data, cc, pData) 
            || data.isVelocityJumpPhase() || BlockProperties.isOnGroundOrResetCond(player, loc, cc.yOnGround)) {
            useToggleFlightLoc.setWorld(null);
            aux.returnPlayerMoveInfo(moveInfo);
            return;
        }
        aux.returnPlayerMoveInfo(moveInfo);
        useToggleFlightLoc.setWorld(null);
        // TODO: Confine to minimum activation ticks.
        data.addVelocity(player, cc, 0.0, 0.3, 0.0);
    }


    @Override
    public void onTick(final int tick, final long timeLast) {

        // TODO: Change to per world checking (as long as configs are per world).
        // Legacy: enforcing location consistency.
        if (!playersEnforce.isEmpty()) checkOnTickPlayersEnforce();
        // Hover check (SurvivalFly).
        if (tick % hoverTicksStep == 0 && !hoverTicks.isEmpty()) {
            // Only check every so and so ticks.
            checkOnTickHover();
        }
        // Cleanup.
        useTickLoc.setWorld(null);
    }


    /**
     * Check for hovering.<br>
     * NOTE: Makes use of useLoc, without resetting it.
     */
    private void checkOnTickHover() {

        final List<String> rem = new ArrayList<String>(hoverTicks.size()); // Pessimistic.
        final PlayerMoveInfo info = aux.usePlayerMoveInfo();
        for (final String playerName : hoverTicks) {
            // TODO: put players into the set (+- one tick would not matter ?)
            // TODO: might add an online flag to data !
            final Player player = DataManager.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                rem.add(playerName);
                continue;
            }
            final IPlayerData pData = DataManager.getPlayerData(player);
            final MovingData data = pData.getGenericInstance(MovingData.class);
            if (player.isDead() || player.isSleeping() || player.isInsideVehicle()) {
                data.sfHoverTicks = -1;
                // (Removed below.)
            }
            if (data.sfHoverTicks < 0) {
                data.sfHoverLoginTicks = 0;
                rem.add(playerName);
                continue;
            }
            else if (data.sfHoverLoginTicks > 0) {
                // Additional "grace period".
                data.sfHoverLoginTicks --;
                continue;
            }
            final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
            // Check if enabled at all.
            if (!cc.sfHoverCheck) {
                rem.add(playerName);
                data.sfHoverTicks = -1;
                continue;
            }
            // Increase ticks here.
            data.sfHoverTicks += hoverTicksStep;
            if (data.sfHoverTicks < cc.sfHoverTicks) {
                // Don't do the heavier checking here, let moving checks reset these.
                continue;
            }
            if (checkHover(player, data, cc, pData, info)) {
                rem.add(playerName);
            }
        }
        hoverTicks.removeAll(rem);
        aux.returnPlayerMoveInfo(info);
    }


    /**
     * Legacy check: Enforce location of players, in case of inconsistencies.
     * First move exploit / possibly vehicle leave.<br>
     * NOTE: Makes use of useLoc, without resetting it.
     */
    private void checkOnTickPlayersEnforce() {

        final List<String> rem = new ArrayList<String>(playersEnforce.size()); // Pessimistic.
        for (final String playerName : playersEnforce) {
            final Player player = DataManager.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                rem.add(playerName);
                continue;
            } 
            else if (player.isDead() || player.isSleeping() || player.isInsideVehicle()) {
                // Don't remove but also don't check [subject to change].
                continue;
            }
            final MovingData data = DataManager.getGenericInstance(player, MovingData.class);
            final Location newTo = enforceLocation(player, player.getLocation(useTickLoc), data);
            if (newTo != null) {
                data.prepareSetBack(newTo);
                Folia.teleportEntity(player, newTo, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
            }
        }
        if (!rem.isEmpty()) playersEnforce.removeAll(rem);
    }


    private Location enforceLocation(final Player player, final Location loc, final MovingData data) {

        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (lastMove.toIsValid && TrigUtil.distanceSquared(lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ(), loc.getX(), loc.getY(), loc.getZ()) > 1.0 / 256.0) {
            // Teleport back. 
            if (data.hasSetBack()) {
                // Might have to re-check all context with playerJoins and keeping old set backs...
                // Could use a flexible set back policy (switch to in-air on login). 
                return data.getSetBack(loc); // (OK? ~ legacy)
            }
            else {
                return new Location(player.getWorld(), lastMove.to.getX(), lastMove.to.getY(), lastMove.to.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
        else return null;
    }


    /**
     * The heavier checking including on.ground etc., check if enabled/valid to check before this. 
     * @param player
     * @param data
     * @param cc
     * @param info
     * @return
     */
    private boolean checkHover(final Player player, final MovingData data, final MovingConfig cc, final IPlayerData pData,
                               final PlayerMoveInfo info) {

        // Check if player is on ground.
        final Location loc = player.getLocation(useTickLoc); // useLoc.setWorld(null) is done in onTick.
        info.set(player, loc, null, cc.yOnGround);
        // (Could use useLoc of MoveInfo here. Note orderm though.)
        final boolean res;
        // TODO: Collect flags, more margin ?
        final int loaded = info.from.ensureChunksLoaded();
        if (loaded > 0 && pData.isDebugActive(checkType)) {
            // DEBUG
            StaticLog.logInfo("Hover check: Needed to load " + loaded + " chunk" + (loaded == 1 ? "" : "s") + " for the world " + loc.getWorld().getName() +  " around " + loc.getBlockX() + "," + loc.getBlockZ() + " in order to check player: " + player.getName());
        }
        if (info.from.isOnGroundOrResetCond() || info.from.isAboveLadder() || info.from.isAboveStairs()) {
            res = true;
            data.sfHoverTicks = 0;
        }
        else {
            if (data.sfHoverTicks > cc.sfHoverTicks) {
                // Re-Check if survivalfly can apply at all.
                final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
                moveInfo.set(player, loc, null, cc.yOnGround);
                if (MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, moveInfo.to, data, cc, pData)) {
                    handleHoverViolation(player, moveInfo.from, cc, data, pData);
                    // Assume the player might still be hovering.
                    res = false;
                    data.sfHoverTicks = 0;
                }
                else {
                    // Reset hover ticks and check next period.
                    res = false;
                    data.sfHoverTicks = 0;
                }
                aux.returnPlayerMoveInfo(moveInfo);
            }
            else res = false;
        }
        info.cleanup();
        return res;
    }


    private void handleHoverViolation(final Player player, final PlayerLocation loc, 
                                      final MovingConfig cc, final MovingData data, final IPlayerData pData) {

        // Check nofall damage (!).
        if (cc.sfHoverFallDamage && noFall.isEnabled(player, pData)) {
            // Consider adding 3/3.5 to fall distance if fall distance > 0?
            noFall.checkDamage(player, loc.getY(), data, pData);
        }
        // Delegate violation handling.
        survivalFly.handleHoverViolation(player, loc, cc, data);
    }


    @Override
    public CheckType getCheckType() {
        // TODO: this is for the hover check only...
        // TODO: ugly.
        return CheckType.MOVING_SURVIVALFLY;
    }


    @Override
    public IData removeData(String playerName) {
        // Let TickListener handle automatically
        //hoverTicks.remove(playerName);
        //playersEnforce.remove(playerName);
        return null;
    }


    @Override
    public void removeAllData() {
        hoverTicks.clear();
        playersEnforce.clear();
        aux.clear();
    }


    @Override
    public void onReload() {
        aux.clear();
        hoverTicksStep = Math.max(1, ConfigManager.getConfigFile().getInt(ConfPaths.MOVING_SURVIVALFLY_HOVER_STEP));
    }


    /**
     * Output information specific to player-move events.
     * @param player
     * @param from
     * @param to
     * @param mcAccess
     */
    private void outputMoveDebug(final Player player, final PlayerLocation from, final PlayerLocation to, 
            final double maxYOnGround, final MCAccess mcAccess) {
        final StringBuilder builder = new StringBuilder(250);
        final Location loc = player.getLocation();
        builder.append(CheckUtils.getLogMessagePrefix(player, checkType));
        builder.append("MOVE in world " + from.getWorld().getName() + ":\n");
        DebugUtil.addMove(from, to, loc, builder);
        final double jump = mcAccess.getJumpAmplifier(player);
        final double speed = mcAccess.getFasterMovementAmplifier(player);
        final double strider = BridgeEnchant.getDepthStriderLevel(player);
        if (BuildParameters.debugLevel > 0) {
            try{
                // TODO: Check backwards compatibility (1.4.2). Remove try-catch
                builder.append("\n(walkspeed=" + player.getWalkSpeed() + " flyspeed=" + player.getFlySpeed() + ")");
            } catch (Throwable t) {}
            if (player.isSprinting()) {
                builder.append("(sprinting)");
            }
            if (player.isSneaking()) {
                builder.append("(sneaking)");
            }
            if (player.isBlocking()) {
                builder.append("(blocking)");
            }
            final Vector v = player.getVelocity();
            if (v.lengthSquared() > 0.0) {
                builder.append("(svel=" + v.getX() + "," + v.getY() + "," + v.getZ() + ")");
            }
        }
        if (!Double.isInfinite(speed)) {
            builder.append("(e_speed=" + (speed + 1) + ")");
        }
        final double slow = PotionUtil.getPotionEffectAmplifier(player, BridgePotionEffect.SLOWNESS);
        if (!Double.isInfinite(slow)) {
            builder.append("(e_slow=" + (slow + 1) + ")");
        }
        if (!Double.isInfinite(jump)) {
            builder.append("(e_jump=" + (jump + 1) + ")");
        }
        if (strider != 0) {
            builder.append("(e_depth_strider=" + strider + ")");
        }
        if (Bridge1_9.isGliding(player)) {
            builder.append("(gliding)");
        }
        if (player.getAllowFlight()) {
            builder.append("(allow_flight)");
        }
        if (player.isFlying()) {
            builder.append("(flying)");
        }
        // Print basic info first in order
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
        // Extended info.
        if (BuildParameters.debugLevel > 0) {
            builder.setLength(0);
            // Note: the block flags are for normal on-ground checking, not with yOnGrond set to 0.5.
            from.collectBlockFlags(maxYOnGround);
            if (from.getBlockFlags() != 0) {
                builder.append("\nFrom flags: " + StringUtil.join(BlockFlags.getFlagNames(from.getBlockFlags()), "+"));
            }
            if (!BlockProperties.isAir(from.getTypeId())) {
                DebugUtil.addBlockInfo(builder, from, "\nFrom");
            }
            if (!BlockProperties.isAir(from.getTypeIdBelow())) {
                DebugUtil.addBlockBelowInfo(builder, from, "\nFrom");
            }
            if (!from.isOnGround() && from.isOnGround(0.5)) {
                builder.append(" (ground within 0.5)");
            }
            to.collectBlockFlags(maxYOnGround);
            if (to.getBlockFlags() != 0) {
                builder.append("\nTo flags: " + StringUtil.join(BlockFlags.getFlagNames(to.getBlockFlags()), "+"));
            }
            if (!BlockProperties.isAir(to.getTypeId())) {
                DebugUtil.addBlockInfo(builder, to, "\nTo");
            }
            if (!BlockProperties.isAir(to.getTypeIdBelow())) {
                DebugUtil.addBlockBelowInfo(builder, to, "\nTo");
            }
            if (!to.isOnGround() && to.isOnGround(0.5)) {
                builder.append(" (ground within 0.5)");
            }
            NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
        }
    }
}