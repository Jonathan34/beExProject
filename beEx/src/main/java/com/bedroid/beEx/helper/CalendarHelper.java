package com.bedroid.beEx.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import com.bedroid.beEx.adapter.AndroidAdapter;
import com.bedroid.beEx.adapter.ExchangeAdapter;
import com.bedroid.beEx.adapter.IAdapter;
import com.bedroid.beEx.entity.CalendarEntry;

import java.util.HashMap;
import java.util.TimeZone;

import microsoft.exchange.webservices.data.MeetingResponseType;
import microsoft.exchange.webservices.data.ServiceLocalException;

public class CalendarHelper {
    private static final String TAG = "CalendarHelper";

    public enum CalendarType {
        ANDROID_CALENDAR,
        EXCHANGE_CALENDAR;

        public static CalendarType fromString(String name) {
            return getEnumFromString(CalendarType.class, name);
        }
    }

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
            CalendarContract.Calendars.CALENDAR_COLOR,
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
    private static final int PROJECTION_NAME_INDEX = 3;
    private static final int PROJECTION_COLOR_INDEX = 4;


    public static String addCalendar(Account account, ContentResolver cr) {
        if (account == null)
            throw new IllegalArgumentException();

        final ContentValues cv = buildContentValues(account);

        //v1
        //Uri calUri = buildCalUri(CalendarContract.Calendars.CONTENT_URI, account, CalendarContract.ACCOUNT_TYPE_LOCAL);
        //Uri uri = cr.insert(calUri, cv);

        //v2
        Uri calUri = buildCalUri(CalendarContract.Calendars.CONTENT_URI, account, "com.bedroid.beEx.account");
        Uri uri = cr.insert(calUri, cv);

        String eventID = uri.getLastPathSegment();
        return eventID;
    }

