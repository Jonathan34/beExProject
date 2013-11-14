package com.bedroid.beEx.sync;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.support.v7.appcompat.R;
import android.util.Log;

import com.bedroid.beEx.helper.CalendarHelper;
import com.bedroid.beEx.helper.ExchangeHelper;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindItemsResults;

public class CalendarSyncAdapterService extends Service {
    private static final String TAG = "CalendarSyncAdapterService";
    private static SyncAdapterImpl sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;



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
                /*new */CalendarSyncAdapterService/*()*/.performSync(mContext, account, extras, authority, provider, syncResult);
            } catch (OperationCanceledException e) {
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
        //mContentResolver = context.getContentResolver();
        Log.i(TAG, "performSync: " + account.toString());
        //This is where the magic will happen!
        //TODO
        try {
            ExchangeHelper eh = ExchangeHelper.getInstance();
            ExchangeService service = eh.connectToExchange(context);

            System.out.println("Check if exist");
            long id = CalendarHelper.fetchCalendars(account, context.getContentResolver());
            if(id == -1) {
                System.out.println("Adding...");
                CalendarHelper.addCalendar(account, context.getContentResolver());
            }

            /*
            System.out.println("Deleting");
            for(int i=0;i<10;i++)
                CalendarHelper.deleteCalendar(i, account, context.getContentResolver());
            */

            System.out.println("After add");
            id = CalendarHelper.fetchCalendars(account, context.getContentResolver());
            if(id == -1) {
                System.out.println("Does not exist... returning");
                return;
            }

            FindItemsResults<Appointment> appointments = eh.getCalendarItems();
            for (Appointment appointment : appointments.getItems())
            {
                String when = appointment.getWhen();
                Date start = appointment.getStart();
                Date end = appointment.getEnd();

                String ei = CalendarHelper.addCalendarEntry(context.getContentResolver(),
                                            id, start.getTime(), end.getTime(), appointment.getSubject(), "Descr", appointment.getIsAllDayEvent());
                System.out.println("entry created" + ei);
                /*Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType("vnd.android.cursor.item/event");
                intent.setData(CalendarContract.Events.CONTENT_URI);
                intent.putExtra(CalendarContract.Events.CALENDAR_DISPLAY_NAME, account.name);
                intent.putExtra(CalendarContract.Events.DTSTART, start.getTime());
                intent.putExtra(CalendarContract.Events.ALL_DAY, appointment.getIsAllDayEvent());
                //intent.putExtra("rrule", "FREQ=YEARLY");
                intent.putExtra(CalendarContract.Events.DTEND, end.getTime());
                intent.putExtra(CalendarContract.Events.TITLE, appointment.getSubject());
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.getTime());
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.getTime());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);*/
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
