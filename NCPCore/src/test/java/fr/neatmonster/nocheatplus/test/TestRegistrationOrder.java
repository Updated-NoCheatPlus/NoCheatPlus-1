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
package fr.neatmonster.nocheatplus.test;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import fr.neatmonster.nocheatplus.components.registry.order.IGetRegistrationOrder;
import fr.neatmonster.nocheatplus.components.registry.order.RegistrationOrder;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;

public class TestRegistrationOrder {

    public static interface IAccessSort<F> {
        public LinkedList<F> getSortedLinkedList(Collection<F> items);
        public RegistrationOrder getRegistrationOrder(F item);
    }

    public static final IAccessSort<RegistrationOrder> accessRegistrationOrder = new IAccessSort<RegistrationOrder>() {
        @Override
        public LinkedList<RegistrationOrder> getSortedLinkedList(Collection<RegistrationOrder> items) {
            return RegistrationOrder.sortRegistrationOrder.getSortedLinkedList(items);
        }
        @Override
        public RegistrationOrder getRegistrationOrder(RegistrationOrder item) {
            return item;
        }
    };

    public static final class AccessIGetRegistrationOrder implements IAccessSort<IGetRegistrationOrder> {
        @Override
        public LinkedList<IGetRegistrationOrder> getSortedLinkedList(Collection<IGetRegistrationOrder> items) {
            return RegistrationOrder.sortIGetRegistrationOrder.getSortedLinkedList(items);
        }
        @Override
        public RegistrationOrder getRegistrationOrder(IGetRegistrationOrder item) {
            return item.getRegistrationOrder();
        }
    }

    public static final IAccessSort<IGetRegistrationOrder> accessIGetRegistrationOrder = new AccessIGetRegistrationOrder();

    private static String[] tags = new String[]{null, "foo", "bar", "low", "high", "ledge", "bravo", "bravissimo"};

    @Test
    public void testRegistrationOrder() {

        int repeatShuffle = 15;
        int rand1samples = BuildParameters.testLevel > 0 ? 500 : 50;
        final List<List<RegistrationOrder>> samples = new ArrayList<List<RegistrationOrder>>();
        // TODO: Hand crafted, random, part-random (defined tags, random order).
        for (int i = 0; i < rand1samples; i++) {
            samples.add(getSample(5, 12)); // Random-ish tests to have something in place.
        }

        // Iterate ...
        for (List<RegistrationOrder> sample : samples) {
            List<IGetRegistrationOrder> sample2 = new ArrayList<IGetRegistrationOrder>();
            for (final RegistrationOrder order : sample) {
                sample2.add(new IGetRegistrationOrder() {
                    @Override
                    public RegistrationOrder getRegistrationOrder() {
                        return order;
                    }

                });
            }

            // Test sorting each.
            testSorting(sample, accessRegistrationOrder, repeatShuffle);
            testSorting(sample2, accessIGetRegistrationOrder, repeatShuffle);

            // Test equality.
            testEquality(sample, accessRegistrationOrder, sample2, accessIGetRegistrationOrder, repeatShuffle);

        }
        // TODO: Create RegistrationOrder and IGetRegistrationOrder inputs equally.
        // TODO: Run sorting tests per input, and testEquality.
    }

    /**
     * Run sorting test with the given items (!). Inputs are copied to an ArrayList.
     * 
     * @param items
     * @param fetcher
     */
    private <F> void testSorting(List<F> items, IAccessSort<F> fetcher, int repeatShuffle) {
        // (1) Sorting can't be stable / result in the same in general.
        List<F> originalItems = items;
        items = new ArrayList<F>(items);
        LinkedList<F> sorted;
        for (int i = 0; i < 1 + repeatShuffle; i++) {
            sorted = fetcher.getSortedLinkedList(items);
            testIfContained(originalItems, sorted);
            testIfSorted(sorted, fetcher);
            //testEquality(sorted, fetcher, fetcher.getSortedLinkedList(originalItems), fetcher); // (1)
            Collections.reverse(items);
            // testEquality(sorted, fetcher, fetcher.getSortedLinkedList(items), fetcher); // (1)
            sorted = fetcher.getSortedLinkedList(items);
            testIfContained(originalItems, sorted);
            testIfSorted(sorted, fetcher);
            //testEquality(sorted, fetcher, fetcher.getSortedLinkedList(originalItems), fetcher); // (1)
            // Finally shuffle.
            shuffle2s(items);
        }
    }

