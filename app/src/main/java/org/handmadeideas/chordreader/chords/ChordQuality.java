package org.handmadeideas.chordreader.chords;

import java.util.*;

/**
 * Class represeting the quality of a chord, e.g. maj/min/aug/dim.
 * @author nolan
 *
 */
public enum ChordQuality {

	Major (Arrays.asList("", "major", "maj", "M")),
	Minor (Arrays.asList("m", "minor", "min")),
	Augmented (Arrays.asList("aug","augmented","+")),
	Diminished (Arrays.asList("dim","diminished"));
	
	private List<String> aliases;
	
	ChordQuality (List<String> aliases) {
		this.aliases = aliases;
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public static List<String> getAllAliases() {
		List<String> result = new ArrayList<String>();
		
		for (org.handmadeideas.chordreader.chords.ChordQuality chordQuality : values()) {
			result.addAll(chordQuality.aliases);
		}
		
		return result;
	}	
	
	
	private static Map<String, org.handmadeideas.chordreader.chords.ChordQuality> lookupMap = new HashMap<String, org.handmadeideas.chordreader.chords.ChordQuality>();
	
	static {
		for (org.handmadeideas.chordreader.chords.ChordQuality value : values()) {
			for (String alias : value.aliases) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}
	
	public static org.handmadeideas.chordreader.chords.ChordQuality findByAlias(String alias) {
		
		// special case for 'm'
		if (alias.equals("m")) {
			return Minor;
		} else if (alias.equals("M")) {
			return Major;
		}
		
		return lookupMap.get(alias.toLowerCase());
	}	
}
