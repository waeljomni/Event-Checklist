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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.jakewharton.threetenabp.AndroidThreeTen;

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

public class MainActivity extends AppCompatActivity {
    public static final String FILENAME = "todoitems.json";
    public static final String SHARED_PREF_DATA_SET_CHANGED = "com.philschatz.checklist.datasetchanged";
    public static final String CHANGE_OCCURED = "com.philschatz.checklist.changeoccured";
    public static final String THEME_PREFERENCES = "com.philschatz.checklist.themepref";
    public static final String RECREATE_ACTIVITY = "com.philschatz.checklist.recreateactivity";
    public static final String THEME_SAVED = "com.philschatz.checklist.savedtheme";
    public static final String DARKTHEME = "com.philschatz.checklist.darktheme";
    public static final String LIGHTTHEME = "com.philschatz.checklist.lighttheme";
    public static final int REQUEST_ID_TODO_ITEM = 100;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ID_TODO_LIST = 200;

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


    private static FirebaseDatabase __db;

    public static FirebaseDatabase getFirebaseDatabase() {
        // ensure setPersistenceEnabled is called only once in the app
        if (__db == null) {
            __db = FirebaseDatabase.getInstance();
            __db.setPersistenceEnabled(true); // Support offline storage
        }
        return __db;
    }
    public static DatabaseReference getReference(String path) {
        return getFirebaseDatabase().getReference(path);
    }
    public static DatabaseReference getListsReference() {
        return getFirebaseDatabase().getReference().child("lists");
    }
    public static DatabaseReference getListReference(String listKey) {
        return getFirebaseDatabase().getReference().child("lists").child(listKey);
    }
    public static DatabaseReference getListItemsReference() {
        return getFirebaseDatabase().getReference().child("list-items1");
    }
    public static DatabaseReference getListItemsReference(String listKey) {
        return getFirebaseDatabase().getReference().child("list-items1").child(listKey);
    }
    public static DatabaseReference getListItemReference(String listKey, String itemKey) {
         return getFirebaseDatabase().getReference().child("list-items1").child(listKey).child(itemKey);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.send(this);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_DATA_SET_CHANGED, MODE_PRIVATE);
        if (sharedPreferences.getBoolean(ReminderActivity.EXIT, true)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(ReminderActivity.EXIT, false);
            editor.apply();
            finish();
        }
        /*
        We need to do this, as this activity's onCreate won't be called when coming back from SettingsActivity,
        thus our changes to dark/light mode won't take place, as the setContentView() is not called again.
        So, inside our SettingsFragment, whenever the checkbox's value is changed, in our shared preferences,
        we mark our recreate_activity key as true.

        Note: the recreate_key's value is changed to false before calling recreate(), or we woudl have ended up in an infinite loop,
        as onResume() will be called on recreation, which will again call recreate() and so on....
        and get an ANR

         */
        if (getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE).getBoolean(RECREATE_ACTIVITY, false)) {
            SharedPreferences.Editor editor = getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE).edit();
            editor.putBoolean(RECREATE_ACTIVITY, false);
            editor.apply();
            recreate();
        }


    }

    @Override
    protected void onStart() {
        app = (AnalyticsApplication) getApplication();
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    private void setAlarms(Query databaseRef) {
        databaseRef.addChildEventListener(new ChildEventListener() {

//            HashMap<String, ToDoItemAlarmListener> listeners = new HashMap<>();

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ToDoList list = dataSnapshot.getValue(ToDoList.class);
                String listKey = dataSnapshot.getKey();
                ToDoItemAlarmListener l = new ToDoItemAlarmListener(MainActivity.this, list, listKey);
                getListItemsReference(listKey).addChildEventListener(l);
//                listeners.put(listId, l);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // Nothing to do here
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
//                String listId = dataSnapshot.getKey();
//                listeners.remove(listId);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    protected void onCreate(Bundle savedInstanceState) {
        app = (AnalyticsApplication) getApplication();

        // Init timezones
        AndroidThreeTen.init(this);

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

        databaseReference = getListsReference();


        setAlarms(getListsReference());

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("My Lists");
        setSupportActionBar(toolbar);


        mCoordLayout = (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout);
        mAddToDoItemFAB = (FloatingActionButton) findViewById(R.id.addToDoItemFAB);

        mAddToDoItemFAB.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View v) {
                app.send(this, "Action", "FAB pressed");
                Intent newTodo = new Intent(MainActivity.this, AddToDoListActivity.class);

                ToDoList list = new ToDoList();
                list.setTitle(""); // This way the editor will start up blank
                newTodo.putExtra(Const.TODOLISTSNAPSHOT, list);
                // new items do not have a Firebase id yet  TODO PHIL Maybe this should be the point when they get a key
                newTodo.putExtra(Const.TODOLISTKEY, getListsReference().push().getKey());

                startActivity(newTodo);
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
        Query sortedItems = databaseReference.orderByChild("title");
//        ToDoItemAdapter mAdapter = new ToDoItemAdapter(this, this, sortedItems);
        ToDoListAdapter mAdapter = new ToDoListAdapter(this, this, sortedItems);

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
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED && requestCode == REQUEST_ID_TODO_ITEM) {
            ToDoItem item = (ToDoItem) data.getSerializableExtra(Const.TODOITEMSNAPSHOT);
            String itemId = data.getStringExtra(Const.TODOITEMKEY);

            if (item.getTitle().length() <= 0) {
                return;
            }
            boolean existed = false;

//            if (item.legacyGetRemindAt() != null) {
//                Intent i = new Intent(this, TodoNotificationService.class);
//                i.putExtra(TodoNotificationService.TODOTEXT, item.getTitle());
//                i.putExtra(TodoNotificationService.TODOUUID, item.getIdentifier());
//                createAlarm(i, item.getIdentifier().hashCode(), item.legacyGetRemindAt().getTime());
//                Log.d(TAG, "Alarm Created: "+item.getTitle()+" at "+item.legacyGetRemindAt());
//            }

            // append a new item or edit an existing item
            // TODO: Update the item directly without using databaseReference here
            if (itemId != null) {
                databaseReference.child(itemId).setValue(item);
            } else {
                databaseReference.push().setValue(item);
            }

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
