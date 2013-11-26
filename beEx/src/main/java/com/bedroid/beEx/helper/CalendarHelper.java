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
import android.text.format.DateUtils;
import android.util.Log;

import com.bedroid.beEx.adapter.AndroidAdapter;
import com.bedroid.beEx.adapter.ExchangeAdapter;
import com.bedroid.beEx.adapter.IAdapter;
import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.entity.People;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    public static String addCalendarEntry(Context context, Account account, CalendarEntry c) throws ServiceLocalException {
        ContentResolver cr = context.getContentResolver();

        // get calendar id: when adding a new item from remote, the calendar id is not set
        String cal_id = CalendarHelper.getCalendarId(context, account);
        if(cal_id == null) {
            Log.e(TAG, "Error calendar id is incorrect");
            return null;
        }
        c.setCalendarId(Long.parseLong(cal_id));
        Log.d(TAG, " calendar id is " + cal_id);

        //get content values from the calendar entry
        ContentValues values = CalendarEntry.getContentValues(c);

        //do the insert query
        Uri syncUri = CalendarHelper.buildCalUri(CalendarContract.Events.CONTENT_URI, account, "com.bedroid.beEx.account");
        Uri uri = cr.insert(syncUri, values);

        // Retrieve ID for new event
        String eventID = uri.getLastPathSegment();

        //add people
        CalendarHelper.addPeople(cr, account, eventID, c);

        return eventID;
    }

    public static int deleteCalendarEntry(ContentResolver cr, long event_id) {
        String[] selArgs = new String[]{Long.toString(event_id)};
        int deleted =
                cr.delete(CalendarContract.Events.CONTENT_URI,
                        CalendarContract.Events._ID + " =? ",
                        selArgs);
        return deleted;
    }

    public static int updateEntry(Context context, Account account, CalendarEntry local, CalendarEntry remote) {
        //get all content values from the calendar entry

        //update the remote calendar id for the update
        remote.setCalendarId(local.getCalendarId());

        ContentValues values = CalendarEntry.getContentValues(remote);
        ContentResolver cr = context.getContentResolver();
        String[] selArgs = new String[]{local.getEventId()};

        Uri syncUri = CalendarHelper.buildCalUri(CalendarContract.Events.CONTENT_URI, account, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/);
        //Uri uri = cr.insert(syncUri, values);

        int updated =
                cr.
                        update(
                                syncUri,
                                values,
                                CalendarContract.Events._ID + " =? ",
                                selArgs);

        System.out.println(updated);
        CalendarHelper.updatePeople(context.getContentResolver(), account, local.getEventId(), remote);
        return updated;
    }

    private static ArrayList<ContentValues> getPeopleContentValues(String eventID, HashSet<People> peopleList, int type) {
        ArrayList<ContentValues> result = new ArrayList<ContentValues>();
        ContentValues values;

        for (People p : peopleList){
            values = new ContentValues();
            values.put(CalendarContract.Attendees.EVENT_ID, eventID);
            values.put(CalendarContract.Attendees.ATTENDEE_TYPE, type);
            values.put(CalendarContract.Attendees.ATTENDEE_NAME, p.getName());
            values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, p.getEmail());
            values.put(CalendarContract.Attendees.ATTENDEE_STATUS, p.getResponseStatus());
            result.add(values);
        }
        return result;
    }

    private static void addPeople(ContentResolver cr, Account account, String eventID, CalendarEntry c) {
        ContentValues values = new ContentValues();

        Uri syncUri = CalendarHelper.buildCalUri(CalendarContract.Attendees.CONTENT_URI, account, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/);

        ArrayList<ContentValues> list;
        list = CalendarHelper.getPeopleContentValues(eventID, c.getRequiredPeople(), CalendarContract.Attendees.TYPE_REQUIRED);
        for(ContentValues v : list) {
            cr.insert(syncUri, v);
        }
        list.clear();

        list = CalendarHelper.getPeopleContentValues(eventID, c.getOptionalPeople(), CalendarContract.Attendees.TYPE_OPTIONAL);
        for(ContentValues v : list) {
            cr.insert(syncUri, v);
        }
        list.clear();

        list = CalendarHelper.getPeopleContentValues(eventID, c.getResources(), CalendarContract.Attendees.TYPE_RESOURCE);
        for(ContentValues v : list) {
            cr.insert(syncUri, v);
        }
        list.clear();
    }

    private static void updatePeople(ContentResolver cr, Account account, String eventID, CalendarEntry c) {
        String[] selArgs = new String[]{eventID};

        Uri syncUri = CalendarHelper.buildCalUri(CalendarContract.Attendees.CONTENT_URI, account, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/);

        ArrayList<ContentValues> list;
        list = CalendarHelper.getPeopleContentValues(eventID, c.getRequiredPeople(), CalendarContract.Attendees.TYPE_REQUIRED);
        for(ContentValues v : list) {
            cr.update(syncUri, v, CalendarContract.Attendees._ID + " =? ", selArgs);
        }
        list.clear();

        list = CalendarHelper.getPeopleContentValues(eventID, c.getOptionalPeople(), CalendarContract.Attendees.TYPE_OPTIONAL);
        for(ContentValues v : list) {
            cr.update(syncUri, v, CalendarContract.Attendees._ID + " =? ", selArgs);
        }
        list.clear();

        list = CalendarHelper.getPeopleContentValues(eventID, c.getResources(), CalendarContract.Attendees.TYPE_RESOURCE);
        for(ContentValues v : list) {
            cr.update(syncUri, v, CalendarContract.Attendees._ID + " =? ", selArgs);
        }
        list.clear();
    }

    //this is bugged in exchange server, workaround is to loop over all emails to look for the accept message
    public static int getStatusFromAppointment(MeetingResponseType m) {
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
          Log.e(TAG, "Error calendar id is invalid");
            return;
        }

        String selection = "(" + CalendarContract.Events.CALENDAR_ID + " = ?)";

        String[] selectionArgs = new String[] { cal_id };
        int rows = context.getContentResolver().delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs);
        Log.i(TAG, rows + " events deleted");
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
        cv.put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF008080);  //TODO Configure
        cv.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
        cv.put(CalendarContract.Calendars.VISIBLE, 1);
        cv.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        String tz =  TimeZone.getDefault().getID();
        cv.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, tz);
        return cv;
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

    public static long dumpCalendarEntries(Account account, ContentResolver cr) {
        //Duration is RFC5545

        // Run query
        Cursor cur = null;
        Uri uri = CalendarContract.Instances.CONTENT_URI;


        Uri.Builder builder = uri.buildUpon();
        ContentUris.appendId(builder, System.currentTimeMillis());
        ContentUris.appendId(builder, System.currentTimeMillis() + (DateUtils.DAY_IN_MILLIS+21600000));


        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[] {account.name, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/};
        // Submit the query and get a Cursor object back.
        cur = cr.query(/*uri*/builder.build(), null, selection, selectionArgs, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            // Get the field values
            //long id = cur.getLong(PROJECTION_ID_INDEX);
            //String name = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            //int color = cur.getInt(PROJECTION_COLOR_INDEX);
            for(int i=0; i<cur.getColumnCount(); i++) {
                System.out.println(cur.getColumnName(i) + " -> " + cur.getString(i));
            }
            System.out.println("-----------------------");
        }
        cur.close();
        return -1;
    }

    /*public static long fetchCalendars(Account account, ContentResolver cr) {
        //TODO Replace account.name by class member attrbute?

        // Run query
        Cursor cur = null;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[] {account.name, "com.bedroid.beEx.account"};
        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            // Get the field values
            long id = cur.getLong(PROJECTION_ID_INDEX);
            String name = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            int color = cur.getInt(PROJECTION_COLOR_INDEX);

            if(name == account.name)
                return id;
        }
        cur.close();
        return -1;
    }*/

    /*
    // https://github.com/RHSAndroidDevs/RHSSchoolPlanner/blob/master/src/edu/rhs/school_planner/Homework.java
    public void getEventsModern() {
                mHomeworkAdapter.getHomework().clear();
                //content resolver is how we access android databases like text messages and the calendar
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(CalendarContract.Calendars.CONTENT_URI,
                                new String[]{ CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME, CalendarContract.Calendars.VISIBLE },
                                null, null, null);

                HashSet<String> calendarIds = new HashSet<String>();
                while (cursor.moveToNext()) {
                        final String _id = cursor.getString(0);
                        final String displayName = cursor.getString(1);
                        final Boolean selected = !cursor.getString(2).equals("0");

                        Log.v("test","Id: " + _id + " Display Name: " + displayName + " Selected: " + selected);
                        calendarIds.add(_id);
                }

                for (String id: calendarIds) {
                        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                        ContentUris.appendId(builder, mCalendar.getTimeInMillis());
                        Log.v("calendar",mCalendar.getTime().toString());
                        ContentUris.appendId(builder, mCalendar.getTimeInMillis() + (DateUtils.DAY_IN_MILLIS-21600000));
                        Cursor eventCursor = cr.query(builder.build(),
                                        new String[] { CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                                CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY},
                                CalendarContract.Events.CALENDAR_ID + "=" + id,
                                null, CalendarContract.Instances.BEGIN + " ASC");

                        while (eventCursor.moveToNext()) {
                                final String title = eventCursor.getString(0);
                                final Date begin = new Date(eventCursor.getLong(1));
                                final Date end = new Date(eventCursor.getLong(2));
                                final Boolean allDay = !eventCursor.getString(3).equals("0");

                                Log.v("test","Title: " + title + " Begin: " + begin + " End: " + end +
                                                " All Day: " + allDay);

                                if (allDay) {
                                        begin.setTime(begin.getTime() + DateUtils.DAY_IN_MILLIS);
                                }

                                mHomeworkAdapter.addAssignment(new HomeworkAssignment(title,begin.toString()));
                        }

                        mHomeworkAdapter.notifyDataSetChanged();
                }
        }*/
}
