package org.hollowbamboo.chordreader2.helper;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import org.hollowbamboo.chordreader2.util.UtilLogger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SaveFileHelper {

	private static UtilLogger log = new UtilLogger(SaveFileHelper.class);
	
	public static boolean checkIfSdCardExists(Context context) {

		if (SDK_INT < Build.VERSION_CODES.O) {
			File sdcardDir = Environment.getExternalStorageDirectory();

			return sdcardDir != null && sdcardDir.listFiles() != null;
		} else {
			return Objects.requireNonNull(DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context))).exists();
		}
	}
	
	public static boolean fileExists(Context context, String filename) {
		if (SDK_INT < Build.VERSION_CODES.O) {
			File catlogDir = getBaseDirectory();
			File file = new File(catlogDir, filename);
			return file.exists();
		} else {
			DocumentFile file = Objects.requireNonNull(DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context))).findFile(filename);
			return file != null && file.exists();

		}
	}
	
	public static void deleteFile(Context context,String filename) {
		if (SDK_INT < Build.VERSION_CODES.O) {

			File catlogDir = getBaseDirectory();
			File file = new File(catlogDir, filename);

			if(file.exists())
				file.delete();

		} else {
			DocumentFile file = Objects.requireNonNull(DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context))).findFile(filename);

			if(file != null && file.exists())
				file.delete();
		}
	}
	
	public static Date getLastModifiedDate(String filename) {

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

	public static List<String> getSavedSongNames(Context context) {
		List<String> fileNames = getFilenamesInBaseDirectory(context);

//		Collections.sort(tempArrayList, new Comparator<File>() {
//
//			@Override
//			public int compare(File object1, File object2) {
//				return new Long(object2.lastModified()).compareTo(object1.lastModified());
//			}
//		});

		List<String> result = new ArrayList<String>();

		for (String file : fileNames) {
			if(file.endsWith(".txt"))
				result.add(file.replace(".txt", ""));
		}
		return result;
	}


	public static List<String> getSavedSetListNames(Context context) {
		List<String> fileNames = getFilenamesInBaseDirectory(context);


//		Collections.sort(tempArrayList, new Comparator<File>() {
//
//			@Override
//			public int compare(File object1, File object2) {
//				return new Long(object2.lastModified()).compareTo(object1.lastModified());
//			}
//		});

		List<String> result = new ArrayList<String>();

		for (String file : fileNames) {
			if(file.endsWith(".pl"))
				result.add(file.replace(".pl", ""));
		}

		return result;
	}

	public static List<String> getFilenamesInBaseDirectory(Context context) {

		List<String> result = new ArrayList<>();

		if (SDK_INT < Build.VERSION_CODES.O) {
			File baseDir = getBaseDirectory();
			File[] filesArray = baseDir.listFiles();

			if (filesArray != null) {
				for (File file : filesArray) {
					String fileName = file.getName();
					result.add(fileName);
				}
			}
		} else {
			DocumentFile documentFile = DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context));
			DocumentFile[] documentFiles = new DocumentFile[0];
			if (documentFile != null) {
				documentFiles = documentFile.listFiles();
			}

			for (DocumentFile d : documentFiles) {
				result.add(d.getName());
			}
		}
		
		if(result.isEmpty()) {
			return Collections.emptyList();
		}

		return result;
	}

	public static boolean isInvalidFilename(CharSequence filename) {

		String filenameAsString;

		return TextUtils.isEmpty(filename)
				|| (filenameAsString = filename.toString()).contains("/")
				|| filenameAsString.contains(":")
				|| filenameAsString.contains("\\")
				|| filenameAsString.contains("*")
				|| filenameAsString.contains("|")
				|| filenameAsString.contains("<")
				|| filenameAsString.contains(">")
				|| filenameAsString.contains("?");

	}

	public static String openFile(Context context, String filename) {

		BufferedReader bufferedReader = null;
		InputStream inputStream = null;

		if (SDK_INT < Build.VERSION_CODES.O) {
			File baseDir = getBaseDirectory();
			File logFile;

			if (!(filename == null))
				logFile = new File(baseDir, filename);
			else
				return "";

			try {
				bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
			} catch (IOException ex) {
				log.e(ex, "couldn't read file");
			}
		} else {

			DocumentFile logFile, documentFile;

			try {
				documentFile = DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context));
			} catch (Exception e) {
				return "";
			}

			if (!(documentFile == null))
				logFile = documentFile.findFile(filename);
			else
				return "";

			try {
				if (!(logFile == null))
					inputStream = context.getContentResolver().openInputStream(logFile.getUri());
				else
					return "";

				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			} catch (IOException ex) {
				log.e(ex, "couldn't read file");
			}
		}
		
		StringBuilder result = new StringBuilder();
		
		try {
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

	public static List<String> openSetList(Context context, String filename) {

		if (!filename.endsWith(".pl"))
			filename = filename.concat(".pl");

		String fileText = openFile(context,filename);

		ArrayList<String> filesList = new ArrayList<>();

		for (String file : fileText.split("\n")) {
			if (fileExists(context, file))
				filesList.add(file.replace(".txt", ""));
		}

		if (filesList.size() == 1 && filesList.get(0).equals(""))
			filesList.remove(0);
		return filesList;
	}
	
	public static boolean saveFile(Context context, String fileText, String filename) {

		if (SDK_INT < Build.VERSION_CODES.O) {

			File baseDir = getBaseDirectory();

			File newFile = new File(baseDir, filename);

			try {
				if (!newFile.exists())
					newFile.createNewFile();
			} catch (IOException ex) {
				log.e(ex, "couldn't create new file");
				return false;
			}

			PrintStream out = null;

			try {
				// specifying 8192 gets rid of an annoying warning message
				out = new PrintStream(new BufferedOutputStream(new FileOutputStream(newFile, false), 8192));

				out.print(fileText);

			} catch (FileNotFoundException ex) {
				log.e(ex,"unexpected exception");
				return false;
			} finally {
				if(out != null) {
					out.close();
				}
			}

		} else {

			Uri uri;

			try {
				DocumentFile documentFile = DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context));

				DocumentFile logFile = documentFile.findFile(filename);

				if (logFile == null)
					logFile = documentFile.createFile("application/txt",filename);

				uri = logFile.getUri();

			} catch (Exception e) {
				log.e(e, "couldn't create new file");
				return false;
			}

			try {
				ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri,"w");

				FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

				fileOutputStream.write(fileText.getBytes(StandardCharsets.UTF_8));

				fileOutputStream.close();
				parcelFileDescriptor.close();
			} catch (Exception ex) {
				log.e(ex,"couldn't write to file");
				return false;
			}
		}

		return true;
	}
	
	public static File getBaseDirectory() {

		File sdcardDir = Environment.getExternalStorageDirectory();
		
		File baseDir = new File(sdcardDir, "chord_reader_2");
		
		if(!baseDir.exists()) {
			baseDir.mkdir();
		}
		
		return baseDir;
		
	}

}
