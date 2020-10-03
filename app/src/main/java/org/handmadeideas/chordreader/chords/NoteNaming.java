package org.handmadeideas.chordreader.chords;

import org.handmadeideas.chordreader.R;
import org.handmadeideas.chordreader.util.EnumMultiMapBuilder;

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
			.put(ChordRoot.Bb , "Bb", "A#", "Asharp", "Bflat")
			.put(ChordRoot.B , "B")
			.put(ChordRoot.C , "C")
			.put(ChordRoot.Db , "Db", "C#", "Dflat", "Csharp")
			.put(ChordRoot.D , "D")
			.put(ChordRoot.Eb , "Eb", "D#", "Eflat", "Dsharp")
			.put(ChordRoot.E , "E")
			.put(ChordRoot.F , "F")
			.put(ChordRoot.Gb , "Gb", "F#", "Gflat", "Gsharp")
			.put(ChordRoot.G , "G")
			.put(ChordRoot.Ab , "Ab", "G#", "Aflat", "Gsharp")
			.build()),
	
	NorthernEuropean (R.string.pref_note_naming_northern,
			new EnumMultiMapBuilder<ChordRoot,String>(ChordRoot.class)
			.put(ChordRoot.A , "A")
			.put(ChordRoot.Bb , "B", "A#", "Asharp")
			.put(ChordRoot.B , "H")
			.put(ChordRoot.C , "C")
			.put(ChordRoot.Db , "Db", "C#", "Dflat", "Csharp")
			.put(ChordRoot.D , "D")
			.put(ChordRoot.Eb , "Eb", "D#", "Eflat", "Dsharp")
			.put(ChordRoot.E , "E")
			.put(ChordRoot.F , "F")
			.put(ChordRoot.Gb , "Gb", "F#", "Gflat", "Gsharp")
			.put(ChordRoot.G , "G")
			.put(ChordRoot.Ab , "Ab", "G#", "Aflat", "Gsharp")
			.build()),
			
	SouthernEuropean (R.string.pref_note_naming_southern,
			new EnumMultiMapBuilder<ChordRoot,String>(ChordRoot.class)
			.put(ChordRoot.A , "La")
			.put(ChordRoot.Bb , "Tib", "La#")
			.put(ChordRoot.B , "Ti")
			.put(ChordRoot.C , "Do")
			.put(ChordRoot.Db , "Reb", "R�b", "Do#")
			.put(ChordRoot.D , "Re", "R�")
			.put(ChordRoot.Eb , "Mib", "Re#")
			.put(ChordRoot.E , "Mi")
			.put(ChordRoot.F , "Fa")
			.put(ChordRoot.Gb , "Solb", "Sob", "Fa#")
			.put(ChordRoot.G , "Sol", "So")
			.put(ChordRoot.Ab , "Lab", "So#", "Sol#")
			.build()),
	;
	
	private EnumMap<ChordRoot, List<String>> noteNames;
	private Map<String,ChordRoot> lookupMap = new HashMap<String, ChordRoot>();
	private int printableNameResource;
	
	private NoteNaming(int printableNameResource,
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

	public EnumMap<ChordRoot, List<String>> getNoteNames() {
		return noteNames;
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
