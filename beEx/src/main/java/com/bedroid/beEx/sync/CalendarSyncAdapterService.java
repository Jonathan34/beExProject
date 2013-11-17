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

import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.helper.CalendarHelper;
import com.bedroid.beEx.helper.ExchangeHelper;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

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
                //mContentResolver = context.getContentResolver();
                Log.i(TAG, "performSync: " + account.toString());
                //This is where the magic will happen!
                //TODO
                ExchangeHelper eh = ExchangeHelper.getInstance();
                ExchangeService service = eh.connectToExchange(mContext);

                // 1.a Retrieve local items
                List<CalendarEntry> localItems = CalendarHelper.loadFromLocalCalendar(mContext, account);

                // 1.b Retrieve remote items
                List<CalendarEntry> remoteItems = CalendarHelper.loadFromRemoteCalendar(mContext);
                System.out.println("Check if exist");
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
                AccountManager am = AccountManager.get(mContext);
                String id = am.getUserData(account, "CALENDAR_ID");
                if(id == null) {
                    Log.e(TAG, "The calendar ID associated with the account is invalid");
                    return;
                }

                //clears up everything we have
                CalendarHelper.clearCalendarEntries(mContext.getContentResolver(), account, id);

                FindItemsResults<Appointment> appointments = eh.getCalendarItems();
                for (Appointment appointment : appointments.getItems())
                {
                    if(appointment == null) {
                        Log.e(TAG, "Got a null appointment!");
                        continue;
                    }

                    //CalendarEntry.createFromAppointment(appointment);
                    //Test if appointment already exists
                    /*String when = appointment.getWhen();
                    Date start = appointment.getStart();
                    Date end = appointment.getEnd();*/
                    Appointment a = Appointment.bind(service, appointment.getId());
                    String ei = CalendarHelper.addCalendarEntry(mContext.getContentResolver(), Long.parseLong(id), a);
                    Log.i(TAG, "Calendar entry created " + ei);
                }

            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
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

    private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
    }
}
