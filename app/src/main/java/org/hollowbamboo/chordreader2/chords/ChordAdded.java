package org.hollowbamboo.chordreader2.chords;

import java.util.*;

/**
 * Enum for add9, add11, power chords, etc.
 * @author nolan
 *
 */
public enum ChordAdded {

	Add6 (Arrays.asList("add6")),
	Add9 (Arrays.asList("add9", "2")),
	Add11 (Arrays.asList("add11", "4")),
	Augmented9 (Arrays.asList("aug9","+9")),
	AugmentedMajor9 (Arrays.asList("augmaj9","+M9")),
	Major6 (Arrays.asList("6","maj6","major6", "M6")),
	Major6Flat5 (Arrays.asList("6b5")),
	SixNine (Arrays.asList("6/9","6add9","6/add9","69")),
	NineFlat5 (Arrays.asList("9b5")),
	NineSharp5 (Arrays.asList("9#5")),
	PowerChord (Arrays.asList("5")), // duh duh DUH, duh duh DUH-duh, duh duh DUH, duh duh ((c) Deep Purple)
	;
	
	private final List<String> aliases;
	
	ChordAdded (List<String> aliases) {
		this.aliases = aliases;
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public static List<String> getAllAliases() {
		List<String> result = new ArrayList<String>();
		
		for (org.hollowbamboo.chordreader2.chords.ChordAdded chordAdded : values()) {
			result.addAll(chordAdded.aliases);
		}
		
		return result;
	}
	
	private static final Map<String, org.hollowbamboo.chordreader2.chords.ChordAdded> lookupMap = new HashMap<String, org.hollowbamboo.chordreader2.chords.ChordAdded>();
	
	static {
		for (org.hollowbamboo.chordreader2.chords.ChordAdded value : values()) {
			for (String alias : value.aliases) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}
	
	public static org.hollowbamboo.chordreader2.chords.ChordAdded findByAlias(String alias) {
		return lookupMap.get(alias.toLowerCase());
	}

}
