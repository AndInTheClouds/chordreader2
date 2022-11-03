package org.hollowbamboo.chordreader2.ui;

/*
Chord Reader 2 - fetch and display chords for your favorite songs from the Internet
Copyright (C) 2021 AndInTheClouds

This program is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License as published by the Free Software Foundation, either
version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.
If not, see <https://www.gnu.org/licenses/>.

*/

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.adapter.SelectableFilterAdapter;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.databinding.FragmentListBinding;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.helper.SaveFileHelper;
import org.hollowbamboo.chordreader2.model.DataViewModel;
import org.hollowbamboo.chordreader2.model.ListFragmentViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ListFragment extends Fragment implements TextWatcher {

    private static final String MODE_PLAYLIST = "Playlists";
    private static final String MODE_SONGS = "Songs";
    private static final String MODE_PLAYLIST_SONG_SELECTION = "PlaylistSongsSelection";

    private ConstraintLayout songListMainView;
    private EditText filterEditText;
    private ImageButton deleteFilterTextButton;
    private Button searchWebButton;
    private Button okButton;
    private ListView fileList;
    private TextView textView;
    private int foregroundColor;

    private SelectableFilterAdapter fileListAdapter;
    private static Menu menu;

    private DataViewModel dataViewModel;
    private FragmentListBinding binding;

    private boolean IsSelectionModeActive;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        dataViewModel =
                new ViewModelProvider(requireActivity()).get(DataViewModel.class);

        binding = FragmentListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        handleBackButton();

        fileList = binding.fileList;
        fileList.setTextFilterEnabled(true);

        songListMainView = binding.songListMainView;

        filterEditText = binding.listFilterTextView;
        deleteFilterTextButton = binding.deleteFilterTextButton;

        searchWebButton = binding.searchTheWebButton;
        searchWebButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                  startWebView();
            }
        });

        textView = binding.listViewTextView;

        okButton = binding.listViewOkBtn;

        setUpInstance();

        setUpMenu();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {

        super.onResume();

        applyColorScheme();

        //restore filter
        String filterText = filterEditText.getText().toString();
        if(!(filterEditText.getText().toString().equals("")))
            this.fileListAdapter.getFilter().filter(filterText);

    }

    @Override
    public void onPause() {
        super.onPause();

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(filterEditText.getWindowToken(), 0);
    }

    @Override
    public void onStop() {
        super.onStop();

        if(Objects.equals(dataViewModel.mode, MODE_PLAYLIST_SONG_SELECTION))
            dataViewModel.setPlaylistSongs(fileListAdapter.getSelectedFiles());
    }

    private void setUpMenu() {
        MenuHost menuHost = requireActivity();
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.list_view_menu, menu);

                ListFragment.menu = menu;

                if(!Objects.equals(dataViewModel.mode, MODE_PLAYLIST_SONG_SELECTION)) {
                    menu.findItem(R.id.menu_new_file).setVisible(true);
                    menu.findItem(R.id.menu_manage_files).setVisible(true);
                }

                if(fileListAdapter == null)
                    menu.findItem(R.id.menu_manage_files).setVisible(false);
                else if (fileListAdapter.isEmpty())
                    menu.findItem(R.id.menu_manage_files).setVisible(false);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();

                if(itemId == R.id.menu_manage_files) {
                    startSelectionMode();
                    return true;
                } else if(itemId == R.id.menu_new_file) {
                    if(!(dataViewModel.mode.equals(MODE_PLAYLIST)))
                        startSongView(null);
                    else
                        newPlaylistDialog();

                    return true;
                } else if(itemId == R.id.menu_cancel_selection) {
                    cancelSelectionMode();
                    return true;
                } else if(itemId == R.id.menu_select_all) {
                    fileListAdapter.selectAll();
                    return true;
                } else if(itemId == R.id.menu_delete) {
                    verifyDelete();
                    return true;
                }

                return false;
            }
        };

        menuHost.addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void applyColorScheme() {
        //apply color scheme
        ColorScheme colorScheme = PreferenceHelper.getColorScheme(requireContext());
        songListMainView.setBackgroundColor(colorScheme.getBackgroundColor(requireContext()));
        foregroundColor = colorScheme.getForegroundColor(requireContext());
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{0, foregroundColor});
        gradientDrawable.setAlpha(120);
        fileList.setDivider(gradientDrawable);
        fileList.setDividerHeight(1);

    }

    private void newPlaylistDialog() {

        if(!checkSdCard()) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        final EditText editText = new EditText(requireContext());
        editText.setSingleLine();
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (v.requestFocus())
                    editText.post(new Runnable() {
                        @Override
                        public void run() {
                            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                else
                    imm.showSoftInput(v, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        editText.setText(R.string.new_playlist);

        editText.setSelection(0, getString(R.string.new_playlist).length());

        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {

            if(SaveFileHelper.isInvalidFilename(editText.getText())) {
                Toast.makeText(getActivity(), getResources().getString(R.string.enter_good_filename), Toast.LENGTH_SHORT).show();
            } else {
                String playlistFileName = editText.getText().toString().concat(".pl");
                editText.clearFocus();

                if(SaveFileHelper.fileExists(playlistFileName)) {

                    new AlertDialog.Builder(requireContext())
                            .setCancelable(true)
                            .setTitle(R.string.overwrite_file_title)
                            .setMessage(R.string.overwrite_file)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                                SaveFileHelper.saveFile("", playlistFileName);
                                startPlaylistList(playlistFileName);
                            })
                            .show();

                } else {
                    SaveFileHelper.saveFile("", playlistFileName);
                    startPlaylistList(playlistFileName);
                }

            }

            dialog.dismiss();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle(R.string.save_file)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editText.clearFocus();
                    }
                })
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setMessage(R.string.enter_filename)
                .setView(editText);

        builder.show();
        editText.requestFocus();
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
        deleteFilterTextButton.setVisibility(TextUtils.isEmpty(editable) ? View.GONE : View.VISIBLE);
    }

    private void handleBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(IsSelectionModeActive && !dataViewModel.mode.equals(MODE_PLAYLIST_SONG_SELECTION)) {
                    cancelSelectionMode();
                } else
                    Navigation.findNavController(getParentFragment().requireView()).popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

    }

    protected void setUpInstance() {

        dataViewModel.mode = ListFragmentArgs.fromBundle(getArguments()).getMode();

        if(dataViewModel.mode.equals(MODE_SONGS))
            dataViewModel.resetData();

        if(!checkSdCard()) {
            return;
        }

        // Open files depending on listFragment mode
        List<String> filenames = new ArrayList<>(SaveFileHelper.getSavedSongNames());

        switch (dataViewModel.mode) {
            case MODE_SONGS:
                setTitle("Songs");
                textView.setText(R.string.no_local_songs);
                break;
            case MODE_PLAYLIST:
                setTitle("Setlists");
                filenames = new ArrayList<>(SaveFileHelper.getSavedPlayListNames());
                textView.setText(R.string.no_playlists);
                break;
            case MODE_PLAYLIST_SONG_SELECTION:
                setTitle(getString(R.string.file_selection_for_playlist));

                okButton.setVisibility(View.VISIBLE);
                okButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        Navigation.findNavController(getParentFragment().requireView()).popBackStack();
                    }
                });

                break;
        }

        Collections.sort(filenames, (Comparator<CharSequence>) (first, second) -> first.toString().toLowerCase().compareTo(second.toString().toLowerCase()));

        fileListAdapter = new SelectableFilterAdapter(requireContext(), filenames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(foregroundColor);
                return textView;
            }
        };

        fileListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                if (dataViewModel.mode.equals(MODE_SONGS)) {
                    searchWebButton.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);
                    textView.setText(R.string.no_local_songs);
                    textView.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);
                }

                if (dataViewModel.mode.equals(MODE_PLAYLIST)) {
                    textView.setText(R.string.no_playlists);
                    textView.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);
                }

                if (!IsSelectionModeActive)
                    menu.findItem(R.id.menu_manage_files).setVisible(fileListAdapter.getCount() != 0);
            }
        });

        fileList.setAdapter(fileListAdapter);

        //initial empty list
        if(dataViewModel.mode.equals(MODE_SONGS))
        searchWebButton.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);
        textView.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);

        if(dataViewModel.mode.equals(MODE_PLAYLIST_SONG_SELECTION))
            setFileSelection();

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String filename = (String) adapterView.getAdapter().getItem(i);
                view.setBackgroundColor(Color.GRAY);

                if(IsSelectionModeActive) {
                    fileListAdapter.switchSelectionForIndex(i);
                } else
                    switch (dataViewModel.mode) {
                        case MODE_SONGS:
                            startSongView(filename);
                            break;
                        case MODE_PLAYLIST:
                            startPlaylistList(filename);
                            break;
                    }
            }
        });

        fileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long arg3) {
                startSelectionMode();
                fileListAdapter.switchSelectionForIndex(pos);
                return true;
            }
        });

        filterEditText.addTextChangedListener(this);

        deleteFilterTextButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                filterEditText.setText("");
                return true;
            }
        });
    }

    private void setTitle(String titleText) {
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(titleText);
    }

    private void setFileSelection() {
        IsSelectionModeActive = true;
        fileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        for (String filename : dataViewModel.playlistSongs) {
            String s = filename.replace(".txt","");
            int index = fileListAdapter.getIndexOfFile(s);
            fileListAdapter.switchSelectionForIndex(index);
        }
    }

    private void startSelectionMode() {
        IsSelectionModeActive = true;
        fileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        menu.findItem(R.id.menu_manage_files).setVisible(false);
        menu.findItem(R.id.menu_new_file).setVisible(false);
        menu.findItem(R.id.menu_delete).setVisible(true);
        menu.findItem(R.id.menu_cancel_selection).setVisible(true);
        menu.findItem(R.id.menu_select_all).setVisible(true);
    }


    private void cancelSelectionMode() {
        IsSelectionModeActive = false;
        fileList.setChoiceMode(ListView.CHOICE_MODE_NONE);

        menu.findItem(R.id.menu_manage_files).setVisible(true);
        menu.findItem(R.id.menu_new_file).setVisible(true);
        menu.findItem(R.id.menu_delete).setVisible(false);
        menu.findItem(R.id.menu_cancel_selection).setVisible(false);
        menu.findItem(R.id.menu_select_all).setVisible(false);

        fileListAdapter.unselectAll();
    }

    private boolean checkSdCard() {

        boolean result = SaveFileHelper.checkIfSdCardExists();

        if(!result) {
            Toast.makeText(getActivity(), getResources().getString(R.string.sd_card_not_found), Toast.LENGTH_SHORT).show();
        }
        return result;
    }


    protected void verifyDelete() {

        if(!checkSdCard()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        final ArrayList<String> filenameArray = fileListAdapter.getSelectedFiles();
        Log.d("ListFragment", filenameArray.toString());
        final int finalDeleteCount = filenameArray.size();

        if(finalDeleteCount > 0) {

            builder.setTitle(R.string.delete_saved_file)
                    .setCancelable(true)
                    .setMessage(String.format(getText(R.string.are_you_sure).toString(), finalDeleteCount))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // ok, delete

                        for (String s : filenameArray) {
                            String fileName = "";
                            if(Objects.equals(dataViewModel.mode, MODE_SONGS))
                                fileName = s.concat(".txt");
                            else if(Objects.equals(dataViewModel.mode, MODE_PLAYLIST))
                                fileName = s.concat(".pl");
                            SaveFileHelper.deleteFile(fileName);
                        }
                        setUpInstance();

                        String toastText = String.format(getText(R.string.files_deleted).toString(), finalDeleteCount);
                        Toast.makeText(getActivity(),toastText, Toast.LENGTH_SHORT).show();

                        cancelSelectionMode();
                        dialog.dismiss();

                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    private void startPlaylistList(String playlist) {
        dataViewModel.setPlaylistMLD(playlist);

        Navigation.findNavController(getParentFragment().getView()).navigate(R.id.nav_drag_list_view);
    }

    private void startSongView(String filename) {
        String songTitle = null;
        if(filename == null)
            songTitle = getString(R.string.new_file);
        ListFragmentDirections.ActionNavListFragmentToNavSongView action =
                ListFragmentDirections.actionNavListFragmentToNavSongView(songTitle, filename,null);

        Navigation.findNavController(getParentFragment().getView()).navigate(action);
    }

    private void startWebView() {
        String searchText = filterEditText.getText().toString();
         ListFragmentDirections.ActionNavListFragmentToNavWebSearch action =
                ListFragmentDirections.actionNavListFragmentToNavWebSearch(searchText);

        Navigation.findNavController(getParentFragment().getView()).navigate(action);


    }

}