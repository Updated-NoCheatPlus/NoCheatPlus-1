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

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckConfig;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.versions.Bugs;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.config.value.OverrideType;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.worlds.IWorldData;

/**
 * Configurations specific for the "fight" checks. Every world gets one of these assigned to it, or if a world doesn't
 * get it's own, it will use the "global" version.
 */
public class FightConfig extends ACheckConfig {

    public final double angleMove;
    public final double angleYaw;
    public final double angleSwitch;
    public final double angleTime;
    public final ActionList angleActions;

    public final long toolChangeAttackPenalty;

    public final ActionList criticalActions;

    public final boolean directionStrict;
    public final boolean directionFailAll;
    public final double directionloopprecision;
    public final double directionangleprecision;
    public final long directionPenalty;
    public final ActionList directionActions;

    public final long fastHealInterval;
    public final long fastHealBuffer;
    public final ActionList fastHealActions;

    public final long godModeLagMinAge;
    public final long godModeLagMaxAge;
    public final ActionList godModeActions;

    public final ActionList noSwingActions;

    public final long reachPenalty;
    public final boolean reachPrecision;
    public final boolean reachReduce;
    public final double	reachSurvivalDistance;
    public final double reachReduceDistance;
    public final double reachReduceStep;
    public final boolean reachImprobableFeedOnly;
    public final float reachImprobableWeight;

    public final ActionList reachActions;

    public final ActionList selfHitActions;

    public final ActionList visibleActions;

    // Special flags:
    public final boolean cancelDead;
    public boolean enforceItemRelease;
    public boolean enforceClosedInventory;
    public final boolean knockBackVelocityPvP;

    /** Maximum latency counted in ticks for the loop checks (reach, direction). */
    public final long loopMaxLatencyTicks; // TODO: Configurable,  sections for players and entities.
    
