package org.handmadeideas.chordreader.db;

public class Transposition {

	private int id;
	private String filename;
	private int capo;
	private int transpose;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public int getCapo() {
		return capo;
	}
	public void setCapo(int capo) {
		this.capo = capo;
	}
	public int getTranspose() {
		return transpose;
	}
	public void setTranspose(int transpose) {
		this.transpose = transpose;
	}
	
	
	
}
