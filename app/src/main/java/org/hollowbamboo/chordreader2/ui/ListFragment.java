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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.adapter.SelectableFilterAdapter;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.databinding.FragmentListBinding;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;
import org.hollowbamboo.chordreader2.helper.SaveFileHelper;
import org.hollowbamboo.chordreader2.model.DataViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class ListFragment extends Fragment implements TextWatcher {

    private static final String MODE_SETLIST = "Setlists";
    private static final String MODE_SONGS = "Songs";
    private static final String MODE_SETLIST_SONG_SELECTION = "SetlistSongsSelection";
    private String fileExtension;

    private ConstraintLayout songListMainView;
    private EditText filterEditText;
    private ImageButton deleteFilterTextButton;
    private Button searchWebButton;
    private Button okButton;
    private ListView fileList;
    private TextView textView;
    private int foregroundColor;
    private Toolbar toolbar;

    private SelectableFilterAdapter fileListAdapter;
    private static Menu menu;

    private DataViewModel dataViewModel;
    private FragmentListBinding binding;

    private boolean isSelectionModeActive;
    private boolean isSortedbyName = true;

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
        searchWebButton.setOnClickListener(view -> startWebView());

        textView = binding.listViewTextView;

        okButton = binding.listViewOkBtn;

        toolbar = requireActivity().findViewById(R.id.toolbar);

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

        restoreFilter();
    }

    @Override
    public void onPause() {
        super.onPause();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(filterEditText.getWindowToken(), 0);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (Objects.equals(dataViewModel.mode, MODE_SETLIST_SONG_SELECTION))
            dataViewModel.setSetListSongs(fileListAdapter.getSelectedFiles());
    }

    private void setUpMenu() {
        MenuHost menuHost = requireActivity();
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.list_view_menu, menu);

                ListFragment.menu = menu;

                if (!Objects.equals(dataViewModel.mode, MODE_SETLIST_SONG_SELECTION)) {
                    menu.findItem(R.id.menu_new_file).setVisible(true);
                    menu.findItem(R.id.menu_manage_files).setVisible(true);
                    menu.findItem(R.id.menu_sort_by).setVisible(true);
                }

                if (fileListAdapter == null || fileListAdapter.isEmpty()) {
                    menu.findItem(R.id.menu_manage_files).setVisible(false);
                }
                toggleSorting(false);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();

                if (itemId == R.id.menu_manage_files) {
                    startSelectionMode();
                    return true;
                } else if (itemId == R.id.menu_new_file) {
                    if (!(dataViewModel.mode.equals(MODE_SETLIST)))
                        startSongView(null);
                    else
                        newSetListDialog();
                    return true;
                } else if (itemId == R.id.menu_cancel_selection) {
                    cancelSelectionMode();
                    return true;
                } else if (itemId == R.id.menu_select_all) {
                    fileListAdapter.selectAll();
                    return true;
                } else if (itemId == R.id.menu_delete) {
                    verifyDelete();
                    return true;
                } else if (itemId == R.id.menu_share_files) {
                    shareSelectedFiles();
                    return true;
                } else if (itemId == R.id.menu_sort_by) {
                    toggleSorting(true);
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

    private void newSetListDialog() {

        if (!checkSdCard()) {
            return;
        }

        final EditText editText = new EditText(requireContext());
        editText.setSingleLine();
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            InputMethodManager imm = (InputMethodManager)
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (v.requestFocus())
                editText.post(() -> imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT));
            else
                imm.showSoftInput(v, InputMethodManager.HIDE_IMPLICIT_ONLY);
        });

        editText.setText(R.string.new_setlist);

        editText.setSelection(0, getString(R.string.new_setlist).length());

        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {

            if (SaveFileHelper.isInvalidFilename(editText.getText())) {
                Toast.makeText(getActivity(), getResources().getString(R.string.enter_good_filename), Toast.LENGTH_SHORT).show();
            } else {
                String setlistFileName = editText.getText().toString();
                if (!setlistFileName.endsWith(".pl"))
                    setlistFileName = setlistFileName.concat(".pl");

                editText.clearFocus();

                if (SaveFileHelper.fileExists(requireContext(), setlistFileName)) {

                    String finalSetlistFileName = setlistFileName;
                    new AlertDialog.Builder(requireContext())
                            .setCancelable(true)
                            .setTitle(R.string.overwrite_file_title)
                            .setMessage(R.string.overwrite_file)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                                SaveFileHelper.saveFile(requireContext(), "", finalSetlistFileName);
                                startSetListList(finalSetlistFileName);
                            })
                            .show();

                } else {
                    SaveFileHelper.saveFile(requireContext(), "", setlistFileName);
                    startSetListList(setlistFileName);
                }

            }

            dialog.dismiss();
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle(R.string.save_file)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> editText.clearFocus())
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
//            toggleSorting(false);
    }

    @Override
    public void afterTextChanged(Editable editable) {
        deleteFilterTextButton.setVisibility(TextUtils.isEmpty(editable) ? View.GONE : View.VISIBLE);
    }

    private void handleBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSelectionModeActive && !dataViewModel.mode.equals(MODE_SETLIST_SONG_SELECTION)) {
                    cancelSelectionMode();
                } else if (getParentFragment() != null) {
                    Navigation.findNavController(getParentFragment().requireView()).popBackStack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

    }

    @SuppressLint("ClickableViewAccessibility")
    protected void setUpInstance() {

        if (getArguments() != null) {
            dataViewModel.mode = ListFragmentArgs.fromBundle(getArguments()).getMode();
        }

        if (dataViewModel.mode.equals(MODE_SONGS))
            dataViewModel.resetData();

        if (!checkSdCard()) {
            return;
        }

        // Open files depending on listFragment mode

        List<String> filenames = null;

        switch (dataViewModel.mode) {
            case MODE_SONGS:
                setTitle("Songs");
                filenames = getFileNames(".txt");
                fileExtension = ".txt";
                textView.setText(R.string.no_local_songs);
                break;
            case MODE_SETLIST:
                setTitle("Setlists");
                filenames = getFileNames(".pl");
                fileExtension = ".pl";
                textView.setText(R.string.no_setlists);
                break;
            case MODE_SETLIST_SONG_SELECTION:
                setTitle(getString(R.string.file_selection_for_setlist));
                filenames = getFileNames(".txt");
                fileExtension = ".txt";

                okButton.setVisibility(View.VISIBLE);
                okButton.setOnClickListener(view -> {
                    if (getParentFragment() != null) {
                        Navigation.findNavController(getParentFragment().requireView()).popBackStack();
                    }
                });

                break;
        }

        if (filenames != null)
            Collections.sort(filenames, (Comparator<CharSequence>) (first, second) -> first.toString().toLowerCase().compareTo(second.toString().toLowerCase()));

        fileListAdapter = new SelectableFilterAdapter(requireContext(), filenames, fileExtension) {
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

                if (dataViewModel.mode.equals(MODE_SETLIST)) {
                    textView.setText(R.string.no_setlists);
                    textView.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);
                }

                if (!isSelectionModeActive)
                    menu.findItem(R.id.menu_manage_files).setVisible(fileListAdapter.getCount() != 0);
            }
        });

        fileList.setAdapter(fileListAdapter);

        //initial empty list
        if (dataViewModel.mode.equals(MODE_SONGS))
            searchWebButton.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);

        textView.setVisibility((fileListAdapter.getCount() == 0) ? View.VISIBLE : View.GONE);

        if (dataViewModel.mode.equals(MODE_SETLIST_SONG_SELECTION))
            setFileSelection();

        fileList.setOnItemClickListener((adapterView, view, i, l) -> {
            String filename = (String) adapterView.getAdapter().getItem(i);
            view.setBackgroundColor(Color.GRAY);

            if (isSelectionModeActive) {
                fileListAdapter.switchSelectionForIndex(i);
                if (Objects.equals(dataViewModel.mode, MODE_SETLIST_SONG_SELECTION))
                    dataViewModel.isSetListChanged = true;
            } else
                switch (dataViewModel.mode) {
                    case MODE_SONGS:
                        startSongView(filename);
                        break;
                    case MODE_SETLIST:
                        startSetListList(filename);
                        break;
                }
        });

        fileList.setOnItemLongClickListener((arg0, arg1, pos, arg3) -> {
            startSelectionMode();
            fileListAdapter.switchSelectionForIndex(pos);
            return true;
        });

        filterEditText.addTextChangedListener(this);

        deleteFilterTextButton.setOnTouchListener((view, motionEvent) -> {
            filterEditText.setText("");
            toggleSorting(false);
            return true;
        });
    }

    private void setTitle(String titleText) {

        // have to use original thread, else exception
        Looper looper = toolbar.getContext().getMainLooper();

        if (looper == null)
            return;

        Handler handlerInMainThread = new Handler(looper);

        Runnable yourRunnable = () -> toolbar.setTitle(titleText);

        handlerInMainThread.post(yourRunnable);
    }

    private void setFileSelection() {
        isSelectionModeActive = true;
        fileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        for (String filename : dataViewModel.setListSongs) {
            String s = filename.replace(".txt", "");
            int index = fileListAdapter.getIndexOfFile(s);

            if (index == -1)
                continue;

            fileListAdapter.switchSelectionForIndex(index);
        }
    }

    private void startSelectionMode() {
        isSelectionModeActive = true;
        fileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        menu.findItem(R.id.menu_manage_files).setVisible(false);
        menu.findItem(R.id.menu_new_file).setVisible(false);
        menu.findItem(R.id.menu_delete).setVisible(true);
        menu.findItem(R.id.menu_cancel_selection).setVisible(true);
        menu.findItem(R.id.menu_select_all).setVisible(true);
        menu.findItem(R.id.menu_sort_by).setVisible(false);

        if (Objects.equals(dataViewModel.mode, MODE_SONGS))
            menu.findItem(R.id.menu_share_files).setVisible(true);

        if (dataViewModel.mode.equals(MODE_SONGS))
            setTitle(getString(R.string.manage_saved_files));
        else if (dataViewModel.mode.equals(MODE_SETLIST))
            setTitle(getString(R.string.manage_saved_setlists));

    }


    private void cancelSelectionMode() {
        isSelectionModeActive = false;
        fileList.setChoiceMode(ListView.CHOICE_MODE_NONE);

        menu.findItem(R.id.menu_manage_files).setVisible(true);
        menu.findItem(R.id.menu_new_file).setVisible(true);
        menu.findItem(R.id.menu_delete).setVisible(false);
        menu.findItem(R.id.menu_cancel_selection).setVisible(false);
        menu.findItem(R.id.menu_select_all).setVisible(false);
        menu.findItem(R.id.menu_share_files).setVisible(false);
        menu.findItem(R.id.menu_sort_by).setVisible(true);

        fileListAdapter.unselectAll();

        if (dataViewModel.mode.equals(MODE_SONGS))
            setTitle("Songs");
        else if (dataViewModel.mode.equals(MODE_SETLIST))
            setTitle("Setlists");
    }

    private List<String> getFileNames(String fileExtension) {

        List<String> result = new ArrayList<>();

        // do in background to avoid jankiness
        final CountDownLatch latch = new CountDownLatch(1);

        HandlerThread handlerThread = new HandlerThread("GetFileListHandlerThread");
        handlerThread.start();

        Handler asyncHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String[] fileList = (String[]) msg.obj;

                result.addAll(Arrays.asList(fileList));

                latch.countDown();

                handlerThread.quit();
            }
        };

        Runnable runnable = () -> {
            Message message = new Message();
            message.obj = SaveFileHelper.getSavedFileNames(requireContext(), fileExtension);

            asyncHandler.sendMessage(message);
        };

        asyncHandler.post(runnable);

        // wait for async saving result
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (result.isEmpty())
            return new ArrayList<>();

        return result;
    }


    private boolean checkSdCard() {

        boolean result = SaveFileHelper.checkIfSdCardExists();

        if (!result) {
            Toast.makeText(getActivity(), getResources().getString(R.string.sd_card_not_found), Toast.LENGTH_SHORT).show();
        }
        return result;
    }


    protected void verifyDelete() {

        if (!checkSdCard()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        final ArrayList<String> filenameArray = fileListAdapter.getSelectedFiles();
        Log.d("ListFragment", filenameArray.toString());
        final int finalDeleteCount = filenameArray.size();

        if (finalDeleteCount > 0) {

            builder.setTitle(R.string.delete_saved_file)
                    .setCancelable(true)
                    .setMessage(String.format(getText(R.string.are_you_sure).toString(), finalDeleteCount))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // ok, delete

                        for (int i = 0; i < filenameArray.size(); i++) {
                            String s = filenameArray.get(i);

                            String fileName = "";
                            if (Objects.equals(dataViewModel.mode, MODE_SONGS))
                                fileName = s.concat(".txt");
                            else if (Objects.equals(dataViewModel.mode, MODE_SETLIST))
                                fileName = s.concat(".pl");
                            filenameArray.set(i, fileName);
                        }

                        boolean result = SaveFileHelper.deleteFile(requireContext(), filenameArray);

                        dataViewModel.getDeleteFileMLD().setValue(result);

                        dataViewModel.getDeleteFileMLD().observe(getViewLifecycleOwner(), deletionFinished -> {
                            if (deletionFinished) {
                                setUpInstance();

                                restoreFilter();

                                String toastText = String.format(getText(R.string.files_deleted).toString(), finalDeleteCount);
                                Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();

                                cancelSelectionMode();
                                dialog.dismiss();
                            }
                        });
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    private void startSetListList(String setlist) {

        if (!setlist.endsWith(".pl"))
            setlist = setlist.concat(".pl");

        ArrayList<String> filesList = (ArrayList<String>) SaveFileHelper.openSetlist(requireContext(), setlist);

        dataViewModel.setSetListSongs(filesList);
        dataViewModel.setSetListMLD(setlist);
        dataViewModel.isSetListChanged = false;

        if (getParentFragment() != null)
            Navigation.findNavController(getParentFragment().requireView()).navigate(R.id.nav_drag_list_view);
    }

    private void startSongView(String filename) {
        String songTitle = null;
        if (filename == null)
            songTitle = getString(R.string.new_file);
        ListFragmentDirections.ActionNavListFragmentToNavSongView action =
                ListFragmentDirections.actionNavListFragmentToNavSongView(songTitle, filename, null, null);

        if (getParentFragment() != null) {
            Navigation.findNavController(getParentFragment().requireView()).navigate(action);
        }
    }

    private void startWebView() {
        String searchText = filterEditText.getText().toString();
        ListFragmentDirections.ActionNavListFragmentToNavWebSearch action =
                ListFragmentDirections.actionNavListFragmentToNavWebSearch(searchText, null);

        if (getParentFragment() != null) {
            Navigation.findNavController(getParentFragment().requireView()).navigate(action);
        }


    }

    private void shareSelectedFiles() {
        ArrayList<String> fileNames = fileListAdapter.getSelectedFiles();

        for (int i = 0; i < fileNames.size(); i++) {
            String songName = fileNames.get(i);
            if (!songName.endsWith(".txt"))
                fileNames.set(i, songName + ".txt");
        }

        Intent intent = SaveFileHelper.shareFiles(requireContext(), fileNames.toArray(new String[0]));

        startActivity(intent);

        cancelSelectionMode();
    }

    private void toggleSorting(boolean isStateToChange) {
        Drawable drawable;

        //toggle state
        if (isStateToChange)
            isSortedbyName = !isSortedbyName;

        if (isSortedbyName) {
            drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_sort_by_size, null);
            if (menu != null) {
                menu.findItem(R.id.menu_sort_by).setIcon(drawable);
                menu.findItem(R.id.menu_sort_by).setTitle(getString(R.string.sort_by_date));
            }
            fileListAdapter.sortByName();
        } else {
            drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_sort_alphabetically, null);
            if (menu != null) {
                menu.findItem(R.id.menu_sort_by).setIcon(drawable);
                menu.findItem(R.id.menu_sort_by).setTitle(getString(R.string.sort_az));
            }
            fileListAdapter.sortByLastModified();
        }
    }

    private void restoreFilter() {
        String filterText = filterEditText.getText().toString();
        if (!(filterEditText.getText().toString().equals("")))
            this.fileListAdapter.getFilter().filter(filterText);
    }
}