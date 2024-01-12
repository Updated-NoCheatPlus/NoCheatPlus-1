package fr.neatmonster.nocheatplus.checks.moving.envelope;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;

/**
 * Confine most/some horizontal workarounds in here.
 */
public class HorizontalUncertainty {
    
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
            if (collidingEntities > 0) {
                // Cap the multiplier at 4.0
                // TODO: STILL NOT ENOUGH
                xTheoreticalDistance[x_idx] *= Math.min(4.0, (1.0 + (collidingEntities * 0.0999)));
                zTheoreticalDistance[z_idx] *= Math.min(4.0, (1.0 + (collidingEntities * 0.0999)));
                player.sendMessage("pushed");
                return true;
            }
        }
        return false;
    }
}
