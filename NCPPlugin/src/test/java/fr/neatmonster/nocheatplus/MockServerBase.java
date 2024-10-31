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
package fr.neatmonster.nocheatplus;

import org.bukkit.*;
import org.junit.After;
import org.junit.Before;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/*
    Inspired from https://github.com/uprial/customcreatures/blob/master/src/test/java/com/gmail/uprial/customcreatures/helpers/TestServerBase.java
    Code by @Jikoo
    https://www.spigotmc.org/threads/cant-create-enchantment-in-test.653255/#post-4747855
 */
public abstract class MockServerBase {
    @Before
    public void setUp() throws Exception {
        final Server mock = mock(Server.class);
        final Logger noOp = mock(Logger.class);

        when(mock.getLogger()).thenReturn(noOp);
        // Server must be available before tags can be mocked.
        Bukkit.setServer(mock);

        // Bukkit has a lot of static constants referencing registry values. To initialize those, the
        // registries must be able to be fetched before the classes are touched.
        final Map<Class<? extends Keyed>, Object> registers = new HashMap<>();

        doAnswer(invocationGetRegistry ->
                registers.computeIfAbsent(invocationGetRegistry.getArgument(0), clazz -> {
                    final Registry<?> registry = mock(Registry.class);
                    final Map<NamespacedKey, Keyed> cache = new HashMap<>();

                    final Answer<?> answer = (invocationGetEntry) -> {
                        final NamespacedKey key = invocationGetEntry.getArgument(0);
                        // Some classes (like BlockType and ItemType) have extra generics that will be
                        // erased during runtime calls. To ensure accurate typing, grab the constant's field.
                        // This approach also allows us to return null for unsupported keys.
                        final Class<? extends Keyed> constantClazz;
                        try {
                            //noinspection unchecked
                            constantClazz = (Class<? extends Keyed>) clazz.getField(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_')).getType();
                        } catch (ClassCastException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchFieldException e) {
                            return null;
                        }

                        return cache.computeIfAbsent(key, key1 -> {
                            final Keyed keyed = mock(constantClazz);
                            doReturn(key).when(keyed).getKey();
                            return keyed;
                        });
                    };

                    doAnswer(answer).when(registry).get(notNull());

                    doAnswer(answer).when(registry).getOrThrow(notNull());

                    return registry;
                }))
                .when(mock).getRegistry(notNull());
    }

    @After
    public void unsetBukkitServer() {
        try
        {
            final Field server = Bukkit.class.getDeclaredField("server");
            server.setAccessible(true);
            server.set(null, null);
        }
        catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}
