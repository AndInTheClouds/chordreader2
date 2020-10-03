package org.handmadeideas.chordreader.helper;

import org.handmadeideas.chordreader.chords.Chord;
import org.handmadeideas.chordreader.chords.ChordRoot;

public class TransposeHelper {

	/**
	 * Transpose a chord given what fret the capo is on, and how many half steps we're transposing beyond that.
	 * @param chord
	 * @param capoFret
	 * @param transposeHalfSteps
	 * @return
	 */
	public static Chord transposeChord(Chord chord, int capoFret, int transposeHalfSteps) {
		
		int trueTranspose = capoFret - transposeHalfSteps;
		
		Chord newChord = (Chord) chord.clone();
		
		newChord.setRoot(transposeRoot(newChord.getRoot(), trueTranspose));
		
		if (newChord.getOverridingRoot() != null) {
			newChord.setOverridingRoot(transposeRoot(newChord.getOverridingRoot(), trueTranspose));
		}
		
		return newChord;
		
	}

	private static ChordRoot transposeRoot(ChordRoot root, int trueTranspose) {
		
		int ordinal = root.ordinal();
		ordinal += trueTranspose;
		
		int numChordRoots = ChordRoot.values().length;
		while (ordinal >= numChordRoots) {
			ordinal -= numChordRoots;
		}
		while (ordinal < 0) {
			ordinal += numChordRoots;
		}
		
		return ChordRoot.values()[ordinal];
	}
	
}
