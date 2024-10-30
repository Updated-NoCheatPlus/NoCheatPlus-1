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
package fr.neatmonster.nocheatplus.compat.blocks.init;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

/**
 * Auxiliary methods for block initialization.
 * @author asofold
 *
 */
public class BlockInit {

    // TODO: Change to assert names only?, would be better with being able to feed MC names or map them as well, though.

    /**
     * Check for Material existence, throw RuntimeException if not.
     * @param id
     */
    public static void assertMaterialExists(String id) {
        if (BlockProperties.getMaterial(id) == null) {
            throw new RuntimeException("Material " + id + " does not exist.");
        }
    }

    public static Material getMaterial(String name) {
        try {
            return Material.matchMaterial(name.toUpperCase());
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Set the block breaking properties of newMat to the same as are present
     * for mat.
     * 
     * @param newMat
     * @param mat
     */
    public static void setPropsAs(Material newMat, Material mat) {
        BlockProperties.setBlockProps(newMat, BlockProperties.getBlockProps(mat));
    }

    /**
     * Set block breaking properties same as the block of the given material.
     * @param newId
     * @param mat
     */
    public static void setPropsAs(String newId, Material mat) {
        BlockProperties.setBlockProps(newId, BlockProperties.getBlockProps(mat));
    }

    /**
     * Set block breaking properties same as the block of the given id.
     * @param newId
     * @param otherId
     */
    public static void setPropsAs(String newId, String otherId) {
        BlockProperties.setBlockProps(newId, BlockProperties.getBlockProps(otherId));
    }

    public static void setAs(Material newMat, Material mat) {
        BlockFlags.setFlagsAs(newMat, mat);
        setPropsAs(newMat, mat);
    }

    /**
     * Set block breaking and shape properties same as the block of the given material.
     * @param newId
     * @param mat
     */
    public static void setAs(String newId, Material mat) {
        BlockFlags.setFlagsAs(newId, mat);
        setPropsAs(newId, mat);
    }

    /**
     * Set block breaking and shape properties same as the block of the given id.
     * @param newId
     * @param otherId
     */
    public static void setAs(String newId, String otherId) {
        BlockFlags.setFlagsAs(newId, otherId);
        setPropsAs(newId, otherId);
    }

    /**
     * Set like air, plus instantly breakable.
     * @param newId
     */
    public static void setInstantPassable(String newId) {

        BlockFlags.setBlockFlags(newId, BlockFlags.F_IGN_PASSABLE);
        BlockProperties.setBlockProps(newId, BlockProperties.instantType);
    }

    /**
     * Set like air, plus instantly breakable.
     * @param newId
     */
    public static void setInstantPassable(Material newId) {
        BlockFlags.setBlockFlags(newId, BlockFlags.F_IGN_PASSABLE);
        BlockProperties.setBlockProps(newId, BlockProperties.instantType);
    }

    public static void setAsIfExists(String newName, Material mat) {
        if (BridgeMaterial.has(newName)) {
            setAs(newName, mat);
        }
    }

    public static void setAsIfExists(String newName, String otherName) {
        if (BridgeMaterial.has(newName)) {
            setAs(newName, otherName);
        }
    }
}
