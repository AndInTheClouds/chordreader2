package org.handmadeideas.chordreader.chords.regex;

import org.handmadeideas.chordreader.chords.Chord;

import java.util.Comparator;

public class ChordInText {

	private Chord chord;
	private int startIndex;
	private int endIndex;
	
	public Chord getChord() {
		return chord;
	}
	

	public void setChord(Chord chord) {
		this.chord = chord;
	}


	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}
	
	
	public static org.handmadeideas.chordreader.chords.regex.ChordInText newChordInText(Chord chord, int startIndex, int endIndex) {
		
		org.handmadeideas.chordreader.chords.regex.ChordInText result = new org.handmadeideas.chordreader.chords.regex.ChordInText();
		
		result.chord = chord;
		result.startIndex = startIndex;
		result.endIndex = endIndex;
		
		return result;
	}

	public static Comparator<org.handmadeideas.chordreader.chords.regex.ChordInText> sortByStartIndex() {
		return new Comparator<org.handmadeideas.chordreader.chords.regex.ChordInText>() {

			@Override
			public int compare(org.handmadeideas.chordreader.chords.regex.ChordInText object1, org.handmadeideas.chordreader.chords.regex.ChordInText object2) {
				return object1.getStartIndex() - object2.getStartIndex();
			}};
	}
	
	@Override
	public String toString() {
		return "ChordInText [chord=" + chord + ", endIndex=" + endIndex
				+ ", startIndex=" + startIndex + "]";
	}
	
}
