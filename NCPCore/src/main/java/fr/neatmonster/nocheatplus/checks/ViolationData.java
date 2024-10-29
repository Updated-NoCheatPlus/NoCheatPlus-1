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
package fr.neatmonster.nocheatplus.checks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.Action;
import fr.neatmonster.nocheatplus.actions.ActionData;
import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.actions.types.GenericLogAction;
import fr.neatmonster.nocheatplus.actions.types.PenaltyAction;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeHealth;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.logging.StreamID;
import fr.neatmonster.nocheatplus.penalties.DefaultPenaltyList;
import fr.neatmonster.nocheatplus.penalties.IPenaltyList;
import fr.neatmonster.nocheatplus.permissions.RegisteredPermission;

/**
 * Violation specific data, for executing actions.<br>
 * This is meant to capture a violation incident in a potentially thread safe way.
 * <hr>
 * TODO: Re-think visibility questions.
 * @author asofold
 */
public class ViolationData implements IViolationInfo, ActionData {

    // TODO: 

    /** The actions to be executed. */
    public final ActionList actions;

    /** The actions applicable for the violation level. */
    public final Action<ViolationData, ActionList>[] applicableActions;

    /** The violation level added. */
    public final double     addedVL;

    /** The check. */
    public final Check      check;

    /** The player. */
    public final Player     player;

    /** The violation level. */
    public final double     vL;

    /** Filled in parameters. */
    private Map<ParameterName, String> parameters = null;

    private boolean needsParameters = false;

    private boolean willCancel;

    /** Group penalties by input type. */
    private final IPenaltyList penaltyList;

    /**
     * Instantiates a new violation data without support for input-specific penalties..
     * <hr>
     * This constructor must be thread-safe for checks that might be executed
     * outside of the primary thread.
     * 
     * @param check
     *            the check
     * @param player
     *            the player
     * @param vL
     *            the violation level
     * @param addedVL
     *            the violation level added
     * @param actions
     *            the actions
     */
    public ViolationData(final Check check, final Player player, 
            final double vL, final double addedVL, 
            final ActionList actions) {
        this(check, player, vL, addedVL, actions, null);
    }

    /**
     * Instantiates a new violation data.
     * <hr>
     * This constructor must be thread-safe for checks that might be executed
     * outside of the primary thread.
     * 
     * @param check
     *            the check
     * @param player
     *            the player
     * @param vL
     *            the violation level
     * @param addedVL
     *            the violation level added
     * @param actions
     *            the actions
     * @param penaltyList
     *            IPenaltyList instances for filling in, or null to skip, in
     *            which case a list is created internally.
     */
    public ViolationData(final Check check, final Player player, 
            final double vL, final double addedVL, 
            final ActionList actions, final IPenaltyList penaltyList) {
        this.check = check;
        this.player = player;
        this.vL = vL;
        this.addedVL = addedVL;
        this.actions = actions;
        this.applicableActions = actions.getActions(vL); // TODO: Consider storing applicableActions only if history wants it.
        this.penaltyList = penaltyList == null ? new DefaultPenaltyList() : penaltyList;
        boolean needsParameters = false;

        for (int i = 0; i < applicableActions.length; i++) {
            final Action<ViolationData, ActionList> action = applicableActions[i];
            if (!needsParameters && action.needsParameters()) {
                needsParameters = true;
            }
            if (action instanceof PenaltyAction) {
                // Note that penalty:action:... or action:penalty:... should be forbidden.
                ((PenaltyAction<ViolationData, ActionList>) action).evaluate(this.penaltyList);
            }
        }
        if (this.penaltyList.willCancel()) {
            // Note: IPenaltyList.isEmpty() might return true, despite willCancel set.
            willCancel = true;
        }
        this.needsParameters = needsParameters;
    }

    @Override
    public boolean willCancel() {
        return willCancel;
    }

    /**
     * For penalty evaluation.
     */
    public void forceCancel() {
        willCancel = true;
    }

    /**
     * Override willCancel.
     */
    public void preventCancel() {
        willCancel = false;
    }



    /**
     * Gets the actions.
     * 
     * @return the actions
     */
    public Action<ViolationData, ActionList> [] getActions() {
        return applicableActions;
    }

    /**
     * Execute actions and apply player-specific penalties. Does add to history.
     * 
     * @return
     */
    public void executeActions() {

        // TODO: Still can return boolean for cancel, as it should've been evaluated on creation of ViolationData.
        // TODO: Might be better to return ViolationData, for penalty effects etc.

        try {
            // Statistics.
            ViolationHistory.getHistory(player).log(check.getClass().getName(), addedVL);
            // TODO: the time is taken here, which makes sense for delay, but otherwise ?
            final long time = System.currentTimeMillis() / 1000L;
            // Execute actions, if history wants it. TODO: Consider storing applicableActions only if history wants it.
            for (final Action<ViolationData, ActionList>  action : applicableActions) {
                if (Check.getHistory(player).executeAction(this, action, time)) {
                    // The execution history said it really is time to execute the action, find out what it is and do
                    // what is needed.
                    action.execute(this); // TODO: Add history as argument rather.
                }
            }
            // Apply player penalties here (and remove applied penalties).
            if (!penaltyList.isEmpty()) {
                penaltyList.applyAllApplicablePenalties(player, true);
            }
            // PenaltyAction instances.
            if (!penaltyList.isEmpty()) {
                penaltyList.applyPenaltiesPrecisely(ViolationData.class, this, true);
            }
        } catch (final Exception e) {
            StaticLog.logSevere(e);
            // On exceptions cancel events.
            willCancel = true;
        }
    }

