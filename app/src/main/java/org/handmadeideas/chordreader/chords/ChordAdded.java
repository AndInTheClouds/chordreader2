package org.handmadeideas.chordreader.chords;

import java.util.*;

/**
 * Enum for add9, add11, power chords, etc.
 * @author nolan
 *
 */
public enum ChordAdded {

	Add9 (Arrays.asList("add9", "2")),
	Add11 (Arrays.asList("add11", "4")),
	Major6 (Arrays.asList("6","maj6","major6", "M6")),
	SixNine (Arrays.asList("6/9")),
	PowerChord (Arrays.asList("5")), // duh duh DUH, duh duh DUH-duh, duh duh DUH, duh duh ((c) Deep Purple)
	;
	
	private List<String> aliases;
	
	ChordAdded (List<String> aliases) {
		this.aliases = aliases;
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public static List<String> getAllAliases() {
		List<String> result = new ArrayList<String>();
		
		for (org.handmadeideas.chordreader.chords.ChordAdded chordAdded : values()) {
			result.addAll(chordAdded.aliases);
		}
		
		return result;
	}
	
	private static Map<String, org.handmadeideas.chordreader.chords.ChordAdded> lookupMap = new HashMap<String, org.handmadeideas.chordreader.chords.ChordAdded>();
	
	static {
		for (org.handmadeideas.chordreader.chords.ChordAdded value : values()) {
			for (String alias : value.aliases) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}
	
	public static org.handmadeideas.chordreader.chords.ChordAdded findByAlias(String alias) {
		return lookupMap.get(alias.toLowerCase());
	}

}
