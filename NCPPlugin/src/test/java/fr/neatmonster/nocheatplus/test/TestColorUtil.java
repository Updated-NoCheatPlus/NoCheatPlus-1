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

import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.junit.Test;

import fr.neatmonster.nocheatplus.utilities.ColorUtil;
import fr.neatmonster.nocheatplus.MockServerBase;

public class TestColorUtil extends MockServerBase {
	@Test
	public void testRemoveColor(){
		List<String[]> items = new LinkedList<String[]>();
		for (String[] item : new String[][]{
				{"", ""},
				{" ", " "},
				{"&", "&"},
				{"&&", "&&"},
				{"o3rg7cbo'!§)=?%V823rg7c", "o3rg7cbo'!§)=?%V823rg7c"},
		}){
			items.add(item);
		}
		String[][] generic = new String[][]{
				{"&/&/&/", ""},
				{"&/&/", ""},
				{" &/&/ ", "  "},
				{" &/&/", " "},
				{"&/", ""},
				{"&/ ", " "},
				{" &/", " "},
				{"123&/123", "123123"},
				
		};
		for (ChatColor color : ChatColor.values()){
			char c = color.getChar();
			for (String[] pattern : generic){
				items.add(new String[]{ pattern[0].replace('/', c), pattern[1]});
			}
		}
		int i = 0;
		for (String[] item : items){
			String input = item[0];
			String expectedOutput = item[1];
			String detail = "no details.";
			String output = "(ERROR)"; 
			try{
				output = ColorUtil.removeColors(input);
			}
			catch(Throwable t){
				detail = t.getClass().getSimpleName() + ": " + t.getMessage();
			}
			if (!expectedOutput.equals(output)){
				fail("Wrong output at index " + i + " for input '" + input + "', expected '" + expectedOutput + "', but got instead: '" + output + "', details: " + detail );
			}
			i ++;
		}
	}
}
