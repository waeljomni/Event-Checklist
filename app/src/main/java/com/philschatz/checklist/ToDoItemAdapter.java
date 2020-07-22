package com.philschatz.checklist;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/**
 * This is a single ToDoItem in a list (FirebaseRecyclerAdapter)
 */
class ToDoItemAdapter extends FirebaseRecyclerAdapter<ToDoItem, ToDoItemViewHolder> implements ItemTouchHelperClass.ItemTouchHelperAdapter {

    private final String TAG = ToDoItemAdapter.class.getSimpleName();

    private ToDoListActivity mContext;
    private ToDoList mList;
    private ToDoItem mJustCompletedToDoItem;
    private String mListKey;
    private DatabaseReference mJustCompletedToDoItemRef;


    public ToDoItemAdapter(ToDoListActivity context, ToDoList list, String listKey, Query items) {
        super(ToDoItem.class, R.layout.list_circle_try, ToDoItemViewHolder.class, items);
        mContext = context;
        mList = list;
        mListKey = listKey;
    }

    @Override
    public void populateViewHolder(ToDoItemViewHolder holder, ToDoItem item, int position) {

        holder.mContext = mContext;
        holder.mItem = item;
        holder.mItemKey = getRef(position).getKey();
        holder.mListKey = mListKey; // TODO: Look this up in a less-hacky way
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(MainActivity.THEME_PREFERENCES, Context.MODE_PRIVATE);
        //Background color for each to-do item. Necessary for night/day mode
        int bgColor;
        //color of title text in our to-do item. White for night mode, dark gray for day mode
        int todoTextColor;
        if (sharedPreferences.getString(MainActivity.THEME_SAVED, MainActivity.LIGHTTHEME).equals(MainActivity.LIGHTTHEME)) {
            bgColor = Color.WHITE;
            todoTextColor = mContext.getResources().getColor(R.color.secondary_text);
        } else {
            bgColor = Color.DKGRAY;
            todoTextColor = Color.WHITE;
        }
        holder.linearLayout.setBackgroundColor(bgColor);

        if (item.hasReminder() || item.isComplete()) {
            holder.mToDoTextview.setMaxLines(1);
            holder.mTimeTextView.setVisibility(View.VISIBLE);
        } else {
            holder.mTimeTextView.setVisibility(View.GONE);
            holder.mToDoTextview.setMaxLines(2);
        }
        holder.mToDoTextview.setText(item.getTitle());
        holder.mToDoTextview.setTextColor(todoTextColor);
        if (item.isComplete()) {
            holder.mToDoTextview.setTextColor(Color.LTGRAY);
            holder.mTimeTextView.setTextColor(Color.LTGRAY);
        }
        if (item.isComplete()) {
            holder.mToDoTextview.setPaintFlags(holder.mToDoTextview.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.mToDoTextview.setPaintFlags(0);
        }

        //            holder.mColorTextView.setBackgroundColor(Color.parseColor(item.getTodoColor()));

        //            TextDrawable myDrawable = TextDrawable.builder().buildRoundRect(item.getTitle().substring(0,1),Color.RED, 10);
        //We check if holder.color is set or not
        //            if(item.getTodoColor() == null){
        //                ColorGenerator generator = ColorGenerator.MATERIAL;
        //                int color = generator.getRandomColor();
        //                item.setTodoColor(color+"");
        //            }
        //            Log.d("OskarSchindler", "Color: "+item.getTodoColor());


        String firstLetter = item.getTitle().substring(0, 1);
//        // Use the first letter as the hash for the color
//        int color = ColorGenerator.MATERIAL.getColor(firstLetter);
        int color = mList.getColor();
        TextDrawable myDrawable = TextDrawable.builder().beginConfig()
                .textColor(Color.WHITE)
                .useFont(Typeface.DEFAULT)
                .toUpperCase()
                .endConfig()
                .buildRound(firstLetter, color);

        holder.mColorImageView.setImageDrawable(myDrawable);
        if (item.isComplete()) {
            long time = item.completedAt();
            holder.mTimeTextView.setReferenceTime(time);
        } else if (item.hasReminder()) {
            long time = item.remindAt();
            holder.mTimeTextView.setReferenceTime(time);
        }

    }

    // ItemTouchHelperAdapter methods
    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        Log.d(TAG, "TODO: Item was moved but we do not implement that yet");
    }

    @Override
    public void onItemRemoved(final int position) {
        //Remove this line if not using Google Analytics
//        mContext.app.send(this, "Action", "Swiped Todo Away");

        mJustCompletedToDoItem = getItem(position);
        mJustCompletedToDoItemRef = getRef(position);

        // Toggle the "completedAt" field
        mJustCompletedToDoItem.toggleCompletedAt();

        // Save
        mJustCompletedToDoItemRef.setValue(mJustCompletedToDoItem);

//        Intent i = new Intent(mContext, TodoNotificationService.class);
//        mContext.deleteAlarm(i, mJustCompletedToDoItem.getIdentifier().hashCode());

        String toShow = mJustCompletedToDoItem.getTitle();
        toShow = (toShow.length() > 20) ? toShow.substring(0, 20) + "..." : toShow;

        Snackbar.make(mContext.mCoordLayout, "Completed " + toShow, Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //Comment the line below if not using Google Analytics
//                        mainActivity.app.send(this, "Action", "UNDO Pressed");
                        // TODO: PHIL Insertion order should be a float so we can always insert between 2 items

                        // Toggle completedAt
                        mJustCompletedToDoItem.toggleCompletedAt();

                        // Save changes
                        mJustCompletedToDoItemRef.setValue(mJustCompletedToDoItem);
                    }
                }).show();
    }


}
