package org.hollowbamboo.chordreader2.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.hollowbamboo.chordreader2.R;
import org.hollowbamboo.chordreader2.interfaces.OnItemClickListener;
import org.hollowbamboo.chordreader2.interfaces.StartDragListener;
import org.hollowbamboo.chordreader2.util.ItemMoveCallback;

import java.util.ArrayList;
import java.util.Collections;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract{

    private final OnItemClickListener listener;
    private final StartDragListener startDragListener;
    private final ArrayList<String> data;

    public RecyclerViewAdapter(ArrayList<String> data, OnItemClickListener onItemClickListener, StartDragListener startDragListener) {
        this.data = data;
        this.listener = onItemClickListener;
        this.startDragListener = startDragListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item_setlist_songs, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getListItemCountTextView().setText(String.valueOf((position + 1)));
        holder.getSongFileNameTextView().setText(data.get(position));
        holder.getListItemDragHandleImageView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    startDragListener.requestDrag(holder);
                }
                return true;
            }
        });
        holder.getListItemDeleteImageView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    int position = holder.getBindingAdapterPosition();
                    data.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, data.size());
                    //notifyItemRangeRemoved(position, data.size());
                }
                return true;
            }
        });
        holder.setOnItemClickListener(data.get(position), listener);
    }

    @Override
    public int getItemCount() { return data.size(); }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        if(fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(data, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(data, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);

    }

    @Override
    public void onRowSelected(ViewHolder myViewHolder) {
        //myViewHolder.rowView.setBackgroundColor(Color.GRAY);
    }

    @Override
    public void onRowClear(ViewHolder myViewHolder) {
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView listItemCountTextView, songFileNameTextView;
        private final ImageView listItemDeleteImageView,listItemDragHandleImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            listItemDeleteImageView = itemView.findViewById(R.id.setlist_item_delete);
            listItemCountTextView = itemView.findViewById(R.id.setlist_song_count_text_view);
            songFileNameTextView = itemView.findViewById(R.id.setlist_song_filename_text_view);
            listItemDragHandleImageView = itemView.findViewById(R.id.setlist_item_drag_handle);
        }

        public TextView getListItemCountTextView() {
            return listItemCountTextView;
        }
        public TextView getSongFileNameTextView() {return songFileNameTextView;}
        public ImageView getListItemDragHandleImageView() {
            return listItemDragHandleImageView;
        }
        public ImageView getListItemDeleteImageView() {
            return listItemDeleteImageView;
        }

        public void setOnItemClickListener(final String item, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}

