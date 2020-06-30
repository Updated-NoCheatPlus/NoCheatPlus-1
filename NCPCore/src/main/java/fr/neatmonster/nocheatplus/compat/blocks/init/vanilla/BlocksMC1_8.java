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

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.BlockProps;

@SuppressWarnings("deprecation")
public class BlocksMC1_8 implements BlockPropertiesSetup {

    public BlocksMC1_8() {
        //BlockInit.assertMaterialExists("BARRIER");
        BlockInit.assertMaterialExists("SLIME_BLOCK");
        BlockInit.assertMaterialExists("ACACIA_FENCE_GATE");
        //BlockInit.assertMaterialExists("STANDING_BANNER");
        BlockInit.assertMaterialExists("SEA_LANTERN");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {

        // ---- Changed block break timings ----

        // Melon/pumpkin/like.
        BlockProps props = new BlockProps(BlockProperties.woodAxe, 1f, BlockProperties.secToMs(1.45, 0.70, 0.325, 0.2, 0.13, 0.075, 0.05), 3f);
        for (Material mat : new Material[] {
                BridgeMaterial.MELON,
                Material.PUMPKIN,
                Material.JACK_O_LANTERN,
                // Same core breaking times, but behave different on efficiency + other tool (?): 
                BridgeMaterial.SIGN,
        }) {
            BlockProperties.setBlockProps(mat, props);
        }
        
        // Ladder.
        props = new BlockProps(BlockProperties.woodAxe, 0.4f, BlockProperties.secToMs(0.6, 0.3, 0.15, 0.1, 0.075, 0.05, 0.05));
        BlockProperties.setBlockProps(Material.LADDER, props);

        // ---- New blocks ----

        // 165(SLIME_BLOCK
        BlockInit.setAs("SLIME_BLOCK", Material.TNT); // Full block, instant break.
        // Add the bouncing flag.
        BlockFlags.addFlags("SLIME_BLOCK", BlockProperties.F_BOUNCE25);

        // 166(BARRIER
        BlockInit.setAs("BARRIER", Material.BEDROCK); // Full block, unbreakable.

        // 167(IRON_TRAP_DOOR
        BlockFlags.setFlagsAs("IRON_TRAPDOOR", BridgeMaterial.OAK_TRAPDOOR);
        BlockInit.setPropsAs("IRON_TRAPDOOR", BridgeMaterial.IRON_DOOR);

        // 168(PRISMARINE
        BlockInit.setAs("PRISMARINE", Material.STONE);

        // 169(SEA_LANTERN
        BlockInit.setAs("SEA_LANTERN", 
                BridgeMaterial.getFirstNotNull("redstone_lamp", "redstone_lamp_off"));

        // 176(STANDING_BANNER
        // 177(WALL_BANNER

        // 178(DAYLIGHT_DETECTOR_INVERTED
        BlockInit.setAsIfExists("DAYLIGHT_DETECTOR_INVERTED", Material.DAYLIGHT_DETECTOR);

        // 179(RED_SANDSTONE
        BlockInit.setAs("RED_SANDSTONE", Material.SANDSTONE);

        // 180(RED_SANDSTONE_STAIRS
        BlockInit.setAs("RED_SANDSTONE_STAIRS", Material.SANDSTONE_STAIRS);

        // 181(DOUBLE_STEP_2
        BlockInit.setAsIfExists("DOUBLE_STONE_SLAB2", BridgeMaterial.get("double_step")); // TODO: red sandstone / prismarine ?

        // 182(STEP_2
        BlockInit.setAsIfExists("STONE_SLAB2", BridgeMaterial.STONE_SLAB); // TODO: red sandstone / prismarine ?

        //        // 183(SPRUCE_FENCE_GATE
        //        BlockInit.setAs("SPRUCE_FENCE_GATE", Material.FENCE_GATE);
        //
        //        // 184(BIRCH_FENCE_GATE
        //        BlockInit.setAs("BIRCH_FENCE_GATE", Material.FENCE_GATE);
        //
        //        // 185(JUNGLE_FENCE_GATE
        //        BlockInit.setAs("JUNGLE_FENCE_GATE", Material.FENCE_GATE);
        //
        //        // 186(DARK_OAK_FENCE_GATE
        //        BlockInit.setAs("DARK_OAK_FENCE_GATE", Material.FENCE_GATE);
        //
        //        // 187(ACACIA_FENCE_GATE
        //        BlockInit.setAs("ACACIA_FENCE_GATE", Material.FENCE_GATE);
        //
        //        // 188(SPRUCE_FENCE
        //        BlockInit.setAs("SPRUCE_FENCE", Material.FENCE);
        //
        //        // 189(BIRCH_FENCE
        //        BlockInit.setAs("BIRCH_FENCE", Material.FENCE);
        //
        //        // 190(JUNGLE_FENCE
        //        BlockInit.setAs("JUNGLE_FENCE", Material.FENCE);
        //
        //        // 191(DARK_OAK_FENCE
        //        BlockInit.setAs("DARK_OAK_FENCE", Material.FENCE);
        //
        //        // 192(ACACIA_FENCE
        //        BlockInit.setAs("ACACIA_FENCE", Material.FENCE);
        //
        //        // 193(SPRUCE_DOOR
        //        BlockInit.setAs("SPRUCE_DOOR", Material.WOODEN_DOOR);
        //
        //        // 194(BIRCH_DOOR
        //        BlockInit.setAs("BIRCH_DOOR", Material.WOODEN_DOOR);
        //
        //        // 195(JUNGLE_DOOR
        //        BlockInit.setAs("JUNGLE_DOOR", Material.WOODEN_DOOR);
        //
        //        // 196(ACACIA_DOOR
        //        BlockInit.setAs("ACACIA_DOOR", Material.WOODEN_DOOR);
        //
        //        // 197(DARK_OAK_DOOR
        //        BlockInit.setAs("DARK_OAK_DOOR", Material.WOODEN_DOOR);

        StaticLog.logInfo("Added block-info for Minecraft 1.8 blocks.");
    }

}
