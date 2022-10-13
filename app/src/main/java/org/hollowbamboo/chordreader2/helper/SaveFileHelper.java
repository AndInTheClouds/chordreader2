package org.hollowbamboo.chordreader2.helper;

import android.os.Environment;
import android.text.TextUtils;

import org.hollowbamboo.chordreader2.util.UtilLogger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SaveFileHelper {

	private static UtilLogger log = new UtilLogger(SaveFileHelper.class);
	
	public static boolean checkIfSdCardExists() {
		
		File sdcardDir = Environment.getExternalStorageDirectory();
			
		return sdcardDir != null && sdcardDir.listFiles() != null;
		
	}
	
	public static boolean fileExists(String filename) {

		filename = rectifyFilename(filename);

		File catlogDir = getBaseDirectory();
		
		File file = new File(catlogDir, filename);
		
		return file.exists();
	}
	
	public static void deleteFile(String filename) {

		filename = rectifyFilename(filename);

		File catlogDir = getBaseDirectory();
		
		File file = new File(catlogDir, filename);
		
		if(file.exists()) {
			file.delete();
		}
		
	}
	
	public static Date getLastModifiedDate(String filename) {

		filename = rectifyFilename(filename);
		
		File catlogDir = getBaseDirectory();
		
		File file = new File(catlogDir, filename);
		
		if(file.exists()) {
			return new Date(file.lastModified());
		} else {
			// shouldn't happen
			log.e("file last modified date not found: %s", filename);
			return new Date();
		}
	}

	public static List<String> getSavedSongNames() {
		List<File> fileNames = getFilenamesInBaseDirectory();

		ArrayList<File> tempArrayList = new ArrayList<File>();

		for (File file : fileNames) {
			String fileName = file.getName();
			if(fileName.endsWith(".txt"))
				tempArrayList.add(file);
		}

//		Collections.sort(tempArrayList, new Comparator<File>() {
//
//			@Override
//			public int compare(File object1, File object2) {
//				return new Long(object2.lastModified()).compareTo(object1.lastModified());
//			}
//		});


		List<String> result = new ArrayList<String>();

		for (File file : tempArrayList) {
			String fileName = file.getName().replace("_", " ").replace(".txt", "");
			result.add(fileName);
		}

		return result;
	}


	public static List<String> getSavedPlayListNames() {
		List<File> fileNames = getFilenamesInBaseDirectory();

		ArrayList<File> tempArrayList = new ArrayList<File>();

		for (File file : fileNames) {
			String fileName = file.getName();
			if(fileName.endsWith(".pl"))
				tempArrayList.add(file);
		}

		Collections.sort(tempArrayList, new Comparator<File>() {

			@Override
			public int compare(File object1, File object2) {
				return new Long(object2.lastModified()).compareTo(object1.lastModified());
			}
		});


		List<String> result = new ArrayList<String>();

		for (File file : tempArrayList) {
			String fileName = file.getName().replace("_", " ").replace(".pl", "");
			result.add(fileName);
		}

		return result;
	}

	public static List<File> getFilenamesInBaseDirectory() {
		
		File baseDir = getBaseDirectory();
		
		File[] filesArray = baseDir.listFiles();
		
		if(filesArray == null) {
			return Collections.emptyList();
		}

		return Arrays.asList(filesArray);
	}

	public static boolean isInvalidFilename(CharSequence filename) {

		String filenameAsString;

		return TextUtils.isEmpty(filename)
				|| (filenameAsString = filename.toString()).contains("/")
				|| filenameAsString.contains(":")
				|| filenameAsString.contains("\\")
				|| filenameAsString.contains("*")
				|| filenameAsString.contains("|")
				|| filenameAsString.contains(":")
				|| filenameAsString.contains("<")
				|| filenameAsString.contains(">")
				|| filenameAsString.contains("?");

	}

	public static String rectifyFilename(String filename) {
		filename = filename.replace(" ", "_");

		return filename;
	}

	public static String openFile(String filename) {

		filename = rectifyFilename(filename);

		File baseDir = getBaseDirectory();
		File logFile;

		if(!(filename == null))
			logFile = new File(baseDir, filename);
		else
			return "";
		
		StringBuilder result = new StringBuilder();
		
		BufferedReader bufferedReader = null;
		
		try {
			
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
			
			while (bufferedReader.ready()) {
				result.append(bufferedReader.readLine()).append("\n");
			}
		} catch (IOException ex) {
			log.e(ex, "couldn't read file");
			
		} finally {
			if(bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					log.e(e, "couldn't close buffered reader");
				}
			}
		}
		
		return result.toString();
	}

	public static List<String> openPlaylist(String filename) {

		if (!filename.endsWith(".pl"))
			filename = rectifyFilename(filename).concat(".pl");

		File baseDir = getBaseDirectory();
		File logFile;

		if(!(filename == null))
			logFile = new File(baseDir, filename);
		else
			return Collections.emptyList();

		List<String> filenames = new ArrayList<>();

		BufferedReader bufferedReader = null;

		try {

			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));

			while (bufferedReader.ready()) {
				String songFile = bufferedReader.readLine();
				if (fileExists(songFile)) {
					String songFileName = songFile.replace(".txt", "").replace("_"," ");
					filenames.add(songFileName);
				}
			}
		} catch (IOException ex) {
			log.e(ex, "couldn't read file");

		} finally {
			if(bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					log.e(e, "couldn't close buffered reader");
				}
			}
		}
		return filenames;
	}
	
	public static boolean saveFile(String filetext, String filename) {

		filename = rectifyFilename(filename);

		File baseDir = getBaseDirectory();
		
		File newFile = new File(baseDir, filename);
		try {
			if(!newFile.exists()) {
				newFile.createNewFile();
			}
		} catch (IOException ex) {
			log.e(ex, "couldn't create new file");
			return false;
		}
		PrintStream out = null;
		try {
			// specifying 8192 gets rid of an annoying warning message
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(newFile, false), 8192));
			
			out.print(filetext);
			
		} catch (FileNotFoundException ex) {
			log.e(ex,"unexpected exception");
			return false;
		} finally {
			if(out != null) {
				out.close();
			}
		}
		
		return true;
		
		
	}
	
	private static File getBaseDirectory() {
		File sdcardDir = Environment.getExternalStorageDirectory();
		
		File baseDir = new File(sdcardDir, "chord_reader_2");
		
		if(!baseDir.exists()) {
			baseDir.mkdir();
		}
		
		return baseDir;
		
	}

}
