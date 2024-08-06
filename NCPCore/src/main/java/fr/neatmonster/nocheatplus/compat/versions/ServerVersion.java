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

import org.bukkit.Bukkit;
import org.bukkit.Server;

import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

/**
 * Static utility that stores the Minecraft version, providing some parsing
 * methods. This has to be initialized from an external source (e.g. a plugin
 * calling BukkitVersion.init).
 * <hr/>
 * Taken from the TrustCore plugin.
 * 
 * @author mc_dev
 *
 */
public class ServerVersion {

    private static String minecraftVersion = GenericVersion.UNKNOWN_VERSION;

    private static final String[][] versionTagsMatch = {
            {"1.7.2-r", "1.7.2"}, // Example. Probably this method will just be removed.
    };

    /**
     * Test if the set Minecraft version is unknown.
     */
    public static boolean isMinecraftVersionUnknown() {
        return GenericVersion.isVersionUnknown(minecraftVersion);
    }

    /**
     * Attempt to return the Minecraft version for a given server version
     * string.
     * 
     * @param serverVersion
     *            As returned by Bukkit.getServer().getVersion();
     * @return null if not known/parsable.
     */
    public static String parseMinecraftVersion(String... versionCandidates) {
        for (String serverVersion : versionCandidates) {
            serverVersion = serverVersion.trim();
            for (String minecraftVersion : new String[]{
                    GenericVersion.collectVersion(serverVersion, 0),
                    parseMinecraftVersionGeneric(serverVersion),
                    parseMinecraftVersionTokens(serverVersion)
            }) {
                // Validate if not null, might mean double validation.
                if (minecraftVersion != null && validateMinecraftVersion(minecraftVersion)) {
                    return minecraftVersion;
                } 
            }
        }
        return null;
    }

    public static Object getNMSMinecraftServer() {
        final Server server = Bukkit.getServer();
        try {
            final Object obj = ReflectionUtil.invokeMethodNoArgs(server, "getServer");
            return obj;
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static String fetchNMSMinecraftServerVersion() {
        final Object server = getNMSMinecraftServer();
        if (server == null) {
            return null;
        }
        try {
            return (String) ReflectionUtil.invokeMethodNoArgs(server, "getVersion", String.class);
        }
        catch (Throwable t) {
            return null;
        }
    }

    /**
     * Simple consistency check.
     * 
     * @param minecraftVersion
     * @return
     */
    private static boolean validateMinecraftVersion(String minecraftVersion) {
        return GenericVersion.collectVersion(minecraftVersion, 0) != null;
    }

    /**
     * Match directly versus hard coded examples. Not for direct use.
     * 
     * @param serverVersion
     * @return
     */
    private static String parseMinecraftVersionTokens(String serverVersion) {
        serverVersion = serverVersion.trim().toLowerCase();
        for (String[] entry : versionTagsMatch) {
            if (serverVersion.contains(entry[0])) {
                return entry[1];
            }
        }
        return null;
    }

    /**
     * 
     * @param serverVersion
     * @return
     */
    private static String parseMinecraftVersionGeneric(String serverVersion) {
        String lcServerVersion = serverVersion.trim().toLowerCase();
        for (String candidate : new String[] {
                GenericVersion.parseVersionDelimiters(lcServerVersion, "(mc:", ")"),
                GenericVersion.parseVersionDelimiters(lcServerVersion, "(mc:", "-pre"),
                GenericVersion.parseVersionDelimiters(lcServerVersion, "mcpc-plus-", "-"),
                GenericVersion.parseVersionDelimiters(lcServerVersion, "git-bukkit-", "-r"),
                GenericVersion.parseVersionDelimiters(lcServerVersion, "", "-r"),
                // TODO: Other server mods + custom builds !?.
        }) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Sets lower-case Minecraft version - or GenericVersion.UNKNOWN_VERSION, if
     * null or empty.
     * 
     * @param version
     *            Passing null will yield GenericVersion.UNKNOWN_VERSION. If the
     *            input equals GenericVersion.UNKNOWN_VERSION,
     *            GenericVersion.UNKNOWN_VERSION is set.
     * @return The String that minecraftVersion has been set to.
     */
    public static String setMinecraftVersion(String version) {
        if (version == null) {
            minecraftVersion = GenericVersion.UNKNOWN_VERSION;
        } else {
            version = version.trim().toLowerCase();
            if (version.isEmpty() || version.equals(GenericVersion.UNKNOWN_VERSION)) {
                minecraftVersion = GenericVersion.UNKNOWN_VERSION;
            } else {
                minecraftVersion = version;
            }
        }
        return minecraftVersion;
    }

    /**
     * 
     * @return Some version string, or UNKNOWN_VERSION.
     */
    public static String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Convenience for compareVersions(getMinecraftVersion(), version).
     * 
     * @param version
     *            Can not be GenericVersion.UNKNOWN_VERSION.
     * @return 0 if equal, -1 if the Minecraft version is lower, 1 if the
     *         Minecraft version is higher.
     */
    public static int compareMinecraftVersion(String toVersion) {
        return GenericVersion.compareVersions(getMinecraftVersion(), toVersion);
    }
    
    /** Convenience method (no equality check). Delegate to compareMinecraftVersion(String toVersion) */
    public static boolean isLowerThan(String version) {
        return compareMinecraftVersion(version) == -1;
    }
    
    /** Convenience method (no equality check). Delegate to compareMinecraftVersion(String toVersion) */
    public static boolean isHigherThan(String version) {
        return compareMinecraftVersion(version) == 1;
    }
    
    /** Convenience method. Delegate to compareMinecraftVersion(String toVersion) */
    public static boolean isAtLeast(String version) {
        return compareMinecraftVersion(version) >= 0;
    }
    
    /** Convenience method. Delegate to compareMinecraftVersion(String toVersion) */
    public static boolean isAtMost(String version){
        return compareMinecraftVersion(version) <= 0;
    }

    /**
     * Test if the Minecraft version is between the two given ones.
     * 
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
    public static boolean isMinecraftVersionBetween(String versionLow, boolean includeLow, String versionHigh, boolean includeHigh) {
        return GenericVersion.isVersionBetween(getMinecraftVersion(), versionLow, includeLow, versionHigh, includeHigh);
    }

    /**
     * Select a value based on the Minecraft version.
     * 
     * @param cmpVersion
     *            Version to compare to, comparison of server version vs. given
     *            version.
     * @param valueLT
     *            Server has an earlier version.
     * @param valueEQ
     *            Same versions.
     * @param valueGT
     *            The server version is later.
     * @param valueUnknown
     *            Value to return, if the server version could not be
     *            determined.
     * @return
     */
    public static <V> V select(final String cmpVersion, final V valueLT, final V valueEQ, final V valueGT, final V valueUnknown) {
        final String mcVersion = ServerVersion.getMinecraftVersion();
        if (mcVersion == GenericVersion.UNKNOWN_VERSION) {
            return valueUnknown;
        } else {
            final int cmp = GenericVersion.compareVersions(mcVersion, cmpVersion);
            if (cmp == 0) {
                return valueEQ;
            } else if (cmp < 0) {
                return valueLT;
            } else {
                return valueGT;
            }
        }
    }

}
