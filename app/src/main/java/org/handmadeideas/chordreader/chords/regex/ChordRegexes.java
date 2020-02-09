package org.handmadeideas.chordreader.chords.regex;

import android.text.TextUtils;
import org.handmadeideas.chordreader.chords.*;
import org.handmadeideas.chordreader.util.ListUtil;

import java.util.*;
import java.util.regex.Pattern;

public class ChordRegexes {

	private static final Map<NoteNaming, ChordRegex> chordRegexes = initializeChordRegexes();
	
	public static Pattern getChordPattern(NoteNaming noteNaming) {
		return chordRegexes.get(noteNaming).getPattern();
	}
	
	private static Map<NoteNaming, ChordRegex> initializeChordRegexes() {
		Map<NoteNaming, ChordRegex> map = new HashMap<NoteNaming, ChordRegex>();
		for (NoteNaming noteNaming : NoteNaming.values()) {
			map.put(noteNaming, initializeChordRegex(noteNaming));
		}
		return map;
	}

	private static ChordRegex initializeChordRegex(NoteNaming noteNaming) {
		ChordRegex chordRegex = new ChordRegex();
		
		String regexString = createRegexString(noteNaming);
		String regexStringWithParens = createRegexStringWithParens(regexString);
		
		chordRegex.setRegexString(regexString);
		chordRegex.setRegexStringWithParens(regexStringWithParens);
		chordRegex.setPattern(Pattern.compile(regexString));
		chordRegex.setPatternWithParens(Pattern.compile(regexStringWithParens));
		
		return chordRegex;
	}

	public static Pattern getChordWithParensPattern(NoteNaming noteNaming) {
		return chordRegexes.get(noteNaming).getPatternWithParens();
	}
	
	private static String optional(String pattern) {
		return "(" + pattern + "?)";
	}
	
	private static String greedyDisjunction(List<String> aliases) {
		return greedyDisjunction(aliases, false);
	}
	
	private static String createRegexString(NoteNaming noteNaming) {

		return greedyDisjunction(noteNaming.getAllNames(), true) + // root note
		optional(greedyDisjunction(ListUtil.concatenate(
				ChordQuality.getAllAliases(), 
				ChordExtended.getAllAliases()))) + // quality OR seventh
		optional(greedyDisjunction(ChordAdded.getAllAliases())) + // add
		optional(greedyDisjunction(ChordSuspended.getAllAliases())) + // sus
		optional("(?:/" + greedyDisjunction(noteNaming.getAllNames()) + ")") + // overridden root note ("over")
		"";
	}
	
	private static String createRegexStringWithParens(String regexString) {
		return "[\\(\\[]" + regexString + "[\\)\\]]";
	}
	
	/**
	 * Take an array of strings and make a greedy disjunction regex pattern out of it,
	 * with the longest strings first, e.g. ["sus4","sus","sus2"] -->
	 * 
	 * (sus4|sus2|sus)
	 * @param allAliases
	 * @return
	 */
	private static String greedyDisjunction(List<String> aliases, boolean matchingGroup) {
		
		aliases = new ArrayList<String>(aliases); // copy
		
		// sort by longest string first
		Collections.sort(aliases, new Comparator<Object>(){
			
			@Override
			public int compare(Object o1, Object o2) {
				return ((String)o2).length() - ((String)o1).length();
			}
			
			
		});
		
		StringBuilder stringBuilder = new StringBuilder("(");

		if (!matchingGroup) {
			stringBuilder.append("?:"); // non-matching group
		}
		
		
		for (String alias : aliases) {;
			
			if (TextUtils.isEmpty(alias)) {
				continue; // e.g. the "major" quality can be expressed as an empty string, so skip in the regex
			}
			
			stringBuilder.append(Pattern.quote(alias)).append("|");
		}
		
		// get rid of final dangling pipe |
		return stringBuilder.substring(0,stringBuilder.length() - 1) + ")";
		
	}
	
}