    /**
     * Instantiates a new fight configuration.
     * 
     * @param worldData
     *            the worldData
     */
    public FightConfig(final IWorldData worldData) {
        super(worldData);
        final ConfigFile config = worldData.getRawConfiguration();
        angleMove = config.getDouble(ConfPaths.FIGHT_ANGLE_THRESHOLD_MOVE);
        angleSwitch = config.getDouble(ConfPaths.FIGHT_ANGLE_THRESHOLD_SWITCH);
        angleYaw = config.getDouble(ConfPaths.FIGHT_ANGLE_THRESHOLD_YAW);
        angleTime = config.getDouble(ConfPaths.FIGHT_ANGLE_THRESHOLD_TIME);
        angleActions = config.getOptimizedActionList(ConfPaths.FIGHT_ANGLE_ACTIONS, Permissions.FIGHT_ANGLE);

        toolChangeAttackPenalty = config.getLong(ConfPaths.FIGHT_TOOLCHANGEPENALTY);

        criticalActions = config.getOptimizedActionList(ConfPaths.FIGHT_CRITICAL_ACTIONS, Permissions.FIGHT_CRITICAL);

        directionStrict = config.getBoolean(ConfPaths.FIGHT_DIRECTION_STRICT);
		directionFailAll = config.getBoolean(ConfPaths.FIGHT_DIRECTION_FAILALL);
        directionangleprecision = config.getDouble(ConfPaths.FIGHT_DIRECTION_STRICTANGLEPRECISION, 50.0, 100.0, 80.0);
        directionloopprecision = config.getDouble(ConfPaths.FIGHT_DIRECTION_LOOPPRECISION, 0.0, 2.0, 0.5);
        directionPenalty = config.getLong(ConfPaths.FIGHT_DIRECTION_PENALTY);
        directionActions = config.getOptimizedActionList(ConfPaths.FIGHT_DIRECTION_ACTIONS, Permissions.FIGHT_DIRECTION);
        if (ServerVersion.compareMinecraftVersion("1.9") >= 0) {
            /** Note: Disable check should use
             *    NCPAPIProvider#getNoCheatPlusAPI()#getWorldDataManager()#overrideCheckActivation()
             *  to actually disable from all worlds. 
             *  Using worldData from config will only affect
             *  on world they are staying on join; And on other worlds still remain unchanged.
             */
            NCPAPIProvider.getNoCheatPlusAPI().getWorldDataManager().overrideCheckActivation(
                    CheckType.FIGHT_FASTHEAL, AlmostBoolean.NO, 
                    OverrideType.PERMANENT, true);
        }
        if (ServerVersion.compareMinecraftVersion("1.8") >= 0) {
            NCPAPIProvider.getNoCheatPlusAPI().getWorldDataManager().overrideCheckActivation(
                    CheckType.FIGHT_SELFHIT, AlmostBoolean.NO, 
                    OverrideType.PERMANENT, true);
        }
        fastHealInterval = config.getLong(ConfPaths.FIGHT_FASTHEAL_INTERVAL);
        fastHealBuffer = config.getLong(ConfPaths.FIGHT_FASTHEAL_BUFFER);
        fastHealActions = config.getOptimizedActionList(ConfPaths.FIGHT_FASTHEAL_ACTIONS, Permissions.FIGHT_FASTHEAL);

        godModeLagMinAge = config.getLong(ConfPaths.FIGHT_GODMODE_LAGMINAGE);
        godModeLagMaxAge = config.getLong(ConfPaths.FIGHT_GODMODE_LAGMAXAGE);
        godModeActions = config.getOptimizedActionList(ConfPaths.FIGHT_GODMODE_ACTIONS, Permissions.FIGHT_GODMODE);

        noSwingActions = config.getOptimizedActionList(ConfPaths.FIGHT_NOSWING_ACTIONS, Permissions.FIGHT_NOSWING);
        
        reachSurvivalDistance = config.getDouble(ConfPaths.FIGHT_REACH_SURVIVALDISTANCE, 3.5, 6.0, 4.1);
        reachPenalty = config.getLong(ConfPaths.FIGHT_REACH_PENALTY);
        reachPrecision = config.getBoolean(ConfPaths.FIGHT_REACH_PRECISION);
        reachReduce = config.getBoolean(ConfPaths.FIGHT_REACH_REDUCE);
        reachReduceDistance = config.getDouble(ConfPaths.FIGHT_REACH_REDUCEDISTANCE, 0, reachSurvivalDistance, 0.9);
        reachReduceStep = config.getDouble(ConfPaths.FIGHT_REACH_REDUCESTEP, 0, reachReduceDistance, 0.15);
        reachImprobableFeedOnly = config.getBoolean(ConfPaths.FIGHT_REACH_IMPROBABLE_FEEDONLY);
        reachImprobableWeight = (float) config.getDouble(ConfPaths.FIGHT_REACH_IMPROBABLE_WEIGHT);
        reachActions = config.getOptimizedActionList(ConfPaths.FIGHT_REACH_ACTIONS, Permissions.FIGHT_REACH);

        selfHitActions = config.getOptimizedActionList(ConfPaths.FIGHT_SELFHIT_ACTIONS, Permissions.FIGHT_SELFHIT);

        visibleActions = config.getOptimizedActionList(ConfPaths.FIGHT_VISIBLE_ACTIONS, Permissions.FIGHT_VISIBLE);

        cancelDead = config.getBoolean(ConfPaths.FIGHT_CANCELDEAD);
        enforceItemRelease = config.getBoolean(ConfPaths.FIGHT_ENFORCE_ITEM_RELEASE);
        enforceClosedInventory = config.getBoolean(ConfPaths.FIGHT_ENFORCE_CLOSED_INVENTORY);
        
		loopMaxLatencyTicks = config.getInt(ConfPaths.FIGHT_MAXLOOPLETENCYTICKS, 1, 15, 8);
        AlmostBoolean ref = config.getAlmostBoolean(ConfPaths.FIGHT_KNOCKBACKVELOCITY, AlmostBoolean.MAYBE);
        knockBackVelocityPvP = ref == AlmostBoolean.MAYBE ? Bugs.shouldPvpKnockBackVelocity() : ref.decide();
    }
}