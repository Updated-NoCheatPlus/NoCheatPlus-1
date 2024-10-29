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
package fr.neatmonster.nocheatplus.utilities.entity;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.VehicleMoveData;
import fr.neatmonster.nocheatplus.checks.moving.vehicle.VehicleSetPassengerTask;
import fr.neatmonster.nocheatplus.checks.workaround.WRPT;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.components.entity.IEntityAccessVehicle;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.AuxMoving;

/**
 * Vehicle/passenger related static utility. Registered as generic instance for
 * now.
 * 
 * @author asofold
 *
 */
public class PassengerUtil {

    public final IHandle<IEntityAccessVehicle> handleVehicle = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IEntityAccessVehicle.class);

    /** Temp use. LocUtil.clone on passing. setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);
    /** Temp use. LocUtil.clone on passing. setWorld(null) after use. */
    private final Location useLoc2 = new Location(null, 0, 0, 0);
    
    private final Plugin plugin = Bukkit.getPluginManager().getPlugin("NoCheatPlus");

    /**
     * Test if the given entity is a passenger of the given vehicle.
     * 
     * @param entity
     * @param vehicle
     * @return
     */
    public boolean isPassenger(final Entity entity, final Entity vehicle) {
        return handleVehicle.getHandle().getEntityPassengers(vehicle).contains(entity);
    }

    /**
     * Check getPassenger recursively until a player is found, return that one
     * or null. This is intended to be the player in charge of steering the
     * vehicle.
     *
     * @param entity
     *            The vehicle.
     * @return The first player found amongst passengers recursively, excludes
     *         the given entity.
     */
    public Player getFirstPlayerPassenger(Entity entity) {
        List<Entity> passengers = handleVehicle.getHandle().getEntityPassengers(entity);
        while (!passengers.isEmpty()){
            entity = passengers.get(0); // The one in charge.
            if (entity == null) {
                break;
            }
            else if (entity instanceof Player){
                return (Player) entity;
            }
            passengers = handleVehicle.getHandle().getEntityPassengers(entity);
        }
        return null;
    }

    /**
     * Check recursively for vehicles, returns null if players are vehicles,
     * otherwise the lowest vehicle (that has no vehicle).
     *
     * @param passenger
     *            The passenger of vehicles. Typically the player.
     * @return Supposedly the vehicle that is steered.
     */
    public Entity getLastNonPlayerVehicle(final Entity passenger) {
        return getLastNonPlayerVehicle(passenger, false);
    }

    /**
     * Check recursively for vehicles, returns null if players are vehicles,
     * otherwise the lowest vehicle (that has no vehicle).
     *
     * @param passenger
     *            The passenger of vehicles. Typically the player.
     * @param includePassenger
     *            If set to true, the passenger is counted as a vehicle as well
     *            (meaning: vehicle enter, the player is not in a vehicle, test
     *            with this set to true and the vehicle returned by the event).
     * @return Supposedly the vehicle that is steered.
     */
    public Entity getLastNonPlayerVehicle(final Entity passenger, final boolean includePassenger) {
        Entity vehicle = includePassenger ? passenger : passenger.getVehicle();
        while (vehicle != null){
            if (vehicle instanceof Player){
                return null;
            }
            else if (vehicle.isInsideVehicle()) {
                vehicle = vehicle.getVehicle();
            }
            else {
                break;
            }
        }
        return vehicle;
    }

    //    /**
    //     * Get a player from an entity. This will return the first player found
    //     * amongst the entity itself and passengers checked recursively.
    //     *
    //     * @param entity
    //     *            the entity
    //     * @return the player passenger recursively
    //     */
    //    public Player getFirstPlayerIncludingPassengersRecursively(Entity entity) {
    //        while (entity != null) {
    //            if (entity instanceof Player) {
    //                // Scrap the case of players riding players for the moment.
    //                return (Player) entity;
    //            }
    //            final Entity passenger = entity.getPassenger();
    //            if (entity.equals(passenger)) {
    //                // Just in case :9.
    //                break;
    //            }
    //            else {
    //                entity = passenger;
    //            }
    //        }
    //        return null;
    //    }

    /**
     * Teleport the player with vehicle, might temporarily eject the passengers
     * and set teleported in MovingData. The passengers are fetched from the
     * vehicle with this method.
     *
     * @param vehicle
     *            The vehicle to teleport.
     * @param player
     *            The (original) player in charge, who'd also trigger
     *            violations. Should be originalPassengers[0].
     * @param location
     *            Location to teleport the vehicle to.
     * @param debug
     *            the debug
     */
    public void teleportWithPassengers(final Entity vehicle, final Player player, final Location location, final boolean debug, final IPlayerData pData) {
        final List<Entity> originalPassengers = handleVehicle.getHandle().getEntityPassengers(vehicle);
        teleportWithPassengers(vehicle, player, location, debug, originalPassengers.toArray(new Entity[originalPassengers.size()]), false, pData);
    }

    /**
     * Teleport the player with vehicle, might temporarily eject the passengers and set
     * teleported in MovingData.
     *
     * @param vehicle
     *            The vehicle to teleport.
     * @param player
     *            The (original) player in charge, who'd also trigger
     *            violations. Should be originalPassengers[0].
     * @param location
     *            Location to teleport the vehicle to.
     * @param debug
     *            the debug
     * @param originalPassengers
     *            The passengers at the time, that is to be restored. Must not be null.
     * @param CheckPassengers Set to true to compare current with original passengers.
     */
    public void teleportWithPassengers(final Entity vehicle, final Player player, final Location location, 
                                       final boolean debug, final Entity[] originalPassengers, final boolean checkPassengers,
                                       final IPlayerData pData) {
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final String pWorld = player.getWorld().getName();
        final String vWorld = vehicle.getWorld() != null ? vehicle.getWorld().getName() : "";
        final boolean vWorldMatchesPWorld = vWorld.equals(pWorld);
        // TODO: Rubber band issue needs synchronizing with packet level and ignore certain incoming ones?
        // TODO: This handling could conflict with WorldGuard region flags.
        // TODO: Account for nested passengers and inconsistencies.
        // TODO: Conception: Restore the passengers at the time of setting the vehicle set back?
        data.isVehicleSetBack = true;
        int otherPlayers = 0;
        boolean playerIsOriginalPassenger = false;
        for (int i = 0; i < originalPassengers.length; i++) { 
            if (originalPassengers[i].equals(player)) {
                playerIsOriginalPassenger = true;
                break;
            }
            else if (originalPassengers[i] instanceof Player) {
                DataManager.getGenericInstance((Player) originalPassengers[i], MovingData.class).isVehicleSetBack = true;
                otherPlayers ++;
            }
        }
        boolean redoPassengers = true; // false; // Some time in the future a teleport might work directly.
        //        if (checkPassengers) {
        //            final List<Entity> passengers = handleVehicle.getHandle().getEntityPassengers(vehicle);
        //            if (passengers.size() != originalPassengers.length) {
        //                redoPassengers = true;
        //            }
        //            else {
        //                for (int i = 0; i < originalPassengers.length; i++) {
        //                    if (originalPassengers[i] != passengers.get(i)) {
        //                        redoPassengers = true;
        //                        break;
        //                    }
        //                }
        //            }
        //        }
        if (!playerIsOriginalPassenger) {
            if (debug) {
                CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Vehicle set back: This player is not an original passenger.");
            }
            //            redoPassengers = true;
        }

        boolean vehicleTeleported = false;
        boolean playerTeleported = false;
        int otherPlayersTeleported = 0;
        if (vehicle.isDead() || !vehicle.isValid()) {
            // TODO: Still consider teleporting the player.
            vehicleTeleported = false;
        }
        else {
            // TODO: if (!redoPassengers) { 
            // Can the vehicle teleport with passengers directly, one day?
            // Attempt to only teleport the entity first. On failure use eject.
            // TODO: Probably needs a guard depending on version.
            //            if (vehicle.teleport(location, 
            //                  BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION)) {
            //                // Check success.
            //                if (vehicle.getLocation(useLoc).equals(location) && isPassenger(player, vehicle)) { // TODO: Compare all passengers (...)
            //                    vehicleTeleported = true;
            //                    playerTeleported = true;
            //                    if (debug) {
            //                        CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Direct teleport of entity with passenger succeeded.");
            //                    }
            //                }
            //            } // TODO: other players flags reset etc.
            if (redoPassengers){
                // Teleport the vehicle independently.
                vehicle.eject(); // NOTE: VehicleExit fires, unknown TP fires.
                // TODO: Confirm eject worked, handle if not.
                //vehicleTeleported = vehicle.teleport(LocUtil.clone(location), BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
                vehicleTeleported = SchedulerHelper.teleportEntity(vehicle, LocUtil.clone(location), BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
            }
        }

        if (redoPassengers) {
            // Add the player first,  if not an original passenger (special case, idk, replaced by squids perhaps).
            if (!playerIsOriginalPassenger) {
                // (Not sure: always add first, until another case is needed.)
                teleportPlayerPassenger(player, vehicle, location, vehicleTeleported, data, debug);
            }
            // Add all other original passengers in a generic way, distinguish players.
            for (int i = 0; i < originalPassengers.length; i++) {
                final Entity passenger = originalPassengers[i];
                if (passenger.isValid() && !passenger.isDead() && vWorldMatchesPWorld) {

                    // Cross world cases? -> Seems like it :)
                    if (passenger instanceof Player) {
                        if (teleportPlayerPassenger((Player) passenger, vehicle, location, vehicleTeleported, DataManager.getGenericInstance((Player) passenger, MovingData.class), debug)) {
                            if (player.equals(passenger)) {
                                playerTeleported = true;
                            }
                            else {
                                otherPlayersTeleported ++;
                            }
                        }
                    }
                    else {
                        if (SchedulerHelper.teleportEntity(passenger, location, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION)
                            && vehicleTeleported && TrigUtil.distance(passenger.getLocation(useLoc2), vehicle.getLocation(useLoc)) < 1.5) {
                            if (!handleVehicle.getHandle().addPassenger(passenger, vehicle)) {
                                // TODO: What?
                            }
                        }
                    }
                }
                else if (debug) { 
                   CheckUtils.debug(player, CheckType.MOVING_VEHICLE, (!vWorldMatchesPWorld) ? "**** Prevent adding passengers to root vehicle on world change (potential exploit)" 
                                    : "Can't add passenger to vehicle: passenger is dead.");
                // Log skipped + failed non player entities.
                }
            }
        }
        // TODO: else: reset flags for other players?

        // Log resolution.
        if (debug) { 
            CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Vehicle set back resolution: " + location + " pt=" + playerTeleported + " vt=" + vehicleTeleported + (otherPlayers > 0 ? (" opt=" + otherPlayersTeleported + "/" + otherPlayers) : ""));
        }
        useLoc.setWorld(null);
        useLoc2.setWorld(null);
    }

    /**
     * Teleport and set as passenger.
     * 
     * @param player
     * @param vehicle
     * @param location
     * @param vehicleTeleported
     * @param data
     * @return
     */
    private final boolean teleportPlayerPassenger(final Player player, final Entity vehicle, final Location location, final boolean vehicleTeleported, final MovingData data, final boolean debug) {
        final boolean playerTeleported;
        if (player.isOnline() && !player.isDead()) {
            final MovingConfig cc = DataManager.getGenericInstance(player, MovingConfig.class);
            // Mask player teleport as a set back.
            data.prepareSetBack(location);
            playerTeleported = SchedulerHelper.teleportEntity(player, LocUtil.clone(location), BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
            data.resetTeleported(); // Cleanup, just in case.
            // Workarounds.
            // Allow re-use of certain workarounds. Hack/shouldbedoneelsewhere?
            data.ws.resetConditions(WRPT.G_RESET_NOTINAIR);

            if (playerTeleported && vehicleTeleported && player.getLocation(useLoc2).distance(vehicle.getLocation(useLoc)) < 1.5) {

                // Still set as passenger.
                // NOTE: VehicleEnter fires, unknown TP fires.
                boolean scheduledelay = cc.schedulevehicleSetPassenger;
                if (data.vehicleSetPassengerTaskId == null) {
                    // TODO: Check which version fixed this?
                    if (MaterialUtil.isBoat(vehicle.getType())) {
                        if (!handleVehicle.getHandle().addPassenger(player, vehicle)) {
                            // Not schedule set passenger for boat due to location async
                            // Re-add to vehicle then reject. Fix boat teleported but player didn't
                        } 
                        else vehicle.eject();
                    } 
                    else if (scheduledelay) {

                        data.vehicleSetPassengerTaskId = SchedulerHelper.runSyncDelayedTaskForEntity(player, plugin, (arg) -> new VehicleSetPassengerTask(handleVehicle, vehicle, player).run(), null, 2L);
                        if (data.vehicleSetPassengerTaskId == null) {

                            if (debug) CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Failed to schedule set passenger!");
                            scheduledelay = false;
                        } 
                        else if (debug) CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Schedule set passenger task id: " + data.vehicleSetPassengerTaskId);
                    }

                    if (!scheduledelay) {
                        if (debug) {
                            CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Attempt set passenger directly");
                        }

                        if (!handleVehicle.getHandle().addPassenger(player, vehicle)) {
                            // TODO: What?
                        }
                    }
                } 
                else if (debug) CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "Set passenger task already scheduled, skip this time.");
                // Ensure a set back.
                // TODO: Set backs get invalidated somewhere, likely on an extra unknown TP. Use data.isVehicleSetBack in MovingListener/teleport.
                if (data.vehicleSetBacks.getFirstValidEntry(location) == null) {
                    // At least ensure one of the entries has to match the location we teleported the vehicle to.
                    if (debug) {
                        CheckUtils.debug(player, CheckType.MOVING_VEHICLE, "No set back is matching the vehicle location that it has just been set back to. Reset all lazily to: " + location);
                    }
                    data.vehicleSetBacks.resetAllLazily(location);
                }
                // Set this location as past move.
                final VehicleMoveData firstPastMove = data.vehicleMoves.getFirstPastMove();
                if (!firstPastMove.valid || firstPastMove.toIsValid || !TrigUtil.isSamePos(firstPastMove.from, location)) {
                    NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class).resetVehiclePositions(vehicle, location, data, cc);
                }
            }
        }
        else {
            playerTeleported = false;
        }
        data.isVehicleSetBack = false;
        return playerTeleported;
    }
}
