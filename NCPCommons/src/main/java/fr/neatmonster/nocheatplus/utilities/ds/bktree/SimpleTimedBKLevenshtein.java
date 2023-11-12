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
package fr.neatmonster.nocheatplus.utilities.ds.bktree;

import java.util.Collection;

import fr.neatmonster.nocheatplus.utilities.ds.bktree.SimpleTimedBKLevenshtein.STBKLResult;
import fr.neatmonster.nocheatplus.utilities.ds.bktree.TimedBKLevenshtein.SimpleTimedLevenNode;

public class SimpleTimedBKLevenshtein extends TimedBKLevenshtein<SimpleTimedLevenNode, STBKLResult> {

	public static class STBKLResult extends LookupEntry<char[], SimpleTimedLevenNode>{
		public STBKLResult(Collection<SimpleTimedLevenNode> nodes, SimpleTimedLevenNode match, int distance, boolean isNew) {
			super(nodes, match, distance, isNew);
		}
		
	}
	
	public SimpleTimedBKLevenshtein() {
		super(
			new NodeFactory<char[], SimpleTimedLevenNode>(){
				@Override
				public SimpleTimedLevenNode newNode( char[] value, SimpleTimedLevenNode parent) {
					return new SimpleTimedLevenNode(value);
				}	
			}
			,
			new LookupEntryFactory<char[], SimpleTimedLevenNode, STBKLResult>() {
				@Override
				public STBKLResult newLookupEntry(Collection<SimpleTimedLevenNode> nodes, SimpleTimedLevenNode match, int distance, boolean isNew) {
					return new STBKLResult(nodes, match, distance, isNew);
				}
			}
			);
	}
}
