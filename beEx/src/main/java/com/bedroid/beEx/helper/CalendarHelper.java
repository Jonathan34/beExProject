package com.bedroid.beEx.helper;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.TimeZone;

/**
 * Created by Jon on 11/14/13.
 */
public class CalendarHelper {
    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    private static final String[] EVENT_PROJECTION = new String[] {
            /*CalendarContract.Calendars._ID, // 0
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, // 1
            CalendarContract.Calendars.CALENDAR_COLOR // 2*/
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 1;
    private static final int PROJECTION_COLOR_INDEX = 2;

    public static void addCalendar(Account account, ContentResolver cr) {
        if (account == null)
            throw new IllegalArgumentException();

        final ContentValues cv = buildContentValues(account);

        Uri calUri = buildCalUri(account);
        cr.insert(calUri, cv);
    }

    public static String addCalendarEntry(ContentResolver cr, long id, long start, long end, String title, String descr, boolean isAllDay) {
        // Insert Event
        ContentValues values = new ContentValues();
        TimeZone timeZone = TimeZone.getDefault();
        values.put(CalendarContract.Events.DTSTART, start);
        values.put(CalendarContract.Events.DTEND, end);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.DESCRIPTION, descr);
        values.put(CalendarContract.Events.CALENDAR_ID, id);
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        // Retrieve ID for new event
        String eventID = uri.getLastPathSegment();
        return eventID;
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

    public static long fetchCalendars(Account account, ContentResolver cr) {
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

            if(name == account.name)
                return id;
        }
        return -1;
    }
}
