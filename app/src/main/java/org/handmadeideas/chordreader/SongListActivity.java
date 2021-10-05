package org.handmadeideas.chordreader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.handmadeideas.chordreader.adapter.FileAdapter;
import org.handmadeideas.chordreader.adapter.SearchableAdapter;
import org.handmadeideas.chordreader.data.ColorScheme;
import org.handmadeideas.chordreader.helper.PreferenceHelper;
import org.handmadeideas.chordreader.helper.SaveFileHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SongListActivity extends DrawerBaseActivity implements TextWatcher {

    private LinearLayout songListMainView;
    private EditText filterEditText;
    private ListView fileList;
    SearchableAdapter fileListAdapter;

    private int indexCurrentPosition, top;
    //private static UtilLogger log = new UtilLogger(SongListActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_song_list);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_song_list, null, false);
        mDrawerLayout.addView(contentView, 0);

        fileList = (ListView) findViewById(R.id.file_list);
        fileList.setTextFilterEnabled(true);

        songListMainView = (LinearLayout) findViewById(R.id.song_list_main_view);

        filterEditText = (EditText) findViewById(R.id.song_list_filter_text_view);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // save index and top position
        indexCurrentPosition = fileList.getFirstVisiblePosition();
        View v = fileList.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - fileList.getPaddingTop());
    }

        @Override
    protected void onResume() {
        super.onResume();
        setUpWidgets();

        //Update drawer selection
        super.selectItem(2);


        // restore index and position
        fileList.setSelectionFromTop(indexCurrentPosition, top);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.song_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_manage_files) {
            startDeleteSavedFilesDialog();
            return true;
        } else if (itemId == R.id.menu_new_file) {
            startChordViewActivity(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // came back from the settings activity; need to update the colours
        PreferenceHelper.clearCache();

        setUpWidgets();
    }
    
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        //do nothing
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        this.fileListAdapter.getFilter().filter(charSequence);
    }

    @Override
    public void afterTextChanged(Editable editable) {
        //do nothing
    }

    private void setUpWidgets() {

        //apply color scheme
        ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);
        songListMainView.setBackgroundColor(colorScheme.getBackgroundColor(this));
        final int foregroundColor = colorScheme.getForegroundColor(this);
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{0,foregroundColor});
        gradientDrawable.setAlpha(120);
        fileList.setDivider(gradientDrawable);
        fileList.setDividerHeight(1);

        if (!checkSdCard()) {
            return;
        }

        final List<String> filenames = new ArrayList<>(SaveFileHelper.getSavedFilenames());

        if (filenames.isEmpty()) {
            return;
        }

        Collections.sort(filenames, (Comparator<CharSequence>) (first, second) -> first.toString().toLowerCase().compareTo(second.toString().toLowerCase()));

        fileListAdapter = new SearchableAdapter(this, filenames){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(foregroundColor);
                return textView;
            }
        };

        fileList.setAdapter(fileListAdapter);

        fileList.setOnItemClickListener((adapterView, view, i, l) -> {
            String filename = (String) adapterView.getAdapter().getItem(i);
            startChordViewActivity(filename);
        });

        filterEditText.addTextChangedListener(this);
    }

    private boolean checkSdCard() {

        boolean result = SaveFileHelper.checkIfSdCardExists();

        if (!result) {
            super.showToastLong(getResources().getString(R.string.sd_card_not_found));
        }
        return result;
    }

    private void startDeleteSavedFilesDialog() {

        if (!checkSdCard()) {
            return;
        }

        List<CharSequence> filenames = new ArrayList<>(SaveFileHelper.getSavedFilenames());

        if (filenames.isEmpty()) {
            super.showToastShort(getResources().getString(R.string.no_saved_files));
            return;
        }

        final CharSequence[] filenameArray = filenames.toArray(new CharSequence[0]);

        final FileAdapter dropdownAdapter = new FileAdapter(
                this, filenames, -1, true);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.select_files_to_delete)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.delete_all, (dialog, which) -> {
                    boolean[] allChecked = new boolean[dropdownAdapter.getCount()];

                    Arrays.fill(allChecked, true);
                    verifyDelete(filenameArray, allChecked, dialog);

                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> verifyDelete(filenameArray, dropdownAdapter.getCheckedItems(), dialog))
//                .setView(messageTextView)
                .setSingleChoiceItems(dropdownAdapter, 0, (dialog, which) -> dropdownAdapter.checkOrUncheck(which));

        builder.show();

    }


    protected void verifyDelete(final CharSequence[] filenameArray,
                                final boolean[] checkedItems, final DialogInterface parentDialog) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int deleteCount = 0;

        for (boolean checkedItem : checkedItems) {
            if (checkedItem) {
                deleteCount++;
            }
        }
        final int finalDeleteCount = deleteCount;

        if (finalDeleteCount > 0) {

            builder.setTitle(R.string.delete_saved_file)
                    .setCancelable(true)
                    .setMessage(String.format(getText(R.string.are_you_sure).toString(), finalDeleteCount))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // ok, delete

                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                SaveFileHelper.deleteFile(filenameArray[i].toString());
                            }
                        }
                        setUpWidgets();

                        String toastText = String.format(getText(R.string.files_deleted).toString(), finalDeleteCount);
                        super.showToastShort(toastText);

                        dialog.dismiss();
                        parentDialog.dismiss();

                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    private void startChordViewActivity(String filename) {
        Intent intent = new Intent(this, SongViewActivity.class);
        Bundle b = new Bundle();
        b.putString("filename",filename);
        intent.putExtras(b);
        startActivity(intent);
    }
}