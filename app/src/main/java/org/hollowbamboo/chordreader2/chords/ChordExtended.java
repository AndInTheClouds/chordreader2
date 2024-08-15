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
	Major9 (Major, Arrays.asList("9","maj9", "M9", "dom9","7/9","7(add9)", "7(9)","7/add9")),
	Major11 (Major, Arrays.asList("11", "maj11", "M11")),
	Major13 (Major, Arrays.asList("13", "maj13", "M13")),

	MajorSharp11 (Major, Arrays.asList("maj#11", "M#11")),
	MajorSharp13 (Major, Arrays.asList("maj#13", "M#13")),

	Minor9 (Minor, Arrays.asList("m9", "mih9")),
	Minor11 (Minor, Arrays.asList("m11", "min11")),
	Minor13 (Minor, Arrays.asList("m13", "min13")),
	
	// weird ones
	AugmentedDominant7Sharp5 (Major, Arrays.asList("7#5", "7(#5)", "7+5", "+7", "aug7")),
	AugmentedMajor7 (Major, Arrays.asList("maj7#5", "maj7(#5)", "augM7", "+M7", "M7#5", "M7(#5)", "M7/#5", "M7+5", "maj+7", "augmaj7")),
	AugmentedMinor5 (Minor, Arrays.asList("m#5", "-(5+)")),

	HalfDiminished7 (Minor, Arrays.asList("m7b5", "m7#5", "m7(b5)", "min7(b5)", "-7b5")),
	MinorMajor7Sharp5 (Minor, Arrays.asList("mmaj7#5","mM7#5")),
	Minor7Add9 (Minor, Arrays.asList("m9", "m7/9", "min7(add9)", "minor9", "min9", "m7(add9)", "-7/9", "-7(add9)", "min7/9", "min7(add9)", "m7+9")),
	Major7Add11 (Major, Arrays.asList("7/11","7(add11)", "7(11)","7/add11")),
	Minor7Add11 (Minor, Arrays.asList("m7/11","m7(add11)", "m7(11)","m7/add11")),
	Major7Diminished5 (Major, Arrays.asList("7b5","7(5-)","maj7b5","+7b5","M7b5")),
	Minor7Diminished5 (Minor, Arrays.asList("m7b5","min7(b5)","min7b5","m7(5-)","min7(5-)")),
	MinorMajor7Diminished5 (Major, Arrays.asList("mmaj7b5","mM7b5")),
	MinorMajor7DoubleDiminished5 (Major, Arrays.asList("mmaj7bb5","mM7bb5")),
	Major7flat5sharp9 (Major, Arrays.asList("7b5#9","7-5+9","dom7b5#9")),
	Major7flat9 (Major, Arrays.asList("7b9","7-9","dom7b9")),
	Major7flat9flat13(Major, Collections.singletonList("7b9b13")),
	Major7sharp9(Major,Arrays.asList("7#9","7+9","dom7#9","7/#9")),
	Major7sharp9Flat5(Major,Arrays.asList("7#9b5","7+9-5","dom7#9b5","7/#9b5")),
	Major7sharp11(Major,Arrays.asList("7#11","7+11","dom7#11","maj7#11","maj7/#11")),
	MajorFlat5(Major,Arrays.asList("majb5","Mb5")),
	MinorMajor7(Minor,Arrays.asList("mmaj7","MinMaj7","m(maj7)","min/maj7", "mM7", "min(maj7)")),
	MinorMajor9(Minor,Arrays.asList("mmaj9","MinMaj9","m(maj9)","min/maj9", "mM9", "min(maj9)")),
	MinorMajor11(Minor,Arrays.asList("mmaj11","MinMaj11","m(maj11)","min/maj11", "mM11", "min(maj11)")),
	MinorMajor13(Minor,Arrays.asList("mmaj13","MinMaj13","m(maj13)","min/maj1", "mM13", "min(maj13)")),
	MinorDoubleFlat5(Minor,Collections.singletonList("mbb5")),
	MinorSharp5(Minor,Arrays.asList("m#5","Mb5")),

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
