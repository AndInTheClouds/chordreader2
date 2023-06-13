package org.hollowbamboo.chordreader2.helper;

import static android.os.Build.VERSION.SDK_INT;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SaveFileHelper {

    private static final UtilLogger log = new UtilLogger(SaveFileHelper.class);

    public static boolean checkIfSdCardExists() {

        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean fileExists(Context context, String filename) {
        if (SDK_INT < Build.VERSION_CODES.O) {
            File catalogDir = getBaseDirectory();
            File file = new File(catalogDir, filename);
            return file.exists();
        } else {
            ArrayList<String> fileNames = new ArrayList<>();
            fileNames.add(filename);

            ArrayList<Uri> fileUris = getFileUri(context, fileNames);
            Uri fileUri = fileUris.isEmpty() ? null : fileUris.get(0);

            if(fileUri != null) {
                InputStream inputStream = null;
                try {
                    inputStream = context.getContentResolver().openInputStream(fileUri);

                    return true;
                } catch (Exception e) {
                    // file does not exist
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return false;
        }
    }

    public static ArrayList<String> getExistingFiles(Context context, String[] fileNames) {
        if (SDK_INT < Build.VERSION_CODES.O) {
            File catalogDir = getBaseDirectory();

            ArrayList<String> existingFileNames = new ArrayList<>();

            for (String filename : fileNames) {
                File file = new File(catalogDir, filename);

                if (file.exists())
                    existingFileNames.add(filename);
            }

            return existingFileNames;
        } else {
            ArrayList<String> existingFileNames = new ArrayList<>();

            ArrayList<Uri> fileUris = getFileUri(context, Arrays.asList(fileNames));

            for (Uri uri : fileUris) {
                DocumentFile file = DocumentFile.fromSingleUri(context,uri);
                if (file != null) {
                    existingFileNames.add(file.getName());
                }
            }

            return existingFileNames;
        }
    }

    public static void deleteFile(Context context, String filename) {
        if (SDK_INT < Build.VERSION_CODES.O) {

            File catalogDir = getBaseDirectory();
            File file = new File(catalogDir, filename);

            if (file.exists())
                file.delete();

        } else {
            ArrayList<String> fileNames = new ArrayList<>();
            fileNames.add(filename);

            Uri fileUri = getFileUri(context, fileNames).get(0);

            DocumentFile file = null;

            if (fileUri != null)
                file = DocumentFile.fromSingleUri(
                        context,
                        fileUri);

            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    public static String[] getSavedFileNames(Context context, String fileExtension) {

        List<String> fileNames = getFilenamesInBaseDirectory(context);

        List<String> result = new ArrayList<>();

        for (String file : fileNames) {
            if (file.endsWith(fileExtension))
                result.add(file.replace(fileExtension, ""));
        }

        return result.toArray(new String[0]);
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

            if (documentFile != null) {
                ContentResolver contentResolver = context.getContentResolver();
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(documentFile.getUri(),
                        DocumentsContract.getDocumentId(documentFile.getUri()));
                Cursor cursor = null;

                try {
                    cursor = contentResolver.query(childrenUri, new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    }, null, null, null);

                    while (cursor.moveToNext()) {
                        final String fileName = cursor.getString(1);

                        result.add(fileName);
                    }

                } catch (Exception e) {
                    Log.w("SaveFileHelper_ListFile", "Failed query get all file names: " + e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        if (result.isEmpty())
            return new ArrayList<>();

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
                log.e(ex, "couldn't read file, file opening error");
            }
        } else {
            ArrayList<String> fileNames = new ArrayList<>();
            fileNames.add(filename);

            ArrayList<Uri> fileUris = getFileUri(context, fileNames);
            if (fileUris.isEmpty())
                return "";

            Uri fileUri = fileUris.get(0);

			FileInputStream fileInputStream;

            try {
                if (fileUri != null) {
                    ParcelFileDescriptor inputPFD = context.getContentResolver().openFileDescriptor(fileUri, "r");

					fileInputStream = new FileInputStream(inputPFD.getFileDescriptor());

                } else
                    return "";

                bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            } catch (IOException ex) {
                log.e(ex, "couldn't read file, file opening error");
            }
        }

        StringBuilder result = new StringBuilder();

        try {
            if (bufferedReader != null) {
                while (bufferedReader.ready()) {
                    result.append(bufferedReader.readLine()).append("\n");
                }
            }
        } catch (IOException ex) {
            log.e(ex, "couldn't read file, file reading error");

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

    public static List<String> openSetlist(Context context, String setlist) {

        final String[][] tempList2 = {new String[0]};

        // do in background to avoid jankiness
        final CountDownLatch latch = new CountDownLatch(1);

        HandlerThread handlerThread = new HandlerThread("OpenSetlistHandlerThread");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                tempList2[0] = (String[]) msg.obj;

                latch.countDown();

                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            // your async code goes here.
            Message message = new Message();

            String setlistContent  = SaveFileHelper.openFile(context, setlist);

            if (setlistContent.length() == 0) {
                message.obj = new String[0];
                asyncHandler.sendMessage(message);
                return;
            }

            String[] files = setlistContent.split("\n");

            ArrayList<String> existingFiles = SaveFileHelper.getExistingFiles(context, files);

            ArrayList<String> tempList = new ArrayList<>();

            for (String file : existingFiles) {
                tempList.add(file.replace(".txt", ""));
            }

            if (tempList.size() == 1 && tempList.get(0).equals(""))
                tempList.remove(0);

            String[] stringArray = new String[tempList.size()];
            tempList.toArray(stringArray);
            message.obj = stringArray;

            asyncHandler.sendMessage(message);
        };

        asyncHandler.post(runnable);

        // wait for async opening result
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ArrayList<String> fileList = new ArrayList<>(Arrays.asList(tempList2[0]));

        return fileList.isEmpty() ? new ArrayList<>() : fileList;
    }

    public static boolean saveFile(Context context, String fileText, String filename) {

        // do in background to avoid jankiness
        final boolean[] successfullySavedLog = new boolean[1];
        final CountDownLatch latch = new CountDownLatch(1);

        HandlerThread handlerThread = new HandlerThread("SaveFileHandlerThread");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                successfullySavedLog[0] = (boolean) msg.obj;

                latch.countDown();

                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            // your async code goes here.
            Message message = new Message();
            message.obj = doSaving(context, fileText, filename);

            asyncHandler.sendMessage(message);
        };

        asyncHandler.post(runnable);

        // wait for async saving result
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return successfullySavedLog[0];
    }

    private static boolean doSaving(Context context, String fileText, String filename) {

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
                log.e(ex, "unexpected exception");
                return false;
            } finally {
                if (out != null) {
                    out.close();
                }
            }

        } else {

            Uri fileUri;

            try {
                DocumentFile documentFile = DocumentFile.fromTreeUri(
                        context,
                        PreferenceHelper.getStorageLocation(context)
                );

                ArrayList<String> fileNames = new ArrayList<>();
                fileNames.add(filename);

                ArrayList<Uri> fileUris = getFileUri(context, fileNames);
                fileUri = fileUris.isEmpty() ? null : fileUris.get(0);

                if (fileUri == null) {
                    assert documentFile != null;
                    fileUri = DocumentsContract.createDocument(
                            context.getContentResolver(),
                            documentFile.getUri(),
                            "application/txt",
                            filename
                    );
                }

            } catch (Exception e) {
                log.e(e, "couldn't create new file");
                return false;
            }

            try {
                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver()
                        .openFileDescriptor(fileUri, "wt");

                FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                fileOutputStream.write(fileText.getBytes(StandardCharsets.UTF_8));

                fileOutputStream.close();

                parcelFileDescriptor.close();
            } catch (Exception ex) {
                log.e(ex, "couldn't write to file");
                return false;
            }
        }

        return true;
    }

    private static File getBaseDirectory() {

        File sdcardDir = Environment.getExternalStorageDirectory();

        File baseDir = new File(sdcardDir, "chord_reader_2");

        if (!baseDir.exists()) {
            baseDir.mkdir();
        }

        return baseDir;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static ArrayList<Uri> getFileUri(Context context, List<String> requestedFileNames) {

        DocumentFile documentFile = DocumentFile.fromTreeUri(context, PreferenceHelper.getStorageLocation(context));

        if (documentFile != null) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    PreferenceHelper.getStorageLocation(context),
                    DocumentsContract.getDocumentId(documentFile.getUri())
            );

            Cursor cursor = null;

            try {
                cursor = contentResolver.query(childrenUri, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                }, null, null, null);

                Uri[] existingFilesUris = new Uri[requestedFileNames.size()];

                while (cursor.moveToNext()) {
                    final String fileName = cursor.getString(1);

                    if (requestedFileNames.contains(fileName)) {
                        final String documentId = cursor.getString(0);

                        final Uri documentUri =
                                DocumentsContract.buildDocumentUriUsingTree(
                                        PreferenceHelper.getStorageLocation(context), documentId);

                        int index = requestedFileNames.indexOf(fileName);
                        existingFilesUris[index] = documentUri;
                    }
                }

                return new ArrayList<>(Arrays.asList(existingFilesUris));

            } catch (Exception e) {
                Log.w("SaveFileHelper_getUri", "Failed query: " + e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return new ArrayList<>();
    }
}
