package org.hollowbamboo.chordreader2.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ChordReaderDBHelper extends SQLiteOpenHelper {


	// schema constants
	
	private static final String DB_NAME = "chord_reader.db";
	private static final int DB_VERSION = 1;
	
	// table constants
	private static final String TABLE_QUERY = "Queries";
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_QUERY_TEXT = "query";
	private static final String COLUMN_QUERY_TIMESTAMP = "timestamp";
	
	private static final String TABLE_TRANSPOSITIONS = "Transpositions";
	private static final String COLUMN_CAPO = "capo";
	private static final String COLUMN_TRANSPOSE = "transpose";
	private static final String COLUMN_FILENAME = "filename";

	private static final String TABLE_TEXTSIZES = "Textsizes";
	private static final String COLUMN_TEXTSIZE = "textSize";

	
	// private variables
	private final SQLiteDatabase db;
	
	public ChordReaderDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		db = getWritableDatabase();
	}
	

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String createFirstTable = "create table %s " +
			"(" +
			"%s integer not null primary key autoincrement, " +
			"%s text not null, " +
			"%s int not null " +
			");";
		
		createFirstTable = String.format(createFirstTable, TABLE_QUERY, COLUMN_ID, COLUMN_QUERY_TEXT, COLUMN_QUERY_TIMESTAMP);
		
		db.execSQL(createFirstTable);
		db.execSQL("create unique index index_query on " + TABLE_QUERY + " ( " + COLUMN_QUERY_TEXT + ");");
		
		String createSecondTable = "create table %s " +
			"(" +
			"%s integer not null primary key autoincrement, " +
			"%s text not null, " +
			"%s int not null, " +
			"%s int not null " +
			");";
		
		createSecondTable = String.format(createSecondTable, TABLE_TRANSPOSITIONS, COLUMN_ID, COLUMN_FILENAME, COLUMN_TRANSPOSE, COLUMN_CAPO);
		db.execSQL(createSecondTable);
		db.execSQL("create unique index index_filename on " + TABLE_TRANSPOSITIONS + " ( " + COLUMN_FILENAME + ");");

		String createThirdTable = "create table %s " +
				"(" +
				"%s integer not null primary key autoincrement, " +
				"%s text not null, " +
				"%s float not null " +
				");";

		createThirdTable = String.format(createThirdTable, TABLE_TEXTSIZES, COLUMN_ID, COLUMN_FILENAME, COLUMN_TEXTSIZE);
		db.execSQL(createThirdTable);
		db.execSQL("create unique index index_filename_textsizes on " + TABLE_TEXTSIZES + " ( " + COLUMN_FILENAME + ");");
	}
	

	@Override
	public void close() {
		super.close();
		if(db != null && db.isOpen()) { // just to be safe
			db.close();
		}
	}
	

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public Transposition findTranspositionByFilename(CharSequence filename) {
		synchronized (org.hollowbamboo.chordreader2.db.ChordReaderDBHelper.class) {
			Cursor cursor = null;
			try {
				cursor = db.query(TABLE_TRANSPOSITIONS, 
						new String[]{COLUMN_ID, COLUMN_FILENAME, COLUMN_TRANSPOSE, COLUMN_CAPO}, 
						COLUMN_FILENAME + "=?", 
						new String[]{filename.toString()}, 
						null, null, null);
				
				if(cursor.moveToNext()) {
					Transposition transposition = new Transposition();
					
					transposition.setId(cursor.getInt(0));
					transposition.setFilename(cursor.getString(1));
					transposition.setTranspose(cursor.getInt(2));
					transposition.setCapo(cursor.getInt(3));
					
					return transposition;
				} else {
					return null;
				}
			} finally {
				cursor.close();
			}
		}
	}
	
	public void saveTransposition(CharSequence filename, int transpose, int capo) {
		synchronized (org.hollowbamboo.chordreader2.db.ChordReaderDBHelper.class) {
						
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_CAPO, capo);
			contentValues.put(COLUMN_TRANSPOSE, transpose);
			
			int updated = db.update(TABLE_TRANSPOSITIONS, contentValues, COLUMN_FILENAME + "=?", new String[]{filename.toString()});
			
			if(updated == 0) { // needs to be inserted
				String sql = "insert into " + TABLE_TRANSPOSITIONS + " (" + COLUMN_FILENAME + "," + COLUMN_TRANSPOSE + ", " + COLUMN_CAPO
					+ ") values (?," + transpose + "," + capo + ");";
			
				db.execSQL(sql, new String[]{filename.toString()});				
			}
		}
	}

	public float findTextSizeByFilename(CharSequence filename) {
		synchronized (org.hollowbamboo.chordreader2.db.ChordReaderDBHelper.class) {
			Cursor cursor = null;
			try {
				cursor = db.query(TABLE_TEXTSIZES,
						new String[]{COLUMN_ID, COLUMN_FILENAME, COLUMN_TEXTSIZE},
						COLUMN_FILENAME + "=?",
						new String[]{filename.toString()},
						null, null, null);

				if(cursor.moveToNext()) {
					Log.d("ChordReaderDBHelper"," - " + cursor.getFloat(2));
					return cursor.getFloat(2);
				} else {
					return 0;
				}

			} catch (Exception exception) {
				return 0;
			} finally {
				if(cursor != null)
					cursor.close();
			}
		}
	}

	public void saveTextSize(CharSequence filename, float textSize) {
		synchronized (org.hollowbamboo.chordreader2.db.ChordReaderDBHelper.class) {

			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_TEXTSIZE, textSize);

			int updated = db.update(TABLE_TEXTSIZES, contentValues, COLUMN_FILENAME + "=?", new String[]{filename.toString()});

			if(updated == 0) { // needs to be inserted
				String sql = "insert into " + TABLE_TEXTSIZES + " (" + COLUMN_FILENAME + "," + COLUMN_TEXTSIZE
						+ ") values (?," + textSize + ");";

				db.execSQL(sql, new String[]{filename.toString()});
			}
		}
	}
	
	
	public List<String> findAllQueries(long timestamp, CharSequence prefix) {
		synchronized (org.hollowbamboo.chordreader2.db.ChordReaderDBHelper.class) {
			Cursor cursor = null;
			List<String> result = new ArrayList<String>();
			try {
				cursor = db.query(
							TABLE_QUERY, 
							new String[]{COLUMN_ID, COLUMN_QUERY_TEXT}, 
							COLUMN_QUERY_TIMESTAMP + ">" + timestamp + " and " + COLUMN_QUERY_TEXT +" like ?", 
							new String[]{prefix + "%"}, 
							null, 
							null, 
							COLUMN_QUERY_TIMESTAMP + " desc");
					
				while (cursor.moveToNext()) {
					result.add(cursor.getString(1));
				}
				
				return result;
			} finally {
				if(cursor != null) {
					cursor.close();
				}
			}
		}

	}
	
	/**
	 * Return true if a new query was saved
	 * @param queryText
	 * @return
	 */
	public boolean saveQuery(String queryText) {
		synchronized (org.hollowbamboo.chordreader2.db.ChordReaderDBHelper.class) {
			
			long currentTime = System.currentTimeMillis();
			
			ContentValues contentValues = new ContentValues();
			contentValues.put(COLUMN_QUERY_TIMESTAMP, currentTime);
			int updated = db.update(TABLE_QUERY, contentValues, COLUMN_QUERY_TEXT + "=?", new String[]{queryText});
			
			if(updated == 0) { // needs to be inserted
				String insertSql = "insert into " + TABLE_QUERY + " (" + COLUMN_QUERY_TEXT + ", " + COLUMN_QUERY_TIMESTAMP
					+ ") values (?," + currentTime + ");";
				
				db.execSQL(insertSql, new String[]{queryText});	
				return true;
			}
			return false;
		}
	}
}
