package fr.neatmonster.nocheatplus.checks.moving.envelope.workaround;

import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.VehicleMoveData;
import fr.neatmonster.nocheatplus.checks.moving.vehicle.VehicleEnvelope.CheckDetails;
import fr.neatmonster.nocheatplus.checks.workaround.WRPT;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MagicVehicle;

/**
 * Workarounds for ridables entities / vehicles 
 * (Name should be VehicleEnvelope, just to avoid confusion with the actual check)
 */
public class VehicleWorkarounds {

	/**
	 * Ascending in-water (water-water).
	 * @param thisMove
	 * @return
	 */
	public static boolean oddInWaterAscend(final VehicleMoveData thisMove, final CheckDetails checkDetails, final MovingData data) {
	    // (Try individual if this time, let JIT do the rest.)
	    // Boat.
	    if (MaterialUtil.isBoat(checkDetails.simplifiedType)) {
	        if (thisMove.yDistance > MagicVehicle.maxAscend 
	            && thisMove.yDistance < MagicVehicle.boatMaxBackToSurfaceAscend
	            && data.ws.use(WRPT.W_M_V_ENV_INWATER_BTS)) {
	            // (Assume players can't control sinking boats for now.)
	            // TODO: Limit by more side conditions (e.g. to is on the surface and in-medium count is about 1, past moves).
	            // TODO: Checking for surface can be complicated. Might check blocks at location and above and accept if any is not liquid.
	            // (Always smaller than previous descending move, roughly to below 0.5 above water.)
	            return true;
	        }
	    }
	    return false;
	}

	/**
	 * In-water (water-water) cases.
	 * @param thisMove
	 * @return
	 */
	public static boolean oddInWater(final VehicleMoveData thisMove, final CheckDetails checkDetails, final MovingData data) {
	    if (thisMove.yDistance > 0.0) {
	        if (oddInWaterAscend(thisMove, checkDetails, data)) {
	            return true;
	        }
	    }
	    return false;
	}

	/**
	 * 
	 * @param thisMove
	 * @param minDescend Case-sensitive.
	 * @param maxDescend Case-sensitive.
	 * @param data
	 * @return
	 */
	public static boolean oddInAirDescend(final VehicleMoveData thisMove, final double minDescend, final double maxDescend, final CheckDetails checkDetails, final MovingData data) {
	    // TODO: Guard by past move tracking, instead of minDescend and maxDescend.
	    // (Try individual if this time, let JIT do the rest.)
	    // Boat.
	    if (MaterialUtil.isBoat(checkDetails.simplifiedType)) {
	        // Boat descending in-air, skip one vehicle move event during late in-air phase.
	        if (data.sfJumpPhase > 54 && thisMove.yDistance < 2.0 * minDescend && thisMove.yDistance > 2.0 * maxDescend
	                // TODO: Past move tracking.
	                // TODO: Fall distances?
	                && data.ws.use(WRPT.W_M_V_ENV_INAIR_SKIP)
	                ) {
	            // (In-air count usually > 60.)
	            return true;
	        }
	    }
	    return false;
	}

	/**
	 * 
	 * @param thisMove
	 * @param minDescend Case-sensitive.
	 * @param maxDescend Case-sensitive.
	 * @param data
	 * @return
	 */
	public static boolean oddInAir(final VehicleMoveData thisMove, final double minDescend, final double maxDescend, final CheckDetails checkDetails, final MovingData data) {
	    // TODO: Guard by past move tracking, instead of minDescend and maxDescend.
	    // (Try individual if this time, let JIT do the rest.)
	    if (thisMove.yDistance < 0 && oddInAirDescend(thisMove, minDescend, maxDescend, checkDetails, data)) {
	        return true;
	    }
	    return false;
	}

}
