package fr.neatmonster.nocheatplus.checks.moving.envelope;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Confine most/some of the horizontal workarounds in here.
 */
public class HorizontalUncertainty {
    

    /**
     * Try testing for horizontal collision on the X axis.
     * If the player has just touched a wall, do increase speed for that one transition.
     * Otherwise, simply set the highest predicted speed, without any other modification (essentially ignorning the very slight slow down on moving against walls).<br>
     * Without Minecraft's collision function, this move is hidden and cannot be predicted.
     * 
     * @param from
     * @param to
     * @param xTheoreticalDistance
     * @param x_idx
     * @return True, if to apply the workaround.
     */
    public static boolean tryDoWallCollisionX(final PlayerLocation from, final PlayerLocation to, double[] xTheoreticalDistance, int x_idx) {
        // Test "just-touched-a-wall" moves first.
        if (!from.isNextToSolid(0.01, 0.0) && to.isNextToSolid(0.01, 0.0)) {
            // TODO: Naming convention for this kind of transition? ... Slamming/banging/ramming against a wall?
            // Here, speed must be increased slightly.
            xTheoreticalDistance[x_idx] *= 1.09;
            return true;
        }
        // Fail, try using bigger margins (use the currently predicted speed as margins)
        if (!from.isNextToSolid(Math.min(0.9, xTheoreticalDistance[x_idx]), 0.0) && to.isNextToSolid(Math.min(0.9, xTheoreticalDistance[x_idx]), 0.0)) {
            xTheoreticalDistance[x_idx] *= 1.09;
            return true;
        }
        // Fail, check if the player is moving alongside (against, rather?) a wall
        if (from.isNextToSolid(0.01, 0.0) && to.isNextToSolid(0.01, 0.0)) {
            // Just ignore.
            // This means that cheaters can speed up to normal speed against a wall: this is a compromise between false positives VS protection VS maintainability.
            // Are there any MAJOR gamemodes that do rely on this very specific mechanic of the game? Except for parkour I'd imagine.
            // In any case, this "workaround" is likely going to be structural, unless someone can come up with a better solution, or we reocde NCP to make use of MC's collision.
            return true;
        }
        // Last attempt, try using bigger margins (use the currently predicted speed as margins)
        if (from.isNextToSolid(Math.min(0.9, xTheoreticalDistance[x_idx]), 0.0) && to.isNextToSolid(Math.min(0.9, xTheoreticalDistance[x_idx]), 0.0)) {
            // Just ignore.
            return true;
        }
        return false;
    } 

    /**
     * Try testing for horizontal collision on the Z axis.
     * If the player has just touched a wall, do increase speed for that one transition.
     * Otherwise, simply set the highest predicted speed, without any other modification (essentially ignorning the very slight slow down on moving against walls).<br>
     * Without Minecraft's collision function, this move is hidden and cannot be predicted.
     * 
     * @param from
     * @param to
     * @param zTheoreticalDistance
     * @param z_idx
     * @return True, if to apply the workaround.
     */
    public static boolean tryDoWallCollisionZ(final PlayerLocation from, final PlayerLocation to, double[] zTheoreticalDistance, int z_idx) {
        // Test for "ramming-against-a-wall" moves first (a move that has not collision(!from) and slams ont a wall(to))
        if (!from.isNextToSolid(0.0, 0.01) && to.isNextToSolid(0.0, 0.01)) {
            zTheoreticalDistance[z_idx] *= 1.099;
            return true;
        }
        // Fail, try using bigger margins (use the currently predicted speed as margins, cap at 0.9)
        if (!from.isNextToSolid(0.0, Math.min(0.9, zTheoreticalDistance[z_idx])) && to.isNextToSolid(0.0, Math.min(0.9, zTheoreticalDistance[z_idx]))) {
            zTheoreticalDistance[z_idx] *= 1.099;
            return true;
        }
        // Fail, check if the player is moving alongside (against rather?) a wall
        if (from.isNextToSolid(0.0, 0.01) && to.isNextToSolid(0.0, 0.01)) {
            return true;
        }
        // Last attempt, try using bigger margins (use the currently predicted speed as margins)
        if (from.isNextToSolid(0.0, zTheoreticalDistance[z_idx]) && to.isNextToSolid(0.0, zTheoreticalDistance[z_idx])) {
            return true;
        }
        return false;
    } 
    
    /**
     * Applies an arbitrary amount of grace, based on the amount of entities that may intersect with the player's AABB, within a 0.5 blocks of range.
     * 
     * @param player
     * @param isPredictable
     * @param pData
     * @param xTheoreticalDistance
     * @param zTheoreticalDistance
     * @param x_idx
     * @param z_idx
     * @return True, if the workaround applies.
     */
    public static boolean applyEntityPushingGrace(final Player player, final IPlayerData pData, double[] xTheoreticalDistance, double[] zTheoreticalDistance, int x_idx, int z_idx, final PlayerLocation to) {
        // This mechanic was added in 1.9
        int collidingEntities = 0;
        if (pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            collidingEntities = CollisionUtil.getNumberOfEntitiesThatCanPushThePlayer(player, 0.5, 0.0, 0.5);
            // If the player has collided with an entity and is moving backwards, assume the got pushed by the entity.
            if (collidingEntities > 0 && TrigUtil.isMovingBackwards(xTheoreticalDistance[x_idx], zTheoreticalDistance[z_idx], to.getYaw())) {
                // Cap the multiplier at 4.0
                xTheoreticalDistance[x_idx] *= Math.min(4.0, (1.0 + (collidingEntities * 0.099)));
                zTheoreticalDistance[z_idx] *= Math.min(4.0, (1.0 + (collidingEntities * 0.099)));
                player.sendMessage("pushed");
                return true;
            }
        }
        return false;
    }
}
