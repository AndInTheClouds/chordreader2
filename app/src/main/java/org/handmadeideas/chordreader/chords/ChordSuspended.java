package org.handmadeideas.chordreader.chords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to indicate suspended fourth or suspended 2nd
 * @author nolan
 *
 */
public enum ChordSuspended {

	Sus4 (Arrays.asList("sus4", "suspended", "sus")),
	Sus2 (Arrays.asList("sus2", "suspended2"));
	
	private List<String> aliases;
	
	ChordSuspended (List<String> aliases) {
		this.aliases = aliases;
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public static List<String> getAllAliases() {
		List<String> result = new ArrayList<String>();
		
		for (ChordSuspended chordSuspended : values()) {
			result.addAll(chordSuspended.aliases);
		}
		
		return result;
	}	
	
	
	private static Map<String,ChordSuspended> lookupMap = new HashMap<String, ChordSuspended>();
	
	static {
		for (ChordSuspended value : values()) {
			for (String alias : value.aliases) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}
	
	public static ChordSuspended findByAlias(String alias) {
		return lookupMap.get(alias.toLowerCase());
	}	
}
