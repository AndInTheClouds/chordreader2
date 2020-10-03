package org.handmadeideas.chordreader.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {

	public static <T> List<T> concatenate(List<T> left, List<T> right) {
		List<T> result = new ArrayList<T>(left.size() + right.size());
		result.addAll(left);
		result.addAll(right);
		return result;
	}
	
}
