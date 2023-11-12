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
package fr.neatmonster.nocheatplus.utilities.ds.prefixtree;

import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.SimpleTimedCharPrefixTree.SimpleTimedCharLookupEntry;
import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.TimedCharPrefixTree.SimpleTimedCharNode;

public class SimpleTimedCharPrefixTree extends TimedCharPrefixTree<SimpleTimedCharNode, SimpleTimedCharLookupEntry> {
	
	public static class SimpleTimedCharLookupEntry extends TimedCharLookupEntry<SimpleTimedCharNode>{
		public SimpleTimedCharLookupEntry(SimpleTimedCharNode node, SimpleTimedCharNode insertion, int depth, boolean hasPrefix) {
			super(node, insertion, depth, hasPrefix);
		}
	}
	
	public SimpleTimedCharPrefixTree(final boolean access){
		super(
			new NodeFactory<Character, SimpleTimedCharNode>(){
				@Override
				public final SimpleTimedCharNode newNode(final SimpleTimedCharNode parent) {
					final long ts;
					if (parent == null) ts = System.currentTimeMillis();
					else ts = parent.ts;
					return new SimpleTimedCharNode(ts);
				}
				}
				,
				 new LookupEntryFactory<Character, SimpleTimedCharNode, SimpleTimedCharLookupEntry>() {
					@Override
					public final SimpleTimedCharLookupEntry newLookupEntry(final SimpleTimedCharNode node,
							final SimpleTimedCharNode insertion, final int depth, final boolean hasPrefix) {
						return new SimpleTimedCharLookupEntry(node, insertion, depth, hasPrefix);
					}
				}, 
				access);
	}

}
