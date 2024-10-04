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
package fr.neatmonster.nocheatplus.checks.blockplace;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.blockinteract.BlockInteractData;
import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;


/**
 * Check if the placing is legitimate in terms of surrounding materials.
 * @author asofold
 *
 */
public class Against extends Check {


   /**
    * Instanties a new Against check.
    *
    */
    public Against() {
        super(CheckType.BLOCKPLACE_AGAINST);
    }


    /**
     * Checks a player
     * @param player
     * @param block
     * @param placedMat 
     *               The material in hand that has been placed.
     * @param blockAgainst 
     * @param isInteractBlock
     * @param data
     * @param cc
     * @param pData
     *
     */
    public boolean check(final Player player, final Block block, final Material placedMat, 
                         final Block blockAgainst, final boolean isInteractBlock, 
                         final BlockPlaceData data, final BlockPlaceConfig cc, final IPlayerData pData) {
        
        boolean violation = false;
        final BlockInteractData bIData = pData.getGenericInstance(BlockInteractData.class); // TODO: pass as argument.
        /* 
         * Do not use this to check for cheating: Bukkit will return the placed material if the placement is not possible (roughly since 1.13).
         * i.e.: Attempting to place dirt against air will return DIRT, not air as the against type.
         */
        final Material bukkitAgainst = blockAgainst.getType();
        /*
        * The block last interacted with tracked by NCP. This is what must be used to check for actual cheating.
         * Essentially, the Against check relies on the assumption that BlockInteractEvents are always (logically) fired before BlockPlaceEvents,
         * thus, getLastType() should always return a Material type. If not, the player tried to place a block without having to interact with something previously.
         */
        final Material ncpAgainst = bIData.getLastType();

        if (pData.isDebugActive(this.type)) {
            debug(player, "Placed " + placedMat.toString() + " against: " + bukkitAgainst +" (bukkit) / "+ (ncpAgainst == null ? "null" : ncpAgainst.toString()) + " (nc+)");
        }

        if (bIData.isConsumedCheck(this.type) && !bIData.isPassedCheck(this.type)) {
            // TODO: Awareness of repeated violation probably is to be implemented below somewhere.
            violation = true;
            if (pData.isDebugActive(this.type)) {
                debug(player, "Cancel due to block having been consumed by this check.");
            }
        }
        else if (isInteractBlock && !BlockProperties.isAir(ncpAgainst) && !BlockProperties.isLiquid(ncpAgainst)) {
            if (pData.isDebugActive(this.type)) {
                debug(player, "Block was placed against something, allow it.");
            }
        }
        else if (BlockProperties.isAir(ncpAgainst)) { // Holds true for null blocks.
            if (MaterialUtil.isFarmable(placedMat) && MaterialUtil.isFarmable(bukkitAgainst)) {
                // Server - client desync: place a seed/plant down -> fully grow it with bone meal -> quickly harvest/break it while also trying to place down the next seed/plant (possibly with both hands to further speed up the process)
                // Sometimes, the client sends a block placement packet which seemingly attempts to plant the seed down on the crop instead of the farmland.
                // This isn't possible, so NCP's lastType check won't register the last interacted block (= AIR, remember), resulting in a false positive.
                if (pData.isDebugActive(this.type)) {
                    debug(player, "Ignore player attempting to place a seed/plant on a crop/plant (assume desync due to fast-farming).");        
                }
            }
            else if (!pData.hasPermission(Permissions.BLOCKPLACE_AGAINST_AIR, player)
                     && placedMat != BridgeMaterial.LILY_PAD
                     && placedMat != BridgeMaterial.FROGSPAWN) {
                violation = true;
                // Attempted to place a block without having interacted in the past with the placed against block (null)
            }
        }
        else if (BlockProperties.isLiquid(ncpAgainst)) {
            if (((placedMat != BridgeMaterial.LILY_PAD || placedMat != BridgeMaterial.FROGSPAWN) || !BlockProperties.isLiquid(block.getRelative(BlockFace.DOWN).getType()))
                && !BlockProperties.isWaterPlant(ncpAgainst)
                && !pData.hasPermission(Permissions.BLOCKPLACE_AGAINST_LIQUIDS, player)) {
                violation = true;
            }
        }
        
        // Handle violation and return.
        bIData.addConsumedCheck(this.type);
        if (violation) {
            data.againstVL += 1.0;
            final ViolationData vd = new ViolationData(this, player, data.againstVL, 1, cc.againstActions);
            vd.setParameter(ParameterName.BLOCK_TYPE, ncpAgainst == null ? "air" : ncpAgainst.toString());
            return executeActions(vd).willCancel();
        }
        else {
            data.againstVL *= 0.99; // Assume one false positive every 100 blocks.
            bIData.addPassedCheck(this.type);
            return false;
        }
    }
}