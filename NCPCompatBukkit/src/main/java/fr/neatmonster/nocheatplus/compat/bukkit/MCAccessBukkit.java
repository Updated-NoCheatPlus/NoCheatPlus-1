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
package fr.neatmonster.nocheatplus.compat.bukkit;


import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;

public class MCAccessBukkit extends MCAccessBukkitBase implements BlockPropertiesSetup {

    public MCAccessBukkit() {
        super();
    }

    @Override
    public void setupBlockProperties(final WorldConfigProvider<?> worldConfigProvider) {
        final Set<Material> itchyBlocks = new LinkedHashSet<Material>();
        for (final Material mat : Material.values()) {
            if (!mat.isBlock()) {
                continue;
            }
            else if (guessItchyBlock(mat)) {
                // Uncertain bounding-box, allow passing through.
                long flags = BlockFlags.F_IGN_PASSABLE;
                if ((BlockFlags.hasAnyFlag(flags, BlockFlags.SOLID_GROUND))) {
                    // Block can be ground, so allow standing on any height.
                    flags |= BlockFlags.F_GROUND | BlockFlags.F_GROUND_HEIGHT;
                }
                /*
                 * TODO: Might have to set all blocks to ground here, rather
                 * catch flowers and the like with MaterialUtil.
                 */
                BlockFlags.addFlags(mat, flags);
                itchyBlocks.add(mat);
            }
        }
        if (!itchyBlocks.isEmpty()) {
            StaticLog.logDebug("The following blocks can not be modeled correctly: " + StringUtil.join(itchyBlocks, ", "));
        }
    }

}
