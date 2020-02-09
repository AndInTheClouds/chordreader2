package org.handmadeideas.chordreader.chords;


import java.util.*;

import static org.handmadeideas.chordreader.chords.ChordQuality.*;

/**
 * e.g. seventh, ninth, elevenths
 * @author nolan
 *
 */
public enum ChordExtended {

	
	// sevenths
	Major7 (Major, Arrays.asList("Maj7", "maj7", "M7", "+7")),
	Minor7 (Minor, Arrays.asList("Min7", "m7", "min7", "minor7")),
	Dominant7 (Major, Arrays.asList("7", "dom7", "dominant7")),
	Diminished7 (Diminished, Arrays.asList("dim7", "diminished7")),
	
	// true extended
	Major9 (Major, Arrays.asList("maj9", "M9", "9")),
	Major11 (Major, Arrays.asList("maj11", "M11", "11")),
	Major13 (Major, Arrays.asList("maj13", "M13", "13")),
	
	// weird ones
	
	AugmentedDominant7 (Major, Arrays.asList("7#5", "7(#5)")),
	AugmentedMajor7 (Major, Arrays.asList("maj7#5", "maj7(#5)")),
	
	// TODO: I don't know what this one is - can't find it on wikipedia
	Minor9 (Minor, Arrays.asList("min9", "m9","minor9")),
	
	
	;
	/**
	 * TODO: add additional seventh chords
	 */
	
	private List<String> aliases;
	private ChordQuality chordQuality;
	
	ChordExtended(ChordQuality chordQuality, List<String> aliases) {
		this.chordQuality = chordQuality;
		this.aliases = aliases;
	}
	
	public List<String> getAliases() {
		return aliases;
	}

	/**
	 * A chord quality is inherent to every type of seventh.  See the wikipedia page for more info.
	 * http://en.wikipedia.org/wiki/Seventh_chord#Types_of_seventh_chords
	 * @return
	 */
	public ChordQuality getChordQuality() {
		return chordQuality;
	}
	
	
	public static List<String> getAllAliases() {
		List<String> result = new ArrayList<String>();
		
		for (org.handmadeideas.chordreader.chords.ChordExtended chordSeventh : values()) {
			result.addAll(chordSeventh.aliases);
		}
		
		return result;
	}		
	
	
	private static Map<String, org.handmadeideas.chordreader.chords.ChordExtended> lookupMap = new HashMap<String, org.handmadeideas.chordreader.chords.ChordExtended>();
	
	static {
		for (org.handmadeideas.chordreader.chords.ChordExtended value : values()) {
			for (String alias : value.aliases) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}
	
	public static org.handmadeideas.chordreader.chords.ChordExtended findByAlias(String alias) {
		
		// special case for M7 and m7
		if (alias.equals("M7")) {
			return Major7;
		} else if (alias.equals("m7")) {
			return Minor7;
		}
		
		return lookupMap.get(alias.toLowerCase());
	}	
	
}
