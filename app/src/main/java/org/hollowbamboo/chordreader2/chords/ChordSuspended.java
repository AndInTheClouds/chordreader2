package org.hollowbamboo.chordreader2.chords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	Sus2 (Arrays.asList("sus2", "suspended2")),
	SusMajor4 (Arrays.asList("susM4", "susmaj4")),
	SusMajor2 (Arrays.asList("susM2", "susmaj2")),
	Sus2Sus4 (Collections.singletonList("sus2sus4")),
	Sus2Flat5 (Arrays.asList("sus2b5", "2-5", "sus2-5")),
	Sus2Sharp5 (Arrays.asList("sus2#5", "sus2(#5)")),
	Sus4Seventh (Collections.singletonList("7sus4")),
	Sus4Sharp5 (Arrays.asList("sus4#5","sus4(#5)")),


    SevenSus2 (Arrays.asList("7sus2")),
	MajorSevenSus2 (Arrays.asList("M7sus2", "maj7sus2")),
    SevenSus4 (Arrays.asList("7sus4")),
	MajorSevenSus4 (Arrays.asList("M7sus4", "maj7sus4")),
    SevenSus2Sharp5 (Arrays.asList("7sus2#5","7sus2(#5)")),
    SevenSus4Sharp5 (Arrays.asList("7sus4#5", "7sus4(#5)")),
	MajorSevenSus4Sharp5 (Arrays.asList("M7sus4#5", "M7sus4(#5)","maj7sus4#5", "maj7sus4(#5)")),
    SevenSus2Sus4 (Arrays.asList("7sus2sus4")),
	MajorSevenSus2Sus4 (Arrays.asList("M7sus2sus4", "maj7sus2sus4")),

	;
	
	private final List<String> aliases;
	
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
	
	
	private static final Map<String,ChordSuspended> lookupMap = new HashMap<String, ChordSuspended>();
	
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
