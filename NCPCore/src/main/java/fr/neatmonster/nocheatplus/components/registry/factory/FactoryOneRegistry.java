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
package fr.neatmonster.nocheatplus.components.registry.factory;

import java.util.concurrent.locks.Lock;

import fr.neatmonster.nocheatplus.components.concurrent.IPrimaryThreadContextTester;
import fr.neatmonster.nocheatplus.utilities.ds.map.HashMapLOW;

/**
 * Register IFactoryOne instances for object creation.
 * <hr>
 * Instance fetching from factories is done under lock. If instance creation is
 * thread-safe, naturally depends on the underlying factory implementations.
 * 
 * @author asofold
 *
 * @param <A>
 *            Argument type for IFactoryOne instances.
 */
public class FactoryOneRegistry<A> implements IFactoryOneRegistry<A> {

    private final Lock lock;
    private final IPrimaryThreadContextTester primaryThreadContextTester;
    private final HashMapLOW<Class<?>, IFactoryOne<A, ?>> factories;


    public FactoryOneRegistry(Lock lock, IPrimaryThreadContextTester primaryThreadContextTester) {
        this.lock = lock;
        this.primaryThreadContextTester = primaryThreadContextTester;
        // TODO: RegisteredItemStore can't do classes -> need RegisteredClassStore (...).
        factories = new HashMapLOW<Class<?>, IFactoryOne<A,?>>(lock, 30);
    }

    @Override
    public <T> void registerFactory(final Class<T> registerFor, 
            final IFactoryOne<A, T> factory) {
        if (!primaryThreadContextTester.isPrimaryThread()) {
            outsideThreadContext("register factory");
        }
        lock.lock();
        factories.put(registerFor, factory);
        lock.unlock();
    }

    private void outsideThreadContext(final String tag) {
        throw new IllegalStateException("Can't call off the primary thread context: " + tag);
    }

    @Override
    public <T> T getNewInstance(final Class<T> registeredFor, final A arg) {
        @SuppressWarnings("unchecked")
        final IFactoryOne<A, T> factory = (IFactoryOne<A, T>) factories.get(registeredFor);
        if (factory == null) {
            return null;
        }
        else {
            //lock.lock();
            T instance;
            try {
                instance = factory.getNewInstance(arg);
            }
            catch (Exception e) {
                // TODO: Exception type to throw.
                throw new RuntimeException(e);
            }
            //finally {
            //    lock.unlock();
            //}
            return instance;
        }
    }

}
