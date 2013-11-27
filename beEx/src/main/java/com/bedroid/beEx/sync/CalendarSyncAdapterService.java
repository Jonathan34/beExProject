package com.bedroid.beEx.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.util.Log;

import com.bedroid.beEx.adapter.ExchangeAdapter;
import com.bedroid.beEx.adapter.IAdapter;
import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.helper.CalendarHelper;
import com.bedroid.beEx.helper.ExchangeHelper;
import com.bedroid.beEx.observer.CalendarObserver;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindItemsResults;

public class CalendarSyncAdapterService extends Service {
    private static final String TAG = "CalendarSyncAdapterService";
    private static SyncAdapterImpl sSyncAdapter = null;

    public CalendarSyncAdapterService() {
        super();
    }

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        private Context mContext;

        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
         }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            try {
                Calendar cal = Calendar.getInstance();
                Log.i(TAG, "performSync: " + account.toString() + " at " + cal.getTime().toString());

                IAdapter remoteAdapter = CalendarHelper.getCalendarAdapter(mContext, account);
                IAdapter localAdapter = CalendarHelper.getAndroidCalendarAdapter(mContext, account);

                // Retrieve local items
                HashMap<String, CalendarEntry> localItems = localAdapter.getAppointments();

                // Retrieve remote items
                HashMap<String, CalendarEntry> remoteItems = remoteAdapter.getAppointments();

                //CalendarHelper.dumpCalendarEntries(account, mContext.getContentResolver());

                /*StringBuilder sb = new StringBuilder("--CALENDAR LOCAL DUMP--");
                for (Map.Entry<String, CalendarEntry> s: localItems.entrySet()) {
                    sb.append("\n(" + s.getKey() + ")");
                }
                sb.append("----------");
                Log.i(TAG, sb.toString());

                sb = new StringBuilder("--CALENDAR REMOTE DUMP--");
                for (Map.Entry<String, CalendarEntry> s: remoteItems.entrySet()) {
                    sb.append("\n(" + s.getKey() + ")");
                }
                sb.append("----------");
                Log.i(TAG, sb.toString());*/

                //CalendarHelper.clearCalendarEntries(mContext, account);

                for (Map.Entry<String, CalendarEntry> s: localItems.entrySet()) {
                    String lkey = s.getKey();
                    CalendarEntry lval = s.getValue();

                    if(remoteItems.containsKey(lkey)) {
                        CalendarEntry rval = remoteItems.get(lkey);

                        if(rval != null) {
                            Log.i(TAG, "Checking if update is required with " + lkey);
                            if(!lval.equals(rval)) {
                                //check if remote or local changes? use last sync date and compare to remote last modification date?
                                long lmd = lval.getLastModificationTime().getTime();
                                long rmd = rval.getLastModificationTime().getTime();
                                if(lmd < rmd) {
                                    Log.i(TAG, "\t Entry is NOT identical... updating LOCAL");
                                    localAdapter.updateEntry(lval, rval);
                                }
                                else {
                                    Log.i(TAG, "\t Entry is NOT identical... updating REMOTE");
                                    remoteAdapter.updateEntry(lval, rval);
                                    //TODO also update local last modification date to avoid downloading again the update on next sync
                                }
                            }
                            else {
                                Log.i(TAG, "\t Entry is identical... nothing to do");
                            }
                        }
                        remoteItems.remove(lkey);// mark remote item as processed
                    }
                    else {
                        //TODO either it does not exist, either it has to be created on server...
                        //UID is not set if it has to be created on server...?
                        if(lval.getUid() == null) {
                            Log.i(TAG, "\t Entry has been created... creating on remote database");
                            //TODO
                        }
                        else {
                            // Entry doesn't exist. Remove it from the database.
                            int deleted = CalendarHelper.deleteCalendarEntry(mContext.getContentResolver(), Long.parseLong(lval.getEventId()));
                            Log.i(TAG, "\t Entry does not exist... removing from local database: " + deleted);
                        }
                    }
                }

                // Add new items
                for (Map.Entry<String, CalendarEntry> s: remoteItems.entrySet()) {
                    CalendarEntry rval = s.getValue();
                    if(rval != null) {
                        String id = CalendarHelper.addCalendarEntry(mContext, account, rval);
                        Log.i(TAG, "Added entry " + id);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    private SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null)
            sSyncAdapter = new SyncAdapterImpl(this);
        return sSyncAdapter;
    }
}