package org.handmadeideas.chordreader.chords;

public class Chord implements Cloneable {

	private ChordRoot root;
	private ChordQuality quality;
	private ChordExtended seventh;
	private ChordAdded added;
	private ChordSuspended suspended;
	private ChordRoot overridingRoot;
	
	public ChordRoot getRoot() {
		return root;
	}
	public void setRoot(ChordRoot root) {
		this.root = root;
	}
	public ChordQuality getQuality() {
		return quality;
	}
	public void setQuality(ChordQuality quality) {
		this.quality = quality;
	}
	public ChordExtended getSeventh() {
		return seventh;
	}
	public void setSeventh(ChordExtended seventh) {
		this.seventh = seventh;
	}
	public ChordAdded getAdded() {
		return added;
	}
	public void setAdded(ChordAdded added) {
		this.added = added;
	}
	public ChordSuspended getSuspended() {
		return suspended;
	}
	public void setSuspended(ChordSuspended suspended) {
		this.suspended = suspended;
	}
	
	
	
	public ChordRoot getOverridingRoot() {
		return overridingRoot;
	}
	public void setOverridingRoot(ChordRoot overridingRoot) {
		this.overridingRoot = overridingRoot;
	}
	
	@Override
	public Object clone() {
			
		org.handmadeideas.chordreader.chords.Chord clonedChord = new org.handmadeideas.chordreader.chords.Chord();
		
		clonedChord.root = this.root;
		clonedChord.quality = this.quality;
		clonedChord.seventh = this.seventh;
		clonedChord.added = this.added;
		clonedChord.suspended = this.suspended;
		clonedChord.overridingRoot = this.overridingRoot;
		
		return clonedChord;
				
	}
	
	public static org.handmadeideas.chordreader.chords.Chord newChord(ChordRoot root, ChordQuality quality, ChordExtended seventh,
                                                                    ChordAdded added, ChordSuspended suspended, ChordRoot overridingRoot) {
		
		org.handmadeideas.chordreader.chords.Chord chord = new org.handmadeideas.chordreader.chords.Chord();
		
		chord.root = root;
		chord.quality = quality;
		chord.seventh = seventh;
		chord.added = added;
		chord.suspended = suspended;
		chord.overridingRoot = overridingRoot;
		
		return chord;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((added == null) ? 0 : added.hashCode());
		result = prime * result
				+ ((overridingRoot == null) ? 0 : overridingRoot.hashCode());
		result = prime * result + ((quality == null) ? 0 : quality.hashCode());
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		result = prime * result + ((seventh == null) ? 0 : seventh.hashCode());
		result = prime * result
				+ ((suspended == null) ? 0 : suspended.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		org.handmadeideas.chordreader.chords.Chord other = (org.handmadeideas.chordreader.chords.Chord) obj;
		if (added == null) {
			if (other.added != null)
				return false;
		} else if (!added.equals(other.added))
			return false;
		if (overridingRoot == null) {
			if (other.overridingRoot != null)
				return false;
		} else if (!overridingRoot.equals(other.overridingRoot))
			return false;
		if (quality == null) {
			if (other.quality != null)
				return false;
		} else if (!quality.equals(other.quality))
			return false;
		if (root == null) {
			if (other.root != null)
				return false;
		} else if (!root.equals(other.root))
			return false;
		if (seventh == null) {
			if (other.seventh != null)
				return false;
		} else if (!seventh.equals(other.seventh))
			return false;
		if (suspended == null) {
			if (other.suspended != null)
				return false;
		} else if (!suspended.equals(other.suspended))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Chord [added=" + added + ", overridingRoot=" + overridingRoot
				+ ", quality=" + quality + ", root=" + root + ", seventh="
				+ seventh + ", suspended=" + suspended + "]";
	}
	
	public String toPrintableString(NoteNaming noteNaming) {
		
		// TODO: make the aliases customizable rather than just taking the first one
		StringBuilder stringBuilder = new StringBuilder(
				noteNaming.getNames(root).get(0));
		
		if (seventh != null) {
			stringBuilder.append(seventh.getAliases().get(0));
		} else {
			stringBuilder.append(quality.getAliases().get(0));
		}
		
		if (added != null) {
			stringBuilder.append(added.getAliases().get(0));
		}
		
		if (suspended != null) {
			stringBuilder.append(suspended.getAliases().get(0));
		}
		
		if (overridingRoot != null) {
			stringBuilder.append("/").append(noteNaming.getNames(overridingRoot).get(0));
		}
		
		return stringBuilder.toString();
		
	}	
}