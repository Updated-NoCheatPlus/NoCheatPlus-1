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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitFence;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitGate;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitShapeModel;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitShulkerBox;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitSlab;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitStairs;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class MCAccessBukkitModern extends MCAccessBukkit {

    protected final Map<Material, BukkitShapeModel> shapeModels = new HashMap<Material, BukkitShapeModel>();

    private static final BukkitShapeModel MODEL_SLAB = new BukkitSlab();
    private static final BukkitShapeModel MODEL_STAIRS= new BukkitStairs();
    private static final BukkitShapeModel MODEL_THIN_FENCE = new BukkitFence(
            0.1375 + 0.3, 0.8625 - 0.3, 1.0);
    private static final BukkitShapeModel MODEL_THICK_FENCE = new BukkitFence(
            0.075 + 0.3, 0.925  - 0.3, 1.5);
    private static final BukkitShapeModel MODEL_GATE = new BukkitGate(
            0.075 + 0.3, 0.925  - 0.3, 1.5);
    private static final BukkitShapeModel MODEL_SHULKER_BOX = new BukkitShulkerBox(); 

    public MCAccessBukkitModern() {
        super();
        // TODO: Generic setup via Bukkit interface existence/relations, +- fetching methods.
        BlockInit.assertMaterialExists("OAK_LOG");
        BlockInit.assertMaterialExists("CAVE_AIR");
    }

    @Override
    public String getMCVersion() {
        return "1.13|?";
    }

    @Override
    public BlockCache getBlockCache() {
        return new BlockCacheBukkitModern(shapeModels);
    }

    @Override
    public void setupBlockProperties(final WorldConfigProvider<?> worldConfigProvider) {

        // Directly keep blocks as is.
        for (final Material mat : new Material[] {
                BridgeMaterial.MOVING_PISTON,
                Material.SNOW
        }) {
            processedBlocks.add(mat);
        }

        // TODO: Also consider removing flags (passable_x4 etc).

        // Thin fence: Glass panes, iron bars.
        for (final Material mat : MaterialUtil.addBlocks(
                MaterialUtil.GLASS_PANES, BridgeMaterial.IRON_BARS)) {
            processedBlocks.add(mat);
            shapeModels.put(mat, MODEL_THIN_FENCE);
        }

        // Slabs
        for (final Material mat : MaterialUtil.SLABS) {
            processedBlocks.add(mat);
            shapeModels.put(mat, MODEL_SLAB);
        }

        // Shulker boxes.
        for (final Material mat : MaterialUtil.SHULKER_BOXES) {
            processedBlocks.add(mat);
            shapeModels.put(mat, MODEL_SHULKER_BOX);
        }

        // Sort to processed by flags.
        for (final Material mat : Material.values()) {
            final long flags = BlockProperties.getBlockFlags(mat);
            // Stairs.
            if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_STAIRS)) {
                processedBlocks.add(mat);
                shapeModels.put(mat, MODEL_STAIRS);
            }
            // Fences, cobblestone wall.
            if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_THICK_FENCE)) {
                processedBlocks.add(mat);
                if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_PASSABLE_X4)) {
                    // TODO: Perhaps another model flag.
                    shapeModels.put(mat, MODEL_GATE);
                }
                else {
                    shapeModels.put(mat, MODEL_THICK_FENCE);
                }
            }
            // ... (chests, snow, heads, static)
        }

        super.setupBlockProperties(worldConfigProvider);
    }



}