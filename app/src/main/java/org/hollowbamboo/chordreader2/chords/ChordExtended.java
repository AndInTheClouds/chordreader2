package org.hollowbamboo.chordreader2.chords;


import java.util.*;

import static org.hollowbamboo.chordreader2.chords.ChordQuality.*;

/**
 * e.g. seventh, ninth, elevenths
 * @author nolan
 *
 */
public enum ChordExtended {

	
	// sevenths
	Major7 (Major, Arrays.asList("maj7", "Maj7", "M7", "+7")),
	Minor7 (Minor, Arrays.asList("m7", "Min7", "min7", "minor7")),
	Dominant7 (Major, Arrays.asList("7", "dom7", "dominant7")),
	Diminished7 (Diminished, Arrays.asList("dim7", "diminished7")),
	
	// true extended
	Major9 (Major, Arrays.asList("maj9", "M9", "9")),
	Major11 (Major, Arrays.asList("maj11", "M11", "11")),
	Major13 (Major, Arrays.asList("maj13", "M13", "13")),
	
	// weird ones
	AugmentedDominant7 (Major, Arrays.asList("7#5", "7(#5)", "7+5")), //TODO: Rename to AugmentedDominant7Sharp5
	//AugmentedDominant7Flat5 (Major, Arrays.asList("7b5", "7(b5)", "7-5")), //TODO: integrate
	AugmentedMajor7 (Major, Arrays.asList("maj7#5", "maj7(#5)")),
	HalfDiminished7 (Minor, Arrays.asList("m7b5", "m7#5", "m7(b5)", "min7(b5)", "-7b5")),

	Major7Add9 (Major, Arrays.asList("7/9","7(add9)", "7(9)")),
	Minor7Add9 (Minor, Arrays.asList("m7/9", "min7(add9)", "m9", "minor9", "min9", "m7(add9)", "-7/9", "-7(add9)", "min7/9", "min7(add9)", "m7+9")),

	//TODO: add additional seventh chords
	;

	private final List<String> aliases;
	private final ChordQuality chordQuality;
	
	ChordExtended(ChordQuality chordQuality, List<String> aliases) {
		this.chordQuality = chordQuality;
		this.aliases = aliases;
	}
	
	public List<String> getAliases() {
		return aliases;
	}

	/**
	 * A chord quality is inherent to every type of seventh.  See the wikipedia page for more info.
	 * <a href="http://en.wikipedia.org/wiki/Seventh_chord#Types_of_seventh_chords">...</a>
	 */
	public ChordQuality getChordQuality() {
		return chordQuality;
	}
	
	
	public static List<String> getAllAliases() {
		List<String> result = new ArrayList<>();
		
		for (org.hollowbamboo.chordreader2.chords.ChordExtended chordSeventh : values()) {
			result.addAll(chordSeventh.aliases);
		}
		
		return result;
	}		
	
	
	private static final Map<String, org.hollowbamboo.chordreader2.chords.ChordExtended> lookupMap = new HashMap<>();
	
	static {
		for (org.hollowbamboo.chordreader2.chords.ChordExtended value : values()) {
			for (String alias : value.aliases) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}
	
	public static org.hollowbamboo.chordreader2.chords.ChordExtended findByAlias(String alias) {
		
		// special case for M7 and m7
		if (alias.equals("M7")) {
			return Major7;
		} else if (alias.equals("m7")) {
			return Minor7;
		}
		
		return lookupMap.get(alias.toLowerCase());
	}	
	
}
