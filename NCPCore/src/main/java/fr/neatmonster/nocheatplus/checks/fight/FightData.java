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

import java.util.Collection;
import java.util.LinkedList;

import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.components.data.IDataOnRemoveSubCheckData;
import fr.neatmonster.nocheatplus.components.data.IDataOnWorldChange;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.PenaltyTime;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;

/**
 * Player specific data for the fight checks.
 */
public class FightData extends ACheckData implements IDataOnRemoveSubCheckData, IDataOnWorldChange {

    // Violation levels.
    public double angleVL;
    public double criticalVL;
    public double directionVL;
    public double fastHealVL;
    public double godModeVL;
    public double noSwingVL;
    public double reachVL;
    public double speedVL;
    public double impossibleHitVL;
    public double visibleVL;

    // Shared data between checks
    public String lastWorld = "";
    public int lastAttackTick = 0;
    public double lastAttackedX = Double.MAX_VALUE;
    public double lastAttackedY;
    public double lastAttackedZ;
    /** Attack penalty (close combat, ENTITY_ATTACK). */
    public final PenaltyTime attackPenalty = new PenaltyTime();
    /** The entity id which might get counter-attacked. */
    public int thornsId = Integer.MIN_VALUE;
    /** 1.9: The tick of an attack that might lead to further sweep damage to other nearby entities. */
    public int sweepTick = 0;
    /** Hash code of the location of last sweep attack. */
    public int sweepLocationHashCode = 0;
    public long damageTakenByEntityTick;
    // TNT workaround: Allow ENTITY_ATTACK if these attributes match.
    // Discussion at: https://github.com/NoCheatPlus/NoCheatPlus/pull/17 (@Iceee)
    /** Tick the last explosion damage was dealt at. */
    public int lastExplosionDamageTick = -1;
    /** Last explosion damaged entity (id). */
    public int lastExplosionEntityId = Integer.MAX_VALUE;

    // Data of the angle check.
    public LinkedList<Angle.AttackLocation> angleHits = new LinkedList<Angle.AttackLocation>();

    // Data of the FastHeal check.
    public long fastHealRefTime = 0;
    /** Buffer has to be initialized in constructor. */
    public long fastHealBuffer = 0;
    /** Any kind of health regeneration. */
    public long regainHealthTime = 0;

    // Data of the god mode check.
    public int godModeHealthDecreaseTick = 0;
    public double godModeHealth = 0.0;
    public int lastDamageTick = 0;
    public int lastNoDamageTicks = 0;
    public int godModeAcc = 0;
    public int godModeLastAge;
    public long godModeLastTime;
    public int godModeBuffer;

    // Data of the no swing check.
    public int noSwingCount = 0;
    public boolean exemptArmSwing = false;

    // Data of the reach check.
    public double reachMod = 1.0;

    // Data of the SelfHit check.
    public ActionFrequency selfHitVL = new ActionFrequency(6, 5000);

    // Data of the ImpossibleHit check
    public int lookFight = -1;

    public FightData(final FightConfig config){
        // Start with full fast-heal buffer.
        fastHealBuffer = config.fastHealBuffer; 
    }

    @Override
    public boolean dataOnRemoveSubCheckData(final Collection<CheckType> checkTypes) {
        for (final CheckType checkType : checkTypes) {
            switch(checkType) {
                // TODO: case FIGHT: ...
                case FIGHT_DIRECTION:
                    directionVL = 0;
                    break;
                case FIGHT_REACH:
                    reachVL = 0;
                    reachMod = 1.0;
                    break;
                case FIGHT_ANGLE:
                    angleVL = 0;
                    angleHits.clear();
                    break;
                case FIGHT_FASTHEAL:
                    fastHealVL = 0;
                    fastHealRefTime = 0;
                    fastHealBuffer = 0;
                    regainHealthTime = 0;
                    break;
                case FIGHT_GODMODE:
                    godModeVL = 0;
                    godModeBuffer = 0;
                    godModeAcc = 0;
                    godModeLastTime = 0;
                    godModeLastAge = 0;
                    lastNoDamageTicks = 0; // Not sure here, possibly a shared thing.
                    // godModeHealth / ...
                    break;
                case FIGHT_CRITICAL:
                    criticalVL = 0;
                    break;
                case FIGHT_IMPOSSIBLEHIT:
                    impossibleHitVL = 0;
                    lookFight = -1;
                    break;
                case FIGHT_NOSWING:
                    noSwingVL = 0;
                    // Not reset time, for leniency rather.
                    break;
                case FIGHT_VISIBLE:
                    visibleVL = 0;
                    break;
                case FIGHT_SELFHIT:
                    selfHitVL.clear(System.currentTimeMillis());
                    break;
                case FIGHT:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean dataOnWorldChange(Player player, IPlayerData pData, World previousWorld, World newWorld) {
        angleHits.clear();
        lastAttackedX = Double.MAX_VALUE;
        lastAttackTick = 0;
        lastWorld = "";
        return false;
    }
}