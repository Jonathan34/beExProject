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

import com.bedroid.beEx.helper.ExchangeHelper;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindItemsResults;

public class CalendarSyncAdapterService extends Service {
    private static final String TAG = "CalendarSyncAdapterService";
    private static SyncAdapterImpl sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    private static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID, // 0
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, // 1
            CalendarContract.Calendars.CALENDAR_COLOR // 2
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 1;
    private static final int PROJECTION_COLOR_INDEX = 2;

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
                new CalendarSyncAdapterService().performSync(mContext, account, extras, authority, provider, syncResult);
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

    private static ContentValues buildContentValues(Account account) {
        final ContentValues cv = new ContentValues();
        cv.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
        cv.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        cv.put(CalendarContract.Calendars.NAME, account.name);
        cv.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, account.name);
        cv.put(CalendarContract.Calendars.CALENDAR_COLOR, 2);  //Calendar.getColor() returns int
        cv.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
        cv.put(CalendarContract.Calendars.VISIBLE, 1);
        cv.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        return cv;
    }

    public static void fetchCalendars(Account account, ContentResolver cr) {
        //TODO Replace account.name by class member attrbute?

        // Run query
        Cursor cur = null;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[] {account.name, CalendarContract.ACCOUNT_TYPE_LOCAL};
        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            long id = 0;
            String name = null;
            int color;

            // Get the field values
            id = cur.getLong(PROJECTION_ID_INDEX);
            name = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            color = cur.getInt(PROJECTION_COLOR_INDEX);

            System.out.println(id);
            System.out.println(name);
            System.out.println(color);
        }
    }

    public static void addCalendar(Account account, ContentResolver cr) {
        if (account == null)
            throw new IllegalArgumentException();

        final ContentValues cv = buildContentValues(account);

        Uri calUri = buildCalUri(account);
        cr.insert(calUri, cv);
    }

    public static boolean deleteCalendar(long id, Account account, ContentResolver cr) {
        if (id < 0)
            throw new IllegalArgumentException();

        Uri calUri = ContentUris.withAppendedId(buildCalUri(account), id);
        return cr.delete(calUri, null, null) == 1;
    }

    private static Uri buildCalUri(Account account) {
        return CalendarContract.Calendars.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
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

            //TODO Check if calendar is created
            System.out.println("Before add");
            CalendarSyncAdapterService.fetchCalendars(account, context.getContentResolver());

            System.out.println("Deleting");
            for(int i=0;i<10;i++)
                CalendarSyncAdapterService.deleteCalendar(i, account, context.getContentResolver());

            System.out.println("Adding...");
            CalendarSyncAdapterService.addCalendar(account, context.getContentResolver());
            System.out.println("After add");
            CalendarSyncAdapterService.fetchCalendars(account, context.getContentResolver());
            //End TODO

            FindItemsResults<Appointment> appointments = eh.getCalendarItems();
            for (Appointment appointment : appointments.getItems())
            {
                String when = appointment.getWhen();
                Date start = appointment.getStart();
                Date end = appointment.getEnd();

                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra(CalendarContract.Events.CALENDAR_DISPLAY_NAME, account.name);
                intent.putExtra(CalendarContract.Events.DTSTART, start.getTime());
                intent.putExtra(CalendarContract.Events.ALL_DAY, appointment.getIsAllDayEvent());
                //intent.putExtra("rrule", "FREQ=YEARLY");
                intent.putExtra(CalendarContract.Events.DTEND, end.getTime());
                intent.putExtra(CalendarContract.Events.TITLE, appointment.getSubject());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
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