    public boolean getCalendarEvent(ContentResolver cr, long event_id) {
        String[] proj = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.TITLE};
        Cursor cursor = cr.
                        query(
                                CalendarContract.Events.CONTENT_URI,
                                proj,
                                CalendarContract.Events._ID + " = ? ",
                                new String[]{Long.toString(event_id)},
                                null);
        if (cursor.moveToFirst()) {
            // read event data
            Log.i(TAG, "Found a matching entry for event id: " + event_id);
            return true;
        }
        Log.i(TAG, "Impossible to find a matching entry for event id: " + event_id);
        return false;
    }

    public static String addCalendarEntry(Context context, Account account, /*long cal_id,*/ CalendarEntry c) throws ServiceLocalException {
        // Insert Event
        String cal_id = CalendarHelper.getCalendarId(context, account);
        if(cal_id == null) {
            Log.e(TAG, "Error jersdf");
            return null;
        }
        c.setCalendarId(Long.parseLong(cal_id));
        ContentValues values = CalendarEntry.getContentValues(c);
        ContentResolver cr = context.getContentResolver();

        //values.put(CalendarContract.Events.SYNC_DATA1, appointment.getId().getUniqueId());
        //Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        Uri syncUri = CalendarHelper.buildCalUri(CalendarContract.Events.CONTENT_URI, account, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/);
        Uri uri = cr.insert(syncUri, values);

        /*****
         * To query:
         * cur = cr.query(Uri.parse("content://com.android.calendar/events"), null, Events.DIRTY+" = ?", new String[]{String.valueOf(1)}, null);
         *  while (cur.moveToNext()) {
         *  int id = (int) cur.getLong(cur.getColumnIndex(Events._ID));
         */
        // Retrieve ID for new event
        String eventID = uri.getLastPathSegment();

        // TODO adding an attendee:
        /*AttendeeCollection req = c.getRequiredAttendees();
        AttendeeCollection opt = c.getOptionalAttendees();
        AttendeeCollection res = c.getResources();
        for (Attendee a : req){
            values.clear();
            values.put(CalendarContract.Attendees.EVENT_ID, eventID);
            values.put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_REQUIRED);
            values.put(CalendarContract.Attendees.ATTENDEE_NAME, a.getName());
            values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, a.getAddress());
            values.put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarHelper.getStatusFormAppointment(a.getResponseType()));
            cr.insert(CalendarContract.Attendees.CONTENT_URI, values);
        }
        for (Attendee a : opt){
            values.clear();
            values.put(CalendarContract.Attendees.EVENT_ID, eventID);
            values.put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_OPTIONAL);
            values.put(CalendarContract.Attendees.ATTENDEE_NAME, a.getName());
            values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, a.getAddress());
            values.put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarHelper.getStatusFormAppointment(a.getResponseType()));
            cr.insert(CalendarContract.Attendees.CONTENT_URI, values);
        }
        for (Attendee a : res){
            values.clear();
            values.put(CalendarContract.Attendees.EVENT_ID, eventID);
            values.put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_RESOURCE);
            values.put(CalendarContract.Attendees.ATTENDEE_NAME, a.getName());
            values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, a.getAddress());
            values.put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarHelper.getStatusFormAppointment(a.getResponseType()));
            cr.insert(CalendarContract.Attendees.CONTENT_URI, values);
        }*/
        return eventID;
    }

    //this is bugged in exchange server, workaround is to loop over all emails to look for the accept message
    private static int getStatusFormAppointment(MeetingResponseType m) {
        if(m == MeetingResponseType.Accept)
            return CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED;
        else if(m == MeetingResponseType.NoResponseReceived)
            return CalendarContract.Attendees.ATTENDEE_STATUS_INVITED;
        else if(m == MeetingResponseType.Decline)
            return CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;
        else if(m == MeetingResponseType.Tentative)
            return CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE;
        else if(m == MeetingResponseType.Unknown)
            return CalendarContract.Attendees.ATTENDEE_STATUS_NONE;
        return CalendarContract.Attendees.ATTENDEE_STATUS_NONE;
    }

    private static String getCalendarId(Context context, Account account) {
        AccountManager am = AccountManager.get(context);
        String cal_id = am.getUserData(account, "CALENDAR_ID");
        if(cal_id == null) {
            Log.e(TAG, "The calendar ID associated with the account is invalid");
            return null;
        }
        return cal_id;
    }

    public static void clearCalendarEntries(Context context, Account account) {
        String cal_id = CalendarHelper.getCalendarId(context, account);
        if(cal_id == null) {
          Log.e(TAG, "Error jersdf");
            return;
        }

        String selection = "(" + CalendarContract.Events.CALENDAR_ID + " = ?)";

        String[] selectionArgs = new String[] { cal_id };
        int rows = context.getContentResolver().delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs);
        Log.i(TAG, rows + " events deleted");
    }

    public static int deleteCalendarEntry(ContentResolver cr, long event_id) {
        String[] selArgs = new String[]{Long.toString(event_id)};
        int deleted =
                cr.delete(CalendarContract.Events.CONTENT_URI,
                        CalendarContract.Events._ID + " =? ",
                        selArgs);
        return deleted;
    }

    public static boolean deleteCalendar(long id, Account account, ContentResolver cr) {
        if (id < 0)
            throw new IllegalArgumentException();

        Uri calUri = ContentUris.withAppendedId(buildCalUri(CalendarContract.Calendars.CONTENT_URI, account, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/), id);
        return cr.delete(calUri, null, null) == 1;
    }

    private static Uri buildCalUri(Uri uri, Account account, String accountType) {
        return uri
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build();
    }

    private static ContentValues buildContentValues(Account account) {
        final ContentValues cv = new ContentValues();
        cv.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
        cv.put(CalendarContract.Calendars.ACCOUNT_TYPE, /*CalendarContract.ACCOUNT_TYPE_LOCAL*/"com.bedroid.beEx.account");
        cv.put(CalendarContract.Calendars.NAME, account.name);
        cv.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, account.name);
        cv.put(CalendarContract.Calendars.CALENDAR_COLOR, 4);  //TODO Configure
        cv.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
        cv.put(CalendarContract.Calendars.VISIBLE, 1);
        cv.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        cv.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());
        return cv;
    }

    public static long fetchCalendars(Account account, ContentResolver cr) {
        //TODO Replace account.name by class member attrbute?

        // Run query
        Cursor cur = null;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[] {account.name, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/};
        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            // Get the field values
            long id = cur.getLong(PROJECTION_ID_INDEX);
            String name = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            int color = cur.getInt(PROJECTION_COLOR_INDEX);

            /*System.out.println(name);
            System.out.println(id);
            System.out.println(color);*/

            if(name == account.name)
                return id;
        }
        cur.close();
        return -1;
    }

    public static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string)
    {
        if( c != null && string != null ) {
            try {
                return Enum.valueOf(c, string.trim().toUpperCase());
            }
            catch(IllegalArgumentException ex) { }
        }
        return null;
    }

    public static IAdapter getAndroidCalendarAdapter(Context context, Account account) {
        return new AndroidAdapter(context, account);
    }

    public static IAdapter getCalendarAdapter(Context context, Account account) {
        IAdapter adapter = null;
        AccountManager am = AccountManager.get(context);
        CalendarType type = CalendarType.fromString(am.getUserData(account, "ADAPTER_TYPE"));
        if(type == CalendarHelper.CalendarType.ANDROID_CALENDAR)
            return new AndroidAdapter(context, account);
        else if(type == CalendarHelper.CalendarType.EXCHANGE_CALENDAR)
            return new ExchangeAdapter(context, account);
        return adapter;
    }
}
