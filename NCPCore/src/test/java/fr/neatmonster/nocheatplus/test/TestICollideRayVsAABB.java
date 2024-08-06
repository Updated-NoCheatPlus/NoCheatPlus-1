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
package fr.neatmonster.nocheatplus.test;

import static org.junit.Assert.fail;

import org.junit.Test;

import fr.neatmonster.nocheatplus.utilities.collision.ray.CollideRayVsAABB;
import fr.neatmonster.nocheatplus.utilities.collision.ray.ICollideRayVsAABB;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;

public class TestICollideRayVsAABB {

    @Test
    public void testBlocks() {

        ICollideRayVsAABB boulder = new CollideRayVsAABB();
        boulder.setFindNearestPointIfNotCollide(true); // Prefer to have the option.

        // Simple x.
        if (!boulder.setRay(0.5, 1.75, 0.5, 1.0, 0.0, 0.0)
                .setAABB(3, 1, 0, 0.0)
                .loop()
                .collides()) {
            doFail(boulder);
        }
        if (!boulder.setRay(0.5, 1.75, 0.5, -1.0, 0.0, 0.0)
                .setAABB(-3, 1, 0, 0.0)
                .loop()
                .collides()) {
            doFail(boulder);
        }
        // Simple y.
        if (!boulder.setRay(0.5, 1.75, 0.5, 0.0, 1.0, 0.0)
                .setAABB(0, 3, 0, 0.0)
                .loop()
                .collides()) {
            doFail(boulder);
        }
        if (!boulder.setRay(0.5, 1.75, 0.5, 0.0, -1.0, 0.0)
                .setAABB(0, -3, 0, 0.0)
                .loop()
                .collides()) {
            doFail(boulder);
        }
        // Simple z.
        if (!boulder.setRay(0.5, 1.75, 0.5, 0.0, 0.0, 1.0)
                .setAABB(0, 1, 3, 0.0)
                .loop()
                .collides()) {
            doFail(boulder);
        }
        if (!boulder.setRay(0.5, 1.75, 0.5, 0.0, 0.0, -1.0)
                .setAABB(0, 1, -3, 0.0)
                .loop()
                .collides()) {
            doFail(boulder);
        }
    }

    private void doFail(ICollideRayVsAABB boulder) {
        fail("Failed: collides: " + boulder.collides() + " , distSq: " + boulder.getClosestDistanceSquared() + " , pos: " + LocUtil.simpleFormat(boulder));
    }

}
