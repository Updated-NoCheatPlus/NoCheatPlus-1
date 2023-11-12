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
package fr.neatmonster.nocheatplus.utilities.ds.count;

import fr.neatmonster.nocheatplus.utilities.StringUtil;

/**
 * This class is meant to accumulate actions similar to ActionFrequency. 
 * In contrast to ActionFrequency, this class will accumulate values grouped by number of events instead of real-time intervals.
 * @author asofold
 *
 */
public class ActionAccumulator {
    /** Counter of added events for each bucket. */
    private final int[] valueCounter;
    /** Value accumulation (sum). */
    private final float[] buckets;
    /** How many events can a bucket hold. */
    private final int bucketCapacity;
    
    /**
     * Create a new ActionAccumulator instance
     * 
     * @param nBuckets Number of buckets to hold values with
     * @param bucketCapacity How many events can a bucket hold 
     */
    public ActionAccumulator(final int nBuckets, final int bucketCapacity) {
        this.valueCounter = new int[nBuckets];
        this.buckets = new float[nBuckets];
        this.bucketCapacity = bucketCapacity;
    }
    
    /**
     * Add value to accumulation (starting to the first bucket, shifting into the other ones if present)
     * See shift()
     */
    public void add(float value) {
        if (valueCounter[0] >= bucketCapacity) {
            // First bucket is full, shift data into the next one .
            shift();
        }
        // Each time a value is added, the event counter for this bucket increases.
        valueCounter[0]++ ;
        // Accumulate the value
        buckets[0] += value;
    }
    
    /**
     * On reaching the maximum capacity of the first bucket, 
     * shift data into the next one and empty the first (which will be ready to accumulate values again).
     */
    private void shift() {
        // Start from the last bucket and decrease
        for (int bucketIndex = buckets.length - 1; bucketIndex > 0; bucketIndex--) {
            // Shift counted events to the next bucket
            valueCounter[bucketIndex] = valueCounter[bucketIndex - 1];
            // Shift accumulated values to the next bucket
            buckets[bucketIndex] = buckets[bucketIndex - 1];
        }
        // Reset event counter of the first bucket
        valueCounter[0] = 0;
        // Empty the bucket (accumulated values)
        buckets[0] = 0;
    } 

    /**
     * Get the total accumulation value of this accumulator (values of all buckets)
     */
    public float score() {
        float score = 0;
        for (int i = 0; i < buckets.length; i++) {
            score += buckets[i];
        }
        return score;
    }
    
    /**
     * Get the total events of the whole accumulator (of all buckets)
     */
    public int count() {
        int globalCounter = 0;
        for (int i = 0; i < valueCounter.length; i++) {
            globalCounter += valueCounter[i];
        }
        return globalCounter;
    }
    
    /**
     * Reset all 
     */
    public void clear() {
        for (int i = 0; i < buckets.length; i++) {
            valueCounter[i] = 0;
            buckets[i] = 0;
        }
    }
    
    /**
     * Get the events counted for this bucket
     * @param bucket
     * @return how many events have been added(counted) to the bucket
     */
    public int bucketCount(final int bucket) {
        return valueCounter[bucket];
    }
    
    /**
     * Get the accumulation value of this bucket (sum of all values added to this bucket)
     * @param bucket
     * @return accumulation value of this bucket
     */
    public float bucketScore(final int bucket) {
        return buckets[bucket];
    }
    
    /**
     * Get the mean of this bucket
     * @param bucket 
     * @return the mean of this bucket
     */
    public float mean(final int bucket) {
        return bucketScore(bucket) / bucketCount(bucket);
    }

    /**
     * Get the mean of the whole accumulator
     */
    public float mean() {
        return score() / count();
    }  
    
    /**
     * How many buckets does the ActionAccumulator instance have
     */
    public int numberOfBuckets() {
        return buckets.length;
    }
    
    /**
     * How many events(count) can each bucket of the ActionAccumulator instance have
     */
    public int bucketCapacity() {
        return bucketCapacity;
    }
    
    /**
     * Simple display of bucket contents, no class name.
     */
    public String toInformalString() {
        StringBuilder b = new StringBuilder(buckets.length * 10);
        b.append("|");
        for (int i = 0; i < buckets.length; i++){
            b.append(StringUtil.fdec3.format(buckets[i]) + "/" + valueCounter[i] + "|");
        }
        return b.toString();
    }
}