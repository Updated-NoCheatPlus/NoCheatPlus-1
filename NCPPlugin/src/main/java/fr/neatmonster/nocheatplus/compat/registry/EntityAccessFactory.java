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
package fr.neatmonster.nocheatplus.compat.registry;

import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.components.entity.IEntityAccessVehicle;

/**
 * Set up more fine grained entity access providers, registered as generic
 * instances for interfaces for now. Namely:
 * <ul>
 * <li>IEntityAccessPositionAndLook</li>
 * </ul>
 * 
 * @author asofold
 *
 */
public class EntityAccessFactory {

    /**
     * Set up alongside with MCAccess. The MCAccess instance is passed here,
     * before it has been set internally and before it has been advertised to
     * MCAccessHolder instances, so the latter can get other specific access
     * providers during handling setMCAccess.
     * 
     * @param mcAccess
     * @param config
     */
    public void setupEntityAccess(final MCAccess mcAccess, final MCAccessConfig config) {

        // IEntityAccessLastPositionAndLook
        //RegistryHelper.setupGenericInstance(new String[] {
        //        "fr.neatmonster.nocheatplus.compat.cbdev.EntityAccessLastPositionAndLook",
        //        "fr.neatmonster.nocheatplus.compat.spigotcb1_10_R1.EntityAccessLastPositionAndLook",
        //        "fr.neatmonster.nocheatplus.compat.spigotcb1_9_R2.EntityAccessLastPositionAndLook",
        //        "fr.neatmonster.nocheatplus.compat.spigotcb1_9_R1.EntityAccessLastPositionAndLook",
        //}, new String[] {
        //        "fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectEntityLastPositionAndLook",
        //}, IEntityAccessLastPositionAndLook.class, config, false);

        // IEntityAccessVehicle
        RegistryHelper.registerFirstAvailable(new String[] {
                "fr.neatmonster.nocheatplus.compat.bukkit.EntityAccessVehicleMultiPassenger",
                "fr.neatmonster.nocheatplus.compat.bukkit.EntityAccessVehicleLegacy",
        }, IEntityAccessVehicle.class, false);
    }

}
