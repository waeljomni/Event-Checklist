package com.philschatz.checklist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;


/*
Notes for what needs to be worked on:

- [ ] support multiple lists
- [ ] support multiple item types (for counting calories)
- [ ] store lastUpdated times
- [ ] create a log of changes (for export later)
- [ ] allow sharing lists
- [ ] reorder items
- [ ] add snooze button for homescreen reminder
 */

public class ToDoListActivity extends AppCompatActivity {
    public static final String SHARED_PREF_DATA_SET_CHANGED = "com.philschatz.checklist.datasetchanged";
    public static final String CHANGE_OCCURED = "com.philschatz.checklist.changeoccured";
    public static final String THEME_PREFERENCES = "com.philschatz.checklist.themepref";
    public static final String THEME_SAVED = "com.philschatz.checklist.savedtheme";
    public static final String DARKTHEME = "com.philschatz.checklist.darktheme";
    public static final String LIGHTTHEME = "com.philschatz.checklist.lighttheme";
    public static final int REQUEST_ID_TODO_ITEM = 100;
    private static final String TAG = ToDoListActivity.class.getSimpleName();
    private static final int REQUEST_ID_EDIT_LIST = 101;

    private Toolbar mToolbar;
    private String mListKey;
    private ToDoList mList;
    public ItemTouchHelper itemTouchHelper;
    private RecyclerViewEmptySupport mRecyclerView;
    private FloatingActionButton mAddToDoItemFAB;
    CoordinatorLayout mCoordLayout;
    private CustomRecyclerScrollViewListener customRecyclerScrollViewListener;
    private int mTheme = -1;
    private String theme = "name_of_the_theme";
    AnalyticsApplication app;
    private String[] testStrings = {"Clean my room",
            "Water the plants",
            "Get car washed",
            "Get my dry cleaning"
    };
    private DatabaseReference databaseReference;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    protected void onCreate(Bundle savedInstanceState) {

        //We recover the theme we've set and setTheme accordingly
        theme = getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE).getString(THEME_SAVED, LIGHTTHEME);

        if (theme.equals(LIGHTTHEME)) {
            mTheme = R.style.CustomStyle_LightTheme;
        } else {
            mTheme = R.style.CustomStyle_DarkTheme;
        }
        this.setTheme(mTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_DATA_SET_CHANGED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(CHANGE_OCCURED, false);
        editor.apply();

        Intent i = getIntent();
        mListKey = i.getStringExtra(Const.TODOLISTKEY);
        mList = (ToDoList) i.getSerializableExtra(Const.TODOLISTSNAPSHOT);

        databaseReference = MainActivity.getListItemsReference(mListKey);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mToolbar.setTitle(mList.getTitle());
        mToolbar.setBackgroundColor(mList.getColor());
        setSupportActionBar(mToolbar);


        mCoordLayout = (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout);
        mAddToDoItemFAB = (FloatingActionButton) findViewById(R.id.addToDoItemFAB);

        mAddToDoItemFAB.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View v) {
//                app.send(this, "Action", "FAB pressed");
                Intent newTodo = new Intent(ToDoListActivity.this, AddToDoItemActivity.class);
                ToDoItem item = new ToDoItem();
                item.setTitle(""); // This way the editor will start up blank
                newTodo.putExtra(Const.TODOITEMSNAPSHOT, item);
                // new items do not have a Firebase id yet  TODO PHIL Maybe this should be the point when they get an id
                newTodo.putExtra(Const.TODOITEMKEY, databaseReference.push().getKey());
                newTodo.putExtra(Const.TODOLISTKEY, mListKey);

                startActivityForResult(newTodo, REQUEST_ID_TODO_ITEM);
            }
        });


        mRecyclerView = (RecyclerViewEmptySupport) findViewById(R.id.toDoRecyclerView);
        if (theme.equals(LIGHTTHEME)) {
            mRecyclerView.setBackgroundColor(getResources().getColor(R.color.primary_lightest));
        }
        mRecyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Note: Set this to an instance variable so it can be destroyed later
        customRecyclerScrollViewListener = new FABRecyclerScrollViewListener(mAddToDoItemFAB);
        mRecyclerView.addOnScrollListener(customRecyclerScrollViewListener);


        // TODO: Checkout android.R.layout.two_line_list_item instead
        // TODO: Try to sort & filter the list : https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview#30429439
        Query sortedItems = databaseReference.orderByChild("completedAt");
        ToDoItemAdapter mAdapter = new ToDoItemAdapter(this, mList, mListKey, sortedItems);

        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelperClass(mAdapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

//    public void addThemeToSharedPreferences(String theme) {
//        SharedPreferences sharedPreferences = getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString(THEME_SAVED, theme);
//        editor.apply();
//    }
//
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aboutMeMenuItem:
                Intent i = new Intent(this, AboutActivity.class);
                startActivity(i);
                return true;
//            case R.id.switch_themes:
//                if(mTheme == R.style.CustomStyle_DarkTheme){
//                    addThemeToSharedPreferences(LIGHTTHEME);
//                }
//                else{
//                    addThemeToSharedPreferences(DARKTHEME);
//                }
//
////                if(mTheme == R.style.CustomStyle_DarkTheme){
////                    mTheme = R.style.CustomStyle_LightTheme;
////                }
////                else{
////                    mTheme = R.style.CustomStyle_DarkTheme;
////                }
//                this.recreate();
//                return true;
            case R.id.preferences:
                Intent intent = new Intent(this, AddToDoListActivity.class);
                intent.putExtra(Const.TODOLISTKEY, mListKey);
                intent.putExtra(Const.TODOLISTSNAPSHOT, mList);
                startActivityForResult(intent, REQUEST_ID_EDIT_LIST);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode != RESULT_CANCELED && requestCode == REQUEST_ID_TODO_ITEM) {
//            ToDoItem item = (ToDoItem) data.getSerializableExtra(Const.TODOITEMSNAPSHOT);
//            String itemKey = data.getStringExtra(Const.TODOITEMKEY);
//            // TODO: Use the list key here
//            MainActivity.getListItemReference(mListKey, itemKey).setValue(item);
//        }
        if (resultCode != RESULT_CANCELED && requestCode == REQUEST_ID_EDIT_LIST) {
            mList = (ToDoList) data.getSerializableExtra(Const.TODOLISTSNAPSHOT);
            // update the toolbar
            mToolbar.setTitle(mList.getTitle());
            mToolbar.setBackgroundColor(mList.getColor());

        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://philschatz.com"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        mRecyclerView.removeOnScrollListener(customRecyclerScrollViewListener);
    }


}
