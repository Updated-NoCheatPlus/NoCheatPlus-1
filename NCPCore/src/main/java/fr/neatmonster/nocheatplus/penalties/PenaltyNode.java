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
package fr.neatmonster.nocheatplus.penalties;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Internal data representation, managing probabilities, and complex decisions
 * with multiple penalties.
 * 
 * @author asofold
 *
 */
public class PenaltyNode {

    // TODO: Might switch to float for probability or not?

    // TODO: Might add a parsing method (recursive).

    /** The probability for this node to apply. */
    public final double probability;
    /** Penalty to apply when this node applies. */
    private final IPenalty<?> penalty;
    /** Child nodes to test when this node applies. */
    private final PenaltyNode[] childNodes;
    /**
     * Indicate that the result is set with the first child node that applies.
     */
    private final boolean abortOnApply;

    /**
     * Convenience: Simple penalty that always applies with no child nodes.
     * @param penalty
     */
    public PenaltyNode(IPenalty<?> penalty) {
        this(1.0, penalty, null, false);
    }

    /**
     * Convenience: Simple penalty with no child nodes.
     * @param probability
     * @param penalty
     */
    public PenaltyNode(double probability, IPenalty<?> penalty) {
        this(probability, penalty, null, false);
    }

    /**
     * 
     * @param probability
     * @param penalty
     *            Note that child penalties are still evaluated, if penalty is
     *            not null and abortOnApply is set.
     * @param childNodes
     *            May be null. No scaling/normalizing is applied here.
     * @param abortOnApply
     *            Evaluating child nodes: abort as soon as a child node applies.
     */
    public PenaltyNode(double probability, IPenalty<?> penalty,
            Collection<PenaltyNode> childNodes, boolean abortOnApply) {
        this.probability = probability;
        this.penalty = penalty;
        this.childNodes = childNodes == null ? new PenaltyNode[0] : childNodes.toArray(new PenaltyNode[childNodes.size()]);
        this.abortOnApply = abortOnApply;
    }

    /**
     * On the spot evaluation of an applicable path, filling in all applicable
     * penalties into the results collection. This does test
     * 
     * @param results
     * @return If this node applies (, which does not necessarily mean that
     *         anything has been appended to results).
     */
    public final boolean evaluate(final IPenaltyList results) {
        // (Set final to ensure return behavior.)
        if (probability < 1.0 && ThreadLocalRandom.current().nextDouble() > probability) {
            // This node does not apply
            return false;
        }
        add(results);
        return true;
    }

    /**
     * Add this node and evaluate children (add applicable ancestor-penalties to
     * the list).
     * 
     * @param results
     */
    protected void add(final IPenaltyList results) {
        if (penalty != null) {
            /*
             * TODO: Consider abortOnApply taking effect here (typically this is
             * a leaf, if penalty is not null, but that isn't enforced yet).
             */
            penalty.addToPenaltyList(results);
        }
        if (childNodes.length > 0) {
            if (abortOnApply) {
                evaluateChildrenFCFS(results);
            }
            else {
                evaluateAllChildren(results);
            }
        }
    }

    /**
     * For choice of children one random 
     * @param results
     */
    protected void evaluateChildrenFCFS(final IPenaltyList results) {
        final double ref = ThreadLocalRandom.current().nextDouble(); // No scale contained yet.
        double floor = 0.0;
        for (int i = 0 ; i < childNodes.length; i++) {
            final PenaltyNode childNode = childNodes[i];
            final double nextFloor = floor + childNode.probability;
            // TODO: Configurable catch-all amount.
            if (nextFloor >= ref || nextFloor >= 0.999) {
                childNode.add(results);
                return;
            }
            floor = nextFloor;
        }
    }

    /**
     * Each of children can apply, which means for each child at least one
     * further random number is generated.
     * 
     * @param results
     */
    protected void evaluateAllChildren(final IPenaltyList results) {
        for (int i = 0 ; i < childNodes.length; i++) {
            childNodes[i].evaluate(results);
        }
    }

}
