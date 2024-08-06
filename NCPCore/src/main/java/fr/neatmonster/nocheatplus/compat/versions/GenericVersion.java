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
package fr.neatmonster.nocheatplus.compat.versions;

import java.util.Arrays;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.players.DataManager;

/**
 * Some generic version parsing and comparison utility methods.
 * 
 * @author asofold
 *
 */
public class GenericVersion {

    /**
     * The unknown version, meant to work with '=='. All internal version
     * parsing will return this for unknown versions.
     */
    public static final String UNKNOWN_VERSION = "unknown";

    /**
     * Access method meant to stay, test vs. UNKNOWN_VERSION with equals.
     * 
     * @param version
     * @return
     */
    public static boolean isVersionUnknown(String version) {
        return UNKNOWN_VERSION.equals(version);
    }

    /**
     * Split a version tag consisting of integers and dots between the numbers
     * into an int array. Not fail safe, may throw NumberFormatException.
     * 
     * @param version
     * @return
     */
    public static int[] versionToInt(String version) {
        String[] split = version.split("\\.");
        int[] num = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            num[i] = Integer.parseInt(split[i]);
        }
        return num;
    }

    /**
     * Truncate to the specified number of divisions if necessary.
     * 
     * @param version
     *            A standard version that can be converted via versionToInt.
     * @param maxDivisions
     *            Number of divisions, separated by dots.
     * @return A version with maximally the specified number of divisions. If no
     *         truncating is necessary the given version is returned.
     * @throws NumberFormatException
     *             If parsing fails.
     */
    public static String truncateVersion(String version, int maxDivisions) {
        int[] oldInts = versionToInt(version);
        if (oldInts.length <= maxDivisions) {
            return version;
        }
        int[] newInts = Arrays.copyOf(oldInts, maxDivisions);
        StringBuilder builder = new StringBuilder(version.length());
        builder.append(Integer.toString(newInts[0]));
        for (int i = 1; i < newInts.length; i++) {
            builder.append(".");
            builder.append(Integer.toString(newInts[i]));
        }
        return builder.toString();
    }

    /**
     * Pad with zeros, if the version has less than minDivisions divisions.
     * 
     * @param version
     *            A standard version that can be converted via versionToInt.
     * @param minDivisions
     *            The number of divisions to at least have.
     * @return A version with at least the specified number of divisions. If no
     *         padding is necessary the given version is returned.
     * @throws NumberFormatException
     *             If parsing fails.
     */
    public static String padVersion(String version, int minDivisions) {
        int oldSize = getVersionDivisions(version);
        if (oldSize >= minDivisions) {
            return version;
        }
        StringBuilder builder = new StringBuilder(version.length() + minDivisions * 4);
        builder.append(version);
        for (int i = oldSize; i < minDivisions; i++) {
            builder.append(".0");
        }
        return builder.toString();
    }

    /**
     * Ensure the returned version has exactly the specified number of
     * divisions. If necessary, either truncating or padding with zeros will be
     * applied.
     * 
     * @param version
     * @param divisions
     * @return A version with exactly the specified number of divisions. If the
     *         given version already has that amount of divisions, it will be
     *         returned as is.
     * @throws NumberFormatException
     *             If parsing fails.
     */
    public static String ensureVersionDivisions(String version, int divisions) {
        return truncateVersion(padVersion(version, divisions), divisions);
    }

    /**
     * Get versions padded to the maximum number of divisions found, using with
     * zeros.
     * 
     * @param versions
     * @return An new array with versions padded to the maximum number of
     *         divisions found, where necessary. Versions that have the maximum
     *         number of divisions are returned as is. The array has the same
     *         order as the input.
     */
    public static String[] padVersions(String... versions) {
        int maxDivisions = 0;
        for (int i = 0; i < versions.length; i++) {
            maxDivisions = Math.max(maxDivisions, getVersionDivisions(versions[i]));
        }
        String[] out = new String[versions.length];
        for (int i = 0; i < versions.length; i++) {
            out[i] = ensureVersionDivisions(versions[i], maxDivisions);
        }
        return out;
    }

    /**
     * Get the number of divisions for a version.
     * @param version
     * @return
     * @throws NumberFormatException
     *             If parsing fails.
     */
    public static int getVersionDivisions(String version) {
        return versionToInt(version).length;
    }

    /**
     * Simple x.y.z versions. Returns 0 on equality, -1 if version 1 is smaller
     * than version 2, 1 if version 1 is greater than version 2.
     * 
     * @param version1
     *            Can be unknown.
     * @param version2
     *            Must not be unknown.
     * @return
     */
    public static int compareVersions(String version1, String version2) {
        if (version2.equals(UNKNOWN_VERSION)) {
            throw new IllegalArgumentException("version2 must not be 'unknown'.");
        } else if (version1.equals(UNKNOWN_VERSION)) {
            // Assume smaller than any given version.
            return -1;
        }
        if (version1.equals(version2)) {
            return 0;
        }
        try {
            int[] v1Int = versionToInt(version1);
            int[] v2Int = versionToInt(version2);
            for (int i = 0; i < Math.min(v1Int.length, v2Int.length); i++) {
                if (v1Int[i] < v2Int[i]) {
                    return -1;
                } else if (v1Int[i] > v2Int[i]) {
                    return 1;
                }
            }
            // Support sub-sub-sub-...-....-marines.
            if (v1Int.length < v2Int.length) {
                return -1;
            }
            else if (v1Int.length > v2Int.length) {
                return 1;
            }
        } catch (NumberFormatException e) {}

        // Equality was tested above, so it would seem.
        throw new IllegalArgumentException("Bad version input.");
    }

    /**
     * Exact case, no trim.
     * 
     * @param input
     * @param prefix
     * @param suffix
     * @return
     */
    public static String parseVersionDelimiters(String input, String prefix, String suffix) {
        int preIndex = prefix.isEmpty() ? 0 : input.indexOf(prefix);
        if (preIndex != -1) {
            String candidate = input.substring(preIndex + prefix.length());
            int postIndex = suffix.isEmpty() ? candidate.length() : candidate.indexOf(suffix);
            if (postIndex != -1) {
                return GenericVersion.collectVersion(candidate.substring(0, postIndex).trim(), 0);
            }
        }
        return null;
    }

    /**
     * Collect a version of the type X.Y.Z with X, Y, Z being numbers. Demands
     * at least one number, but allows an arbitrary amount of sections X....Y.
     * Rigid character check, probably not fit for snapshots.
     * 
     * @param input
     * @param beginIndex
     * @return null if not successful.
     */
    public static String collectVersion(String input, int beginIndex) {
        StringBuilder buffer = new StringBuilder(128);
        // Rigid scan by character.
        boolean numberFound = false;
        char[] chars = input.toCharArray();
        for (int i = beginIndex; i < input.length(); i++) {
            char c = chars[i];
            if (c == '.') {
                if (numberFound) {
                    // Reset, expecting a number to follow.
                    numberFound = false;
                } else {
                    //  Failure.
                    return null;
                }
            } else if (!Character.isDigit(c)) {
                // Failure.
                return null;
            } else {
                numberFound = true;
            }
            buffer.append(c);
        }
        if (numberFound) {
            return buffer.toString();
        } else {
            return null;
        }
    }

    /**
     * Test if the given version is between the two given ones.
     * 
     * @param version
     *            The version to compare with the given bounds.
     * @param versionLow
     *            Can not be GenericVersion.UNKNOWN_VERSION.
     * @param includeLow
     *            If to allow equality for the low edge.
     * @param versionHigh
     *            Can not be GenericVersion.UNKNOWN_VERSION.
     * @param includeHigh
     *            If to allow equality for the high edge.
     * @return
     */
    public static boolean isVersionBetween(String version, String versionLow, boolean includeLow, String versionHigh, boolean includeHigh) {
        if (includeLow) {
            if (GenericVersion.compareVersions(version, versionLow) == -1) {
                return false;
            }
        } else {
            if (GenericVersion.compareVersions(version, versionLow) <= 0) {
                return false;
            }
        }
        if (includeHigh) {
            if (GenericVersion.compareVersions(version, versionHigh) == 1) {
                return false;
            }
        } else {
            if (GenericVersion.compareVersions(version, versionHigh) >= 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Retrieves the Minecraft version associated with the given entity.
     * <p>
     * If the entity is a player, the method returns the client's Minecraft version.
     * For all other entities, the server's Minecraft version is returned.
     * </p>
     *
     * @param entity The entity whose Minecraft version is to be determined
     * @return The Minecraft version as a string; the client's version if the entity is a player,
     *         otherwise the server's version
     */
    public static String getEntityVersion(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            return DataManager.getPlayerData(player).getClientVersion().getReleaseName();
        }
        // Not a Player. In which the version is determined by the server the entity is on.
        return ServerVersion.getMinecraftVersion();
    }
    
    public static int compareEntityVersion(Entity entity, String targetVersion) {
        return compareVersions(getEntityVersion(entity), targetVersion);
    }
    
    /** Convenience method (no equality check). Delegate to compareEntityVersion(Entity entity, String targetVersion) */
    public static boolean isLowerThan(Entity entity, String version) {
        return compareEntityVersion(entity, version) == -1;
    }
    
    /** Convenience method (no equality check). Delegate to compareEntityVersion(Entity entity, String targetVersion) */
    public static boolean isHigherThan(Entity entity, String version) {
        return compareEntityVersion(entity, version) == 1;
    }
    
    /** Convenience method. Delegate to compareEntityVersion(Entity entity, String targetVersion) */
    public static boolean isAtLeast(Entity entity, String version) {
        return compareEntityVersion(entity, version) >= 0;
    }
    
    /** Convenience method. Delegate to compareEntityVersion(Entity entity, String targetVersion) */
    public static boolean isAtMost(Entity entity, String version){
        return compareEntityVersion(entity, version) <= 0;
    }
}
