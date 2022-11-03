package org.hollowbamboo.chordreader2.adapter;

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

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.data.ColorScheme;
import org.hollowbamboo.chordreader2.helper.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;

public class SelectableFilterAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private final List<String> originalData;
    private List<String> filteredData;
    private final LayoutInflater mInflater;
    private final ItemFilter mFilter = new ItemFilter();
    private final ArrayList<String> selectedItems = new ArrayList<>();
    private final ColorScheme colorScheme;

    public SelectableFilterAdapter(Context context, List<String> data) {
        this.filteredData = data;
        this.originalData = data;
        this.context = context;
        mInflater = LayoutInflater.from(context);
        colorScheme = PreferenceHelper.getColorScheme(context);
    }

    public int getIndexOfFile(String filename) {
        int index = 0;
        for(String s : filteredData) {
            if(s.equals(filename))
                return  index;
            index++;
        }
        return -1;
    }

    public void switchSelectionForIndex(int index) {

        String item = filteredData.get(index);
        if(selectedItems.contains(item))
            selectedItems.remove(item);
        else
            selectedItems.add(item);

        notifyDataSetChanged();
    }

    public void selectAll() {
        for (String item : filteredData) {
            if(!(selectedItems.contains(item)))
                selectedItems.add(item);
        }
        notifyDataSetChanged();
    }

    public void unselectAll() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedFiles() {
        return selectedItems;
    }

    public int getCount() {
        return filteredData == null ? 0 : filteredData.size();
    }

    public Object getItem(int position) {
        return filteredData.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_simple, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to setOnItemClickListener data to.
            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(android.R.id.text1);


            // Bind the data efficiently with the holder.
            convertView.setTag(holder);

        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }


//        if(!selectedIndexes.isEmpty() && selectedIndexes.contains(position)) {
        if(!selectedItems.isEmpty() && selectedItems.contains(filteredData.get(position))) {
            holder.textView.setBackgroundColor(colorScheme.getLinkColor(context));
        } else {
            holder.textView.setBackgroundColor(Color.TRANSPARENT);
        }

        // If weren't re-ordering this you could rely on what you set last time
        holder.textView.setText(filteredData.get(position));

        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
    }

    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final List<String> originalDataList = originalData;

            int count = originalDataList.size();
            final ArrayList<String> filteredDataList = new ArrayList<>(count);

            //Filter original list
            String filterableString;

            if(filterString.isEmpty()) {
                results.values = originalDataList;
                results.count = originalDataList.size();
                return results;
            }

            for (int i = 0; i < count; i++) {
                filterableString = originalDataList.get(i);
                if(filterableString.toLowerCase().contains(filterString)) {
                    filteredDataList.add(filterableString);
                }
            }

            results.values = filteredDataList;
            results.count = filteredDataList.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<String>) results.values;
            notifyDataSetChanged();
        }

    }
}
