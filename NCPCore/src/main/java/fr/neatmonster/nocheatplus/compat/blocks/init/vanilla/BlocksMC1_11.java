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
package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla;

import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.BlockProps;

@SuppressWarnings("deprecation")
public class BlocksMC1_11 implements BlockPropertiesSetup {

    public BlocksMC1_11() {
        BlockInit.assertMaterialExists("OBSERVER");
        BlockInit.assertMaterialExists("BLACK_SHULKER_BOX");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        // 218 OBSERVER
        // Wiki (16-11-25): 17.5, 2.65, 1.32, 0.9, 0.7, 0.45
        BlockProperties.setBlockProps("OBSERVER", 
                new BlockProps(BlockProperties.woodPickaxe, 6,
                        BlockProperties.secToMs(15.0, 2.2, 1.1, 0.7, 0.55, 0.5, 0.45)));
        BlockProperties.setBlockFlags("OBSERVER", 
                BlockFlags.FULLY_SOLID_BOUNDS);
        // ALL SORTS OF SHULKER BOXES

        StaticLog.logInfo("Added block-info for Minecraft 1.11 blocks.");
    }

}
