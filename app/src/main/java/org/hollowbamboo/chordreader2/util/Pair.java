package org.hollowbamboo.chordreader2.util;

public class Pair<E,T> {

	private final E first;
	private final T second;
	public Pair(E first, T second) {
		super();
		this.first = first;
		this.second = second;
	}
	public E getFirst() {
		return first;
	}
	public T getSecond() {
		return second;
	}
	public String toString() {
		return "<" + first + ", " + second + ">";
	}
	
	
	
}
