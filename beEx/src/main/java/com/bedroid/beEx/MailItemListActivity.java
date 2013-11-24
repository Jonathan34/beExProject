package com.bedroid.beEx;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.bedroid.beEx.helper.ExchangeHelper;
import com.bedroid.beEx.observer.CalendarObserver;
import com.bedroid.beEx.sync.CalendarSyncAdapterService;

import java.util.TimeZone;

import microsoft.exchange.webservices.data.ExchangeService;

/**
 * An activity representing a list of MailItems. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link MailItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link MailItemListFragment} and the item details
 * (if present) is a {@link MailItemDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link MailItemListFragment.Callbacks} interface
 * to listen for item selections.
 */

class TabListener implements ActionBar.TabListener {

    private ActionBar.Tab tab;
    public TabListener(ActionBar.Tab tabb){
        this.tab = tabb;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // This is called when a tab is selected.
        /*Intent detailIntent = new Intent(this, MailItemDetailActivity.class);
        detailIntent.putExtra(MailItemDetailFragment.ARG_ITEM_ID, tab.getText());
        startActivity(detailIntent);*/
       //fragmentTransaction.add(R.id.mailitem_detail, tab, null);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        //fragmentTransaction.add(tab);
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }
}


public class MailItemListActivity extends ActionBarActivity implements MailItemListFragment.Callbacks {
    private static final String TAG = "MailItemListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    //private CalendarObserver mCalObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mailitem_list);

        // Set the Action Bar to use tabs for navigation
        ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Add three tabs to the Action Bar for display
        ActionBar.Tab a1 = ab.newTab();
        ActionBar.Tab a2 = ab.newTab();
        ab.addTab(a1.setText("Tab 1").setTabListener(new TabListener(a1)));
        ab.addTab(a2.setText("Tab 2").setTabListener(new TabListener(a2)));

        // TODO: If exposing deep links into your app, handle intents here.
        //calendar content provider events
        //mCalObserver = new CalendarObserver();
        //getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI, true, mCalObserver);

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    }

    /*@Override
    protected void onStop() {
        getContentResolver().unregisterContentObserver(mCalObserver);
    }*/

    /**
     * Use this method to instantiate your menu, and add your items to it. You
     * should return true if you have added items to it and want the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate our menu from the resources by using the menu inflater.
        getMenuInflater().inflate(R.menu.main, menu);

        /*
        DYNAMICALLY ADD AN ITEM

        // It is also possible add items here. Use a generated id from
        // resources (ids.xml) to ensure that all menu ids are distinct.
        //MenuItem locationItem = menu.add(0, R.id.menu_location, 0, R.string.menu_location);
        //locationItem.setIcon(R.drawable.ic_action_location);

        // Need to use MenuItemCompat methods to call any action item related methods
        //MenuItemCompat.setShowAsAction(locationItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);
        */

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                //TODO refresh selected account
                AccountManager am = AccountManager.get(getBaseContext());
                Account[] accounts = am.getAccountsByType("com.bedroid.beEx.account");
                if(accounts != null && accounts.length > 0) {
                    Bundle b = new Bundle();
                    b.putBoolean(getContentResolver().SYNC_EXTRAS_MANUAL, true);
                    b.putBoolean(getContentResolver().SYNC_EXTRAS_EXPEDITED, true);
                    getContentResolver().requestSync(accounts[0], "com.android.calendar", b);
                }

                return false;

            case R.id.menu_add:
                // Here we might call LocationManager.requestLocationUpdates()
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                return true;

            case R.id.menu_settings:
                // Here we would open up our settings activity
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsIntent);
                return true;
        }
        return true;

    }

    @Override
    public void onItemSelected(String id) {
        // This is called when a tab is selected.
        Intent detailIntent = new Intent(this, MailItemDetailActivity.class);
        detailIntent.putExtra(MailItemDetailFragment.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }
}
