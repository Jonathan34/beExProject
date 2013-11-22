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
import android.util.Log;

import com.bedroid.beEx.adapter.ExchangeAdapter;
import com.bedroid.beEx.adapter.IAdapter;
import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.helper.CalendarHelper;
import com.bedroid.beEx.helper.ExchangeHelper;

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
                ///*new */CalendarSyncAdapterService/*()*/.performSync(mContext, account, extras, authority, provider, syncResult);

                Calendar cal = Calendar.getInstance();
                Log.i(TAG, "performSync: " + account.toString() + " at " + cal.getTime().toString());

                IAdapter remoteAdapter = CalendarHelper.getCalendarAdapter(mContext, account);
                IAdapter localAdapter = CalendarHelper.getAndroidCalendarAdapter(mContext, account);

                // 1.a Retrieve local items
                //HashMap<String, CalendarEntry> localItems = CalendarHelper.loadFromLocalCalendar(mContext, account);
                HashMap<String, CalendarEntry> localItems = localAdapter.getAppointments();

                // 1.b Retrieve remote items
                //HashMap<String, CalendarEntry> remoteItems = CalendarHelper.loadFromRemoteCalendar(mContext, service);
                HashMap<String, CalendarEntry> remoteItems = remoteAdapter.getAppointments();

                //1.c debug
                StringBuilder sb = new StringBuilder("--CALENDAR LOCAL DUMP--");
                for (Map.Entry<String, CalendarEntry> s: localItems.entrySet()) {
                    sb.append("("+s.getKey()+"|"+s.getValue()+")");
                }
                sb.append("----------");
                Log.i(TAG, sb.toString());

                sb = new StringBuilder("--CALENDAR REMOTE DUMP--");
                for (Map.Entry<String, CalendarEntry> s: remoteItems.entrySet()) {
                    sb.append("("+s.getKey()+"|"+s.getValue()+")");
                }
                sb.append("----------");
                Log.i(TAG, sb.toString());

                Set<String> processedEntries = new HashSet<String>();

                //TODO Remove -- clears up everything we have
                CalendarHelper.clearCalendarEntries(mContext, account);

                for (Map.Entry<String, CalendarEntry> s: remoteItems.entrySet()) {
                    String rkey = s.getKey();
                    CalendarEntry rval = s.getValue();

                    if(rkey == null || rkey.isEmpty()) {
                        Log.e(TAG, "Incorrect entry found");
                        continue;
                    }

                    //have we already see this entry?
                    if(processedEntries.contains(rkey)) {
                        Log.w(TAG, "Already processed from server: skipping " + rkey);
                        continue;
                    }

                    //check if we have this entry locally and update it
                    if(localItems.containsKey(rkey)) {
                        //TODO update
                        Log.i(TAG, "Updating entry " + rkey);
                        //TODO check for local changes
                        CalendarEntry lval = localItems.get(rkey);
                        if(lval.equals(rval)) {
                            Log.i(TAG, "\t Entry is identical... nothing to do" + rkey);
                        }
                        else {
                            //TODO changes found, upload
                            Log.i(TAG, "\t Entry is NOT identical... updating" + rkey);
                            Calendar ldate = Calendar.getInstance();
                            ldate.setTime(lval.getLastModificationTime());

                            Calendar rdate = Calendar.getInstance();
                            rdate.setTime(rval.getLastModificationTime());

                            if(ldate.before(rdate)) {
                                Log.i(TAG, "\t updating local " + rkey);
                            }
                            else {
                                Log.i(TAG, "\t updating remote " + rkey);
                            }

                        }

                        //TODO no change found, nothing to do
                        //TODO check for remote changes
                    }
                    else {
                        //TODO Add remote entry on local side
                        /*if(processedEntries.contains(rkey)) {
                            Log.w(TAG, "Already processed from server: skipping " + rkey);
                        }*/
                        String id = CalendarHelper.addCalendarEntry(mContext, account, rval);
                        Log.i(TAG, "Added entry " + id);
                    }

                    //flag entry as processed
                   //todo flag correctly processed entries (what if the entry is not updated or added? it is probably deleted?
                   processedEntries.add(rkey);

                }

                /*FindItemsResults<Appointment> appointments = eh.getCalendarItems();
                for (Appointment appointment : appointments.getItems())
                {
                    if(appointment == null) {
                        Log.e(TAG, "Got a null appointment!");
                        continue;
                    }

                    //CalendarEntry.createFromAppointment(appointment);
                    //Test if appointment already exists
                    Appointment a = Appointment.bind(service, appointment.getId());
                    String ei = CalendarHelper.addCalendarEntry(mContext.getContentResolver(), Long.parseLong(id), a);
                    Log.i(TAG, "Calendar entry created " + ei);
                }*/

            }/* catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } */catch (Exception e) {
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

    private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
    }
}
      /*long id = CalendarHelper.fetchCalendars(account, context.getContentResolver());
            if(id == -1) {
                System.out.println("Adding...");
                CalendarHelper.addCalendar(account, context.getContentResolver());
            }*/

            /*
            System.out.println("Deleting");
            for(int i=0;i<10;i++)
                CalendarHelper.deleteCalendar(i, account, context.getContentResolver());
            */

            /*System.out.println("After add");
            id = CalendarHelper.fetchCalendars(account, context.getContentResolver());
            if(id == -1) {
                System.out.println("Does not exist... returning");
                return;
            }*/