package org.hollowbamboo.chordreader2.chords.regex;

import org.hollowbamboo.chordreader2.chords.Chord;

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
	
	
	public static org.hollowbamboo.chordreader2.chords.regex.ChordInText newChordInText(Chord chord, int startIndex, int endIndex) {
		
		org.hollowbamboo.chordreader2.chords.regex.ChordInText result = new org.hollowbamboo.chordreader2.chords.regex.ChordInText();
		
		result.chord = chord;
		result.startIndex = startIndex;
		result.endIndex = endIndex;
		
		return result;
	}

	public static Comparator<org.hollowbamboo.chordreader2.chords.regex.ChordInText> sortByStartIndex() {
		return new Comparator<org.hollowbamboo.chordreader2.chords.regex.ChordInText>() {

			@Override
			public int compare(org.hollowbamboo.chordreader2.chords.regex.ChordInText object1, org.hollowbamboo.chordreader2.chords.regex.ChordInText object2) {
				return object1.getStartIndex() - object2.getStartIndex();
			}};
	}
	
	@Override
	public String toString() {
		return "ChordInText [chord=" + chord + ", endIndex=" + endIndex
				+ ", startIndex=" + startIndex + "]";
	}
	
}
