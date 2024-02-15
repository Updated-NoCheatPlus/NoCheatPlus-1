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
package fr.neatmonster.nocheatplus.actions.types;

import fr.neatmonster.nocheatplus.actions.AbstractActionList;
import fr.neatmonster.nocheatplus.actions.ActionData;
import fr.neatmonster.nocheatplus.penalties.CancelPenalty;
import fr.neatmonster.nocheatplus.penalties.PenaltyNode;

/**
 * Sole purpose is to indicate that an action is to be cancelled 100% (as
 * opposed to a penalty with 30%cancel). This effects A cancel action might
 * mean:
 * <ul>
 * <li>Cancel an event.</li>
 * <li>Undo actions by players in some other way.</li>
 * <li>Do something that should've been done (enforce).</li>
 * </ul>
 */
public class CancelAction<D extends ActionData, L extends AbstractActionList<D, L>> extends PenaltyAction<D, L> {

    // TODO: Deprecate this (let it extend penalty.CancelAction)?

    private static final PenaltyNode node = new PenaltyNode(CancelPenalty.CANCEL);

    /**
     * Default cancel action.
     */
    public CancelAction() {
        super(null, node);
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.actions.Action#execute(fr.neatmonster.nocheatplus.checks.ViolationData)
     */
    @Override
    public void execute(final D data) {
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "cancel";
    }
}
