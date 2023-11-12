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

import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.CharPrefixTree.SimpleCharNode;
import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.SimpleCharPrefixTree.SimpleCharLookupEntry;

/**
 * Adding some convenience methods.
 * @author mc_dev
 *
 */
public class SimpleCharPrefixTree extends CharPrefixTree<SimpleCharNode, SimpleCharLookupEntry>{

	public static class SimpleCharLookupEntry extends CharLookupEntry<SimpleCharNode>{
		public SimpleCharLookupEntry(SimpleCharNode node, SimpleCharNode insertion, int depth, boolean hasPrefix) {
			super(node, insertion, depth, hasPrefix);
		}
	}
	
	public SimpleCharPrefixTree(){
		super(new NodeFactory<Character, SimpleCharNode>(){
			@Override
			public final SimpleCharNode newNode(final SimpleCharNode parent) {
				return new SimpleCharNode();
			}
		}, new LookupEntryFactory<Character, SimpleCharNode, SimpleCharLookupEntry>() {
			@Override
			public final SimpleCharLookupEntry newLookupEntry(final SimpleCharNode node, final SimpleCharNode insertion, final int depth, final boolean hasPrefix) {
				return new SimpleCharLookupEntry(node, insertion, depth, hasPrefix);
			}
		});
	}

}
