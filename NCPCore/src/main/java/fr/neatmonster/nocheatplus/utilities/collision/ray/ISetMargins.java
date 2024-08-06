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
package fr.neatmonster.nocheatplus.utilities.collision.ray;

public interface ISetMargins {

    /**
     * Typical player specific margins (none below feet, eye height, same
     * xzMargin to all sides from the center). Calling this may or may not have
     * effect.
     * 
     * @param height
     * @param xzMargin
     */
    public void setMargins(final double height, final double xzMargin);

    /**
     * Allow cutting off the margins opposite to a checking direction. Call
     * before loop. MAy or may not have any effect.
     * 
     * @param cutOppositeDirectionMargin
     *            If set to true, margins that are opposite to the moving
     *            direction are cut off. This is meant for setups like with
     *            moving out of blocks.
     */
    public void setCutOppositeDirectionMargin(boolean cutOppositeDirectionMargin);

}
