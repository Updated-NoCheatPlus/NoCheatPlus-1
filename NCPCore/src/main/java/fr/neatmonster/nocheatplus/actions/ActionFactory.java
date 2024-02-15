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
package fr.neatmonster.nocheatplus.actions;

import java.util.Map;

import fr.neatmonster.nocheatplus.actions.types.CancelAction;
import fr.neatmonster.nocheatplus.actions.types.LogAction;
import fr.neatmonster.nocheatplus.actions.types.PenaltyAction;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.penalties.CancelPenalty;
import fr.neatmonster.nocheatplus.penalties.PenaltyNode;

/**
 * Helps with creating Actions out of text string definitions.
 */
public class ActionFactory extends AbstractActionFactory<ViolationData, ActionList> {

    /**
     * Instantiates a new action factory.
     * 
     * @param library
     *            the library
     */
    public ActionFactory(final Map<String, Object> library) {
        super(library, ActionList.listFactory);
    }

    /**
     * Creates a new Action object.
     * 
     * @param actionDefinition
     *            the action definition
     * @return the action
     */
    public Action<ViolationData, ActionList> createAction(String actionDefinition) {
        actionDefinition = actionDefinition.toLowerCase();

        if (actionDefinition.equals("cancel")) {
            return new CancelAction<ViolationData, ActionList>();
        }

        if (actionDefinition.endsWith("%cancel")) {
            try {
                Double probability = Double.parseDouble(actionDefinition.substring(
                        0, actionDefinition.length() - 7));
                if (!Double.isInfinite(probability) 
                        && !Double.isNaN(probability) 
                        && probability > 0.0) {
                    // TODO: parsing via factory, store implicit penalties there too.
                    return new PenaltyAction<ViolationData, ActionList>(
                        "imp_" + actionDefinition, new PenaltyNode(
                        probability / 100.0,
                        CancelPenalty.CANCEL));
                }
            }
            catch (NumberFormatException e) {
            }
            StaticLog.logWarning("Bad probability definition for cancel action: '" + actionDefinition + "', relay to always cancelling.");
            return new CancelAction<ViolationData, ActionList>();
        }

        if (actionDefinition.startsWith("cmd:")) {
            return parseCmdAction(actionDefinition.split(":", 2)[1]);
        }

        if (actionDefinition.startsWith("cmdc:")) {
            return parseCmdAction(actionDefinition.split(":", 2)[1], true);
        }

        if (actionDefinition.startsWith("log:")) {
            return parseLogAction(actionDefinition.split(":", 2)[1]);
        }

        throw new IllegalArgumentException("NoCheatPlus doesn't understand action '" + actionDefinition + "' at all.");
    }



    /**
     * Parses the log action.
     * 
     * @param definition
     *            the definition
     * @return the action
     */
    protected Action<ViolationData, ActionList> parseLogAction(final String definition) {
        final String[] parts = definition.split(":");
        final String name = parts[0];
        final Object message = lib.get(parts[0]);
        int delay = 0;
        int repeat = 1;
        boolean toConsole = true;
        boolean toFile = true;
        boolean toChat = true;

        if (message == null) {
            throw new IllegalArgumentException("Can't log, due to entry missing in strings: '" + name);
        }

        try {
            delay = Integer.parseInt(parts[1]);
            repeat = Integer.parseInt(parts[2]);
            toConsole = parts[3].contains("c");
            toChat = parts[3].contains("i");
            toFile = parts[3].contains("f");
        } catch (final Exception e) {
            StaticLog.logWarning("Couldn't parse details of log action '" + definition
                    + "', will use default values instead.");
            StaticLog.logWarning(e);
            delay = 0;
            repeat = 1;
            toConsole = true;
            toFile = true;
            toChat = true;
        }

        return new LogAction(name, delay, repeat, toChat, toConsole, toFile, message.toString());
    }
}
