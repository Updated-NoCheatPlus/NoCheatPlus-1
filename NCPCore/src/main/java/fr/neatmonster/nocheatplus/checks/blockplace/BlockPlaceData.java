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

import java.util.ArrayList;
import java.util.List;

import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;

/**
 * Player specific dataFactory for the block place checks.
 */
public class BlockPlaceData extends ACheckData {

    // Violation levels.
    public double againstVL;
    public double autoSignVL;
    public double directionVL;
    public double fastPlaceVL;
    public double noSwingVL;
    public double reachVL;
    public double scaffoldVL;
    public double speedVL;

    // AutoSign.
    public long signOpenTime = 0;
    /** Using Material.SIGN . */
    public long autoSignPlacedHash = 0;

    // Scaffold data
    public List<Long> placeTick = new ArrayList<>();
	public long sneakTime = 0;
	public long sprintTime = 0;
	public float lastYaw = 0;
	public int lastSlot = 0;
	public long currentTick = 0;
	public boolean cancelNextPlace = false;

    // Data of the fast place check.
    public final ActionFrequency fastPlaceBuckets = new ActionFrequency(2, 1000);
    public int fastPlaceShortTermTick = 0;
    public int fastPlaceShortTermCount = 0;

    // Data of the no swing check.
    public int noSwingCount = 0;

    // Data of the speed check;
    public boolean speedLastRefused;
    public long speedLastTime;
}