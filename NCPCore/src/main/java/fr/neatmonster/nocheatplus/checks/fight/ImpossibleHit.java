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
package fr.neatmonster.nocheatplus.checks.fight;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.blockinteract.BlockInteractData;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;

/**
 * A check to see if players try to attack entities while performing something else
 * (i.e.: while blocking)
 */
public class ImpossibleHit extends Check {


    /**
     * Instantiates a new impossiblehit check.
     */
    public ImpossibleHit() {
        super(CheckType.FIGHT_IMPOSSIBLEHIT);
    }


    /**
     * Checks a player.
     * 
     * @param player
     * @param data
     * @param cc
     * @param pData
     * @param resetActiveItem Whether the resetActiveitem option in Sf is enabled
     * @return true, if successful
     */
    public boolean check(final Player player, final FightData data, final FightConfig cc, final IPlayerData pData, final boolean resetActiveItem) {

        boolean cancel = false;
        boolean violation = false;
        final long currentEventTime = System.currentTimeMillis();
        boolean slientCancel = false;
        List<String> tags = new LinkedList<String>();
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        final BlockInteractData biData = pData.getGenericInstance(BlockInteractData.class);
        
        // Meta check: Fight.direction passed, blockinteract.direction failed ->
        // this is a server-sided rotation (player is interacting with something in another direction client-side)
        // TODO: Adapt to the new Fight.Hitbox check, once merged in master
        if ((data.lookFight == -1 && biData.lookInteraction == 0)) {
            violation = true;
            // Consume the flags.
            data.lookFight = -1;
            biData.lookInteraction = -1;
            tags.add("look_mismatch");
        }
        // Can't attack with inventory open.
        else if (InventoryUtil.hasAnyInventoryOpen(player)) {
            violation = true;
            tags.add("inventoryopen");
            slientCancel = true;
        }
        // Blocking/Using item and attacking
        else if ((mData.isUsingItem || player.isBlocking()) && !resetActiveItem) {
            violation = true;
            tags.add("using/blocking");
        }
        // (While dead is canceled silentely, while sleeping shouldn't be possible...)
        // TODO: Is there more to prevent?
        // TODO: Might also want to prevent on packet-level

        // Handle violations 
        if (violation) {
            data.impossibleHitVL += 1D;
            final ViolationData vd = new ViolationData(this, player, data.impossibleHitVL, 1D, cc.impossibleHitActions);
            if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
            cancel = executeActions(vd).willCancel();
            if (slientCancel && cancel) {
                cancel = false;
                player.closeInventory();
            }
        }
        // Cooldown
        else {
            data.impossibleHitVL *= 0.96D;
        }
        return cancel;
    }
}
