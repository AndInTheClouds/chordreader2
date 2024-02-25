package org.hollowbamboo.chordreader2.chords;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.util.EnumMultiMapBuilder;

import java.util.*;

/**
 * Enum for different note naming conventions.
 * @author nolan
 *
 */
public enum NoteNaming {

	English (R.string.pref_note_naming_english,
			new EnumMultiMapBuilder<ChordRoot,String>(ChordRoot.class)
			.put(ChordRoot.A , "A")
			.put(ChordRoot.Bb , "Bb", "A#", "Asharp", "Bflat", "A♯", "B♭")
			.put(ChordRoot.B , "B", "Cb", "Cflat", "C♭")
			.put(ChordRoot.C , "C", "B#", "Bsharp", "B♯")
			.put(ChordRoot.Db , "Db", "C#", "Dflat", "Csharp", "C♯", "D♭")
			.put(ChordRoot.D , "D")
			.put(ChordRoot.Eb , "Eb", "D#", "Eflat", "Dsharp", "D♯", "E♭")
			.put(ChordRoot.E , "E", "Fb", "Fflat", "F♭")
			.put(ChordRoot.F , "F", "E#", "Esharp", "E♯")
			.put(ChordRoot.Gb , "Gb", "F#", "Gflat", "Fsharp", "F♯", "G♭")
			.put(ChordRoot.G , "G")
			.put(ChordRoot.Ab , "Ab", "G#", "Aflat", "Gsharp", "G♯", "A♭")
			.build()),
	
	NorthernEuropean (R.string.pref_note_naming_northern,
			new EnumMultiMapBuilder<ChordRoot,String>(ChordRoot.class)
			.put(ChordRoot.A , "A")
			.put(ChordRoot.Bb , "B", "A#", "Asharp", "A♯")
			.put(ChordRoot.B , "H")
			.put(ChordRoot.C , "C")
			.put(ChordRoot.Db , "C#", "Db", "Dflat", "Csharp", "C♯", "D♭")
			.put(ChordRoot.D , "D")
			.put(ChordRoot.Eb , "D#", "Eb", "Eflat", "Dsharp", "D♯", "E♭")
			.put(ChordRoot.E , "E", "Fb", "Fflat", "F♭")
			.put(ChordRoot.F , "F", "E#", "Esharp", "E♯")
			.put(ChordRoot.Gb , "F#", "Gb", "Gflat", "Fsharp", "F♯", "G♭")
			.put(ChordRoot.G , "G")
			.put(ChordRoot.Ab , "G#", "Ab", "Aflat", "Gsharp", "G♯", "A♭")
			.build()),
			
	SouthernEuropean (R.string.pref_note_naming_southern,
			new EnumMultiMapBuilder<ChordRoot,String>(ChordRoot.class)
			.put(ChordRoot.A , "La")
			.put(ChordRoot.Bb , "Tib", "La#", "La♯", "Ti♭")
			.put(ChordRoot.B , "Ti")
			.put(ChordRoot.C , "Do")
			.put(ChordRoot.Db , "Reb", "R�b", "Do#", "Do♯", "Re♭")
			.put(ChordRoot.D , "Re", "R�")
			.put(ChordRoot.Eb , "Mib", "Re#", "Re♯", "Mi♭")
			.put(ChordRoot.E , "Mi")
			.put(ChordRoot.F , "Fa")
			.put(ChordRoot.Gb , "Solb", "Sob", "Fa#", "Fa♯", "So♭")
			.put(ChordRoot.G , "Sol", "So")
			.put(ChordRoot.Ab , "Lab", "So#", "Sol#", "So♯", "Sol♯", "La♭")
			.build()),
	;
	
	private final EnumMap<ChordRoot, List<String>> noteNames;
	private final Map<String,ChordRoot> lookupMap = new HashMap<String, ChordRoot>();
	private final int printableNameResource;
	
	NoteNaming(int printableNameResource,
			EnumMap<ChordRoot, List<String>> noteNames) {
		if (noteNames.size() != 12) {
			throw new IllegalArgumentException("must have 12 notes");
		}
		this.noteNames = noteNames;
		this.printableNameResource = printableNameResource;
		
		for (ChordRoot value : ChordRoot.values()) {
			for (String alias : noteNames.get(value)) {
				lookupMap.put(alias.toLowerCase(), value);
			}
		}
	}


	public List<String> getAllNames() {
		List<String> result = new ArrayList<String>();
		
		for (ChordRoot chordRoot : ChordRoot.values()) {
			result.addAll(noteNames.get(chordRoot));
		}
		
		return result;
	}
	
	public ChordRoot findByAlias(String alias) {
		return lookupMap.get(alias.toLowerCase());
	}
	
	public List<String> getNames(ChordRoot chordRoot) {
		return noteNames.get(chordRoot);
	}

	public int getPrintableNameResource() {
		return printableNameResource;
	}
	
}
