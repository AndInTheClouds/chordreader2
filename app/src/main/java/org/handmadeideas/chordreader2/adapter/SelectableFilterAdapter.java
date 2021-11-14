package org.handmadeideas.chordreader2.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.handmadeideas.chordreader2.R;
import org.handmadeideas.chordreader2.data.ColorScheme;
import org.handmadeideas.chordreader2.helper.PreferenceHelper;
import org.handmadeideas.chordreader2.helper.SaveFileHelper;

import java.util.ArrayList;
import java.util.List;

public class SelectableFilterAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private final List<String> originalData;
    private List<String> filteredData;
    private final LayoutInflater mInflater;
    private final ItemFilter mFilter = new ItemFilter();
    private ArrayList<Integer> selectedIndexes = new ArrayList<>();
    ColorScheme colorScheme = PreferenceHelper.getColorScheme(context);

    public SelectableFilterAdapter(Context context, List<String> data) {
        this.filteredData = data;
        this.originalData = data;
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }

    public void switchSelectionForIndex(int index) {
        if (selectedIndexes.contains(index))
            selectedIndexes.remove((Integer) index);
        else
            selectedIndexes.add(index);

        notifyDataSetChanged();
    }

    public void selectAll() {
        for (int i = 0; i < filteredData.size(); i++) {
            if (!(selectedIndexes.contains(i)))
                selectedIndexes.add(i);
        }
        notifyDataSetChanged();
    }

    public void unselectAll() {
        selectedIndexes.clear();
        notifyDataSetChanged();
    }

    public CharSequence[] getSelectedFiles() {
        ArrayList<String> selectedFiles = new ArrayList<>();
        for (int index : selectedIndexes) {
            String filename = filteredData.get(index);
            selectedFiles.add(SaveFileHelper.rectifyFilename(filename));
        }

        return selectedFiles.toArray(new String[0]);
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
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.simple_list_item, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(android.R.id.text1);


            // Bind the data efficiently with the holder.
            convertView.setTag(holder);

        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }


        if (!selectedIndexes.isEmpty() && selectedIndexes.contains(position)) {
            holder.textView.setBackgroundColor(colorScheme.getLinkColor(context));
        } else {
            holder.textView.setBackgroundColor(Color.TRANSPARENT);
        }

        // If weren't re-ordering this you could rely on what you set last time
        holder.textView.setText(filteredData.get(position));
        //TODO: holder.textView.setText("" + (position + 1) + " " + testList.get(position).getTestText());


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
            final ArrayList<String> filteredDataList = new ArrayList<String>(count);

            //Filter original list
            String filterableString;

            if (filterString.isEmpty()) {
                results.values = originalDataList;
                results.count = originalDataList.size();
                return results;
            }

            for (int i = 0; i < count; i++) {
                filterableString = originalDataList.get(i);
                if (filterableString.toLowerCase().contains(filterString)) {
                    filteredDataList.add(filterableString);
                }
            }

            results.values = filteredDataList;
            results.count = filteredDataList.size();

            // Update selectedIndexes according to filtered list
            ArrayList<Integer> filteredSelectedIndexes = new ArrayList<>();
            for (int i = 0; i < filteredDataList.size(); i++) {
                String filename = filteredDataList.get(i);
                int ind = filteredData.indexOf(filename);

                if (selectedIndexes.contains(ind))
                    filteredSelectedIndexes.add(i);
            }
            selectedIndexes = filteredSelectedIndexes;

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