    private <F> void testIfSorted(List<F> items, IAccessSort<F> fetcher) {
        // Test priority ordering and rough region order.
        Integer lastPriority = null;
        Integer maxPriority = null;
        boolean priorityContained = false;
        boolean nullShouldFollow = false;
        for (final F item : items) {
            RegistrationOrder order = fetcher.getRegistrationOrder(item);
            Integer basePriority = order.getBasePriority();
            if (basePriority == null) {
                if (maxPriority == null) {
                    if (order.getBeforeTag() == null && order.getAfterTag() != null) {
                        nullShouldFollow = true;
                        // Assume the pair-comparison would catch wrongly sorted null entries on occasion.
                    }
                }
                else {
                    if (nullShouldFollow && order.getBeforeTag() != null) {
                        fail("Invalid null-basePriority entry after basePriority>0 region: beforeTag is set.");
                    }
                    // 
                    if (maxPriority > 0) {
                        nullShouldFollow = true;
                    }
                    else if (maxPriority == 0) {
                        if (order.getBeforeTag() != null) {
                            fail("Invalid null-basePriority entry within 0-basePriority region: beforeTag is set.");
                        }
                    }
                }
            }
            else {
                if (nullShouldFollow) {
                    fail("Invalid mixture of priority null/set after 0-basePriority region.");
                }
                else if (priorityContained && basePriority < 0 && lastPriority == null) {
                    fail("Invalid mixture of priority null/set before 0-basePriority region.");
                }
                if (maxPriority == null) {
                    maxPriority = basePriority;
                }
                else if (basePriority < maxPriority) {
                    fail("Order by priority broken.");
                }
                else {
                    maxPriority = basePriority;
                }
                priorityContained = true; // Remember to be able to exclude cases.
            }
            lastPriority = basePriority;
        }
        // Compare pairs, for being wrongly ordered.
        for (int i = 1; i <items.size(); i++) {
            // (Careful testing: greedy could apply both ways round.)
            if (!RegistrationOrder.AbstractRegistrationOrderSort.shouldSortBefore(
                    fetcher.getRegistrationOrder(items.get(i - 1)), 
                    fetcher.getRegistrationOrder(items.get(i)))) {
                // Still test the other way round, to see if it's really demanded.
                if (RegistrationOrder.AbstractRegistrationOrderSort.shouldSortBefore(
                        fetcher.getRegistrationOrder(items.get(i)), 
                        fetcher.getRegistrationOrder(items.get(i - 1)))) {
                    // The following object is explicitly set to come before.
                    fail("Pair is wrongly ordered.");
                }
                // TODO: Could still consider checking i-1 versus following items, until explicit stop/end.
            }
        }
    }

    private <F> void testIfContained(List<F> originalItems, List<F> items) {
        if (!new HashSet<F>(items).containsAll(originalItems)) {
            fail("Sorted list does not contain all original items.");
        }
    }

    /**
     * Inputs are copied to an ArrayList.
     * 
     * @param items1
     * @param fetcher1
     * @param items2
     * @param fetcher2
     */
    private <F1, F2> void testEquality(List<F1> items1, IAccessSort<F1> fetcher1, 
            List<F2> items2, IAccessSort<F2> fetcher2, 
            int repeatShuffle) {
        // Test if sorting remains the same: original, and n times shuffled - input + reversed.
        items1 = new ArrayList<F1>(items1);
        items2 = new ArrayList<F2>(items2);
        List<F1> sorted1;
        List<F2> sorted2;
        for (int i = 0; i < repeatShuffle + 1; i++) {
            sorted1 = fetcher1.getSortedLinkedList(items1);
            sorted2 = fetcher2.getSortedLinkedList(items2);
            testEquality(sorted1, fetcher1, sorted2, fetcher2);
            Collections.reverse(items1);
            Collections.reverse(items2);
            sorted1 = fetcher1.getSortedLinkedList(items1);
            sorted2 = fetcher2.getSortedLinkedList(items2);
            testEquality(sorted1, fetcher1, sorted2, fetcher2);
            // In the end "shuffle" both inputs parallel.
            int[][] swap = getSwapIndices(items1.size(), items1.size() * 2);
            shuffle(items1, swap);
            shuffle(items2, swap);
        }
    }