    /**
     * Execute a single action with try-catch and cancel-on-exception. Intended
     * for interoperability with penalties.
     * 
     * @param action
     */
    public void executeAction(final Action<ViolationData, ActionList> action) {
        try {
            // Statistics.
            ViolationHistory.getHistory(player).log(check.getClass().getName(), addedVL);
            final long time = System.currentTimeMillis() / 1000L;
            // Execute actions, if history wants it. TODO: Consider storing applicableActions only if history wants it.
            if (Check.getHistory(player).executeAction(this, action, time)) {
                action.execute(this);
            }
        }
        catch (final Exception e) {
            StaticLog.logSevere(e);
            // On exceptions cancel events.
            willCancel = true;
        }
    }

    @Override
    public String getParameter(final ParameterName parameterName) {
        if (parameterName == null) {
            return "<???>";
        }
        String value = null;
        if (parameters != null) {
            value = parameters.get(parameterName);
        }
        if (value == null) {
            value = getImplicitValue(parameterName);
        }
        // If null, return what would have been parsed to get the parameter.
        return (value == null) ? ("[" + parameterName.getText() + "]") : value;
    }

    /**
     * Evaluate parameters that are supported to be fetched, even if no
     * parameters have been set explicitly.
     * 
     * @param parameterName
     * @return
     */
    private String getImplicitValue(final ParameterName parameterName) {
        switch (parameterName) {
            case CHECK:
                return check.getClass().getSimpleName();
            case HEALTH: {
                // Not optimal to get dynamically if the ViolationData is created off the primary thread. 
                String health = getParameterValue(ParameterName.HEALTH);
                return health == null ? (BridgeHealth.getHealth(player) + "/" + BridgeHealth.getMaxHealth(player)) : health;
            }
            case IP:
                return player.getAddress().toString().substring(1).split(":")[0];
            case PLAYER:
            case PLAYER_NAME:
                return player.getName();
            case PLAYER_DISPLAY_NAME:
                return player.getDisplayName();
            case UUID:
                return player.getUniqueId().toString();
            case VIOLATIONS:
                return String.valueOf((long) Math.round(vL));
            case WORLD: {
                String world = getParameterValue(ParameterName.WORLD);
                return world == null ? player.getWorld().getName() : world;
            }
            default:
                return null;
        }
    }

    private  String getParameterValue(ParameterName parameterName) {
        return parameters == null ? null : parameters.get(parameterName);
    }

    @Override
    public void setParameter(final ParameterName parameterName, final String value) {
        if (parameters == null) {
            parameters = new HashMap<ParameterName, String>();
        }
        parameters.put(parameterName, value);
    }

    /**
     * Convenience method: Delegates to setParameter, return this for chaining.
     * 
     * @param parameterName
     * @param value
     * @return This ViolationData instance.
     */
    public ViolationData chainParameter(final ParameterName parameterName, final String value) {
        setParameter(parameterName, value);
        return this;
    }

    @Override
    public boolean needsParameters() {
        return needsParameters;
    }

    @Override
    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }

    @Override
    public double getAddedVl() {
        return addedVL;
    }

    @Override
    public double getTotalVl() {
        return vL;
    }

    public RegisteredPermission getPermissionSilent() {
        return actions.permissionSilent;
    }

    public ActionList getActionList() {
        return actions;
    }

    /**
     * Test if there exist log actions in applicableActions that might log to
     * the referenced stream. This method will iterate over applicableActions,
     * it's not very efficient to call it multiple times (unless for JIT).
     * Usually actions are stored in 'optimized' form, thus this result should
     * be final. If an action is not stored in optimized form, it might still
     * check for configuration flags on execute (bugs or custom actions
     * implementations).
     * 
     * @param streamID
     * @return
     */
    public boolean logsToStream(final StreamID streamID) {
        if (!needsParameters) {
            return false;
        }
        for (final Action <ViolationData, ActionList> action : applicableActions) {
            if ((action instanceof GenericLogAction) && ((GenericLogAction) action).logsToStream(streamID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if there are any instances of GenericLogAction in the
     * applicableActions. This method will iterate over applicableActions, it's
     * not efficient to call it extra to logsToStream. Usually actions are
     * stored in 'optimized' form, thus this result should be final. If an
     * action is not stored in optimized form, it might still check for
     * configuration flags on execute (bugs or custom actions implementations).
     * 
     * @return
     */
    public boolean hasLogAction() {
        if (!needsParameters) {
            return false;
        }
        for (final Action <ViolationData, ActionList> action : applicableActions) {
            if (action instanceof GenericLogAction) {
                return true;
            }
        }
        return false;
    }

}
