package org.handmadeideas.chordreader.helper;

import android.os.Environment;
import org.handmadeideas.chordreader.util.UtilLogger;

import java.io.*;
import java.util.*;

public class SaveFileHelper {

	private static UtilLogger log = new UtilLogger(org.handmadeideas.chordreader.helper.SaveFileHelper.class);
	
	public static boolean checkIfSdCardExists() {
		
		File sdcardDir = Environment.getExternalStorageDirectory();
			
		return sdcardDir != null && sdcardDir.listFiles() != null;
		
	}
	
	public static boolean fileExists(String filename) {
		File catlogDir = getBaseDirectory();
		
		File file = new File(catlogDir, filename);
		
		return file.exists();
	}
	
	public static void deleteFile(String filename) {
		
		File catlogDir = getBaseDirectory();
		
		File file = new File(catlogDir, filename);
		
		if (file.exists()) {
			file.delete();
		}
		
	}
	
	public static Date getLastModifiedDate(String filename) {
		
		File catlogDir = getBaseDirectory();
		
		File file = new File(catlogDir, filename);
		
		if (file.exists()) {
			return new Date(file.lastModified());
		} else {
			// shouldn't happen
			log.e("file last modified date not found: %s", filename);
			return new Date();
		}
	}
	
	/**
	 * Get all the log filenames, order by last modified descending
	 * @return
	 */
	public static List<String> getSavedFilenames() {
		
		File baseDir = getBaseDirectory();
		
		File[] filesArray = baseDir.listFiles();
		
		if (filesArray == null) {
			return Collections.emptyList();
		}
		
		List<File> files = new ArrayList<File>(Arrays.asList(filesArray));
		
		Collections.sort(files, new Comparator<File>(){

			@Override
			public int compare(File object1, File object2) {
				return new Long(object2.lastModified()).compareTo(object1.lastModified());
			}});
		
		List<String> result = new ArrayList<String>();
		
		for (File file : files) {
			result.add(file.getName());
		}
		
		return result;
		
	}
	
	public static String openFile(String filename) {
		
		File baseDir = getBaseDirectory();
		File logFile = new File(baseDir, filename);	
		
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
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					log.e(e, "couldn't close buffered reader");
				}
			}
		}
		
		return result.toString();
	}
	
	public static boolean saveFile(String filetext, String filename) {
		
		File baseDir = getBaseDirectory();
		
		File newFile = new File(baseDir, filename);
		try {
			if (!newFile.exists()) {
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
			if (out != null) {
				out.close();
			}
		}
		
		return true;
		
		
	}
	
	private static File getBaseDirectory() {
		File sdcardDir = Environment.getExternalStorageDirectory();
		
		File baseDir = new File(sdcardDir, "chord_reader");
		
		if (!baseDir.exists()) {
			baseDir.mkdir();
		}
		
		return baseDir;
		
	}
	
}