    /**
     * Create a somewhat regular randomized sample, having itemsPerLevel times
     * items per priority level plus, plus a similar amount of null entries.<br>
     * TODO: A version that guarantees every type is in?
     * 
     * @param priorities
     * @param itemsPerLevel
     * @return
     */
    private List<RegistrationOrder> getSample(int priorities, int itemsPerLevel) {
        ArrayList<RegistrationOrder> out = new ArrayList<RegistrationOrder>();
        int[] prioArr;
        if (priorities <= 0) {
            prioArr = new int[0];
        }
        else {
            Set<Integer> prios = new LinkedHashSet<Integer>();
            if (ThreadLocalRandom.current().nextBoolean()) {
                prios.add(0);
            }
            while (prios.size() < priorities) {
                prios.add(ThreadLocalRandom.current().nextInt(2 * priorities + 1) - priorities);
            }
            prioArr = new int[priorities];
            int index = 0;
            for (Integer x : prios) {
                prioArr[index] = x;
                index ++;
            }
        }
        for (int i = 0; i < prioArr.length; i++) {
            for (int x = 0; x < itemsPerLevel; x++) {
                out.add(getRegistrationOrder(prioArr[i]));
            }
        }
        for (int i = 0; i < itemsPerLevel; i++) {
            out.add(getRegistrationOrder(null));
        }
        shuffle2s(out); // Extra shuffle to potentially unlink.
        return out;
    }

    /**
     * Get one using the default tags, beforeTag and afterTag get set to null, 1 2 or 3 others.
     * @param basePriority
     * @return
     */
    private RegistrationOrder getRegistrationOrder(Integer basePriority) {
        String tag = tags[ThreadLocalRandom.current().nextInt(tags.length)];
        String afterTag = ThreadLocalRandom.current().nextBoolean() ? null : getTagRegex(ThreadLocalRandom.current().nextInt(3) + 1);
        String beforeTag = ThreadLocalRandom.current().nextBoolean() ? null : getTagRegex(ThreadLocalRandom.current().nextInt(3) + 1);
        // StaticLog.logInfo("RegistrationOrder(b " + basePriority + " t " + tag + " bt " + beforeTag + " at " + afterTag +  ")");
        return new RegistrationOrder(basePriority, tag, beforeTag, afterTag);
    }

    /**
     * Get a non null regex using default tags.
     * @param combinations
     */
    private String getTagRegex(int combinations) {
        Set<String> indices = new HashSet<String>();
        while (indices.size() < combinations) {
            indices.add(tags[1 + ThreadLocalRandom.current().nextInt(tags.length - 1)]); // Avoid null here.
        }
        // Combine tags to a regex (simple).
        return"(" + StringUtil.join(indices, "|") + ")";
    }

    private <F1, F2> void testEquality(List<F1> items1, IAccessSort<F1> fetcher1, 
            List<F2> items2, IAccessSort<F2> fetcher2) {
        for (int i = 0; i < items1.size(); i++) {
            if (!fetcher1.getRegistrationOrder(items1.get(i)).equals(fetcher2.getRegistrationOrder(items2.get(i)))) {
                // TODO: Return / log outside to give context.
                doFail("Lists are not the same");
            }
        }
    }

    /**
     * Shuffle by swapping items according to the give indices.
     * 
     * @param items
     * @param swap
     *            int[N][2], containing valid indices for items.
     */
    private <F> void shuffle(List<F> items, int[][] swap) {
        for (int x = 0; x < swap.length; x++) {
            F temp1 = items.get(swap[x][1]);
            items.set(swap[x][1], items.get(swap[x][0]));
            items.set(swap[x][0], temp1);
        }
    }

    /**
     * Shuffle by swapping 2 times the list size items.
     * 
     * @param items
     */
    private <F> void shuffle2s(List<F> items) {
        shuffle(items, 2 * items.size());
    }

    private <F> void shuffle(List<F> items, int n) {
        int[][] swap = getSwapIndices(items.size(), n);
        for (int x = 0; x < swap.length; x++) {
            F temp1 = items.get(swap[x][1]);
            items.set(swap[x][1], items.get(swap[x][0]));
            items.set(swap[x][0], temp1);
        }
    }

    /**
     * Used for "shuffling".
     * @param upperBound Excluded.
     * @param n
     * @return
     */
    private int[][] getSwapIndices(int upperBound, int n) {
        int[][] out = new int[n][2];
        for (int i = 0; i < n; i++) {
            out[i][0] = ThreadLocalRandom.current().nextInt(upperBound);
            out[i][1] = ThreadLocalRandom.current().nextInt(upperBound);
        }
        return out;
    }

    private void doFail(String message) {
        // TODO: Args.
        fail(message);
    }

}
