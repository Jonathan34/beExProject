package com.bedroid.beEx.helper;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import com.bedroid.beEx.entity.CalendarEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.Attendee;
import microsoft.exchange.webservices.data.AttendeeCollection;
import microsoft.exchange.webservices.data.MeetingResponseType;
import microsoft.exchange.webservices.data.ServiceLocalException;

public class CalendarHelper {
    private static final String TAG = "CalendarHelper";

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

        Uri calUri = buildCalUri(account);
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

    public static String addCalendarEntry(ContentResolver cr, long cal_id, Appointment appointment
    /*, long event_id, long start, long end, String title, String descr, boolean isAllDay*/) throws ServiceLocalException {
        // Insert Event
        ContentValues values = CalendarEntry.getContentValuesFromAppointment(cal_id, appointment);

        //values.put(CalendarContract.Events.SYNC_DATA1, appointment.getId().getUniqueId());
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        // Retrieve ID for new event
        String eventID = uri.getLastPathSegment();

        // adding an attendee:
        AttendeeCollection req = appointment.getRequiredAttendees();
        AttendeeCollection opt = appointment.getOptionalAttendees();
        AttendeeCollection res = appointment.getResources();
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
        }
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

    public static void clearCalendarEntries(ContentResolver cr, Account account, String cal_id) {
        String selection = "(" + CalendarContract.Events.CALENDAR_ID + " = ?)";

        String[] selectionArgs = new String[] { cal_id };
        int rows = cr.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs);
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
        return -1;
    }

    public static List<CalendarEntry> loadFromLocalCalendar(Context context, Account account) {
        List<CalendarEntry> result = new ArrayList<CalendarEntry>();

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(CalendarContract.Events.CONTENT_URI, null, null, null, null);
        while (c.moveToNext()) {
            CalendarEntry ce = new CalendarEntry();
            ce.setId(c.getLong(c.getColumnIndex(CalendarContract.Events._ID)));
            ce.setColor(c.getInt(c.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR)));
            ce.setTimeZone(TimeZone.getTimeZone(c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE))));
            ce.setStart
                    //TODO

            ce.setc.getString(c.getColumnIndex(CalendarContract.Events.DTSTART));
            c.getString(c.getColumnIndex(CalendarContract.Events.DTEND));
            c.getString(c.getColumnIndex(CalendarContract.Events.DURATION));
            c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION));
            c.getString(c.getColumnIndex(CalendarContract.Events.ALL_DAY));
            c.getString(c.getColumnIndex(CalendarContract.Events.AVAILABILITY));
            c.getString(c.getColumnIndex(CalendarContract.Events.DESCRIPTION));
            c.getString(c.getColumnIndex(CalendarContract.Events.STATUS));
            //result.add(CalendarContract.Calendars.NAME, );
			//result.add(CalendarContract.Calendars.ACCOUNT_NAME, c.getString(c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)));
			//result.add(CalendarContract.Calendars.ACCOUNT_TYPE, c.getString(c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)));
			
			//result.add(CalendarContract.Calendars.NAME, c.getString(c.getColumnIndex(CalendarContract.Calendars.NAME)));
			//result.add(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, c.getString(c.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)));

			//result.add(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)));
			//result.add(CalendarContract.Calendars.OWNER_ACCOUNT, c.getString(c.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)));
			//result.add(CalendarContract.Calendars.VISIBLE, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.VISIBLE)));
			//result.add(CalendarContract.Calendars.SYNC_EVENTS, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)));

			//result.add(CalendarContract.Calendars._SYNC_ID, c.getString(c.getColumnIndex(CalendarContract.Calendars._SYNC_ID)));

			//result.add(CalendarContract.Calendars.DIRTY, ""+c.getLong(c.getColumnIndex(CalendarContract.Calendars.DIRTY)));
			//result.add(CalendarContract.Calendars.DELETED, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.DELETED)));
			//result.add(CalendarContract.Calendars.MAX_REMINDERS, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.MAX_REMINDERS)));
			
			//result.add(CalendarContract.Calendars.ALLOWED_REMINDERS, c.getString(c.getColumnIndex(CalendarContract.Calendars.ALLOWED_REMINDERS)));
			//result.add(CalendarContract.Calendars.ALLOWED_AVAILABILITY, c.getString(c.getColumnIndex(CalendarContract.Calendars.ALLOWED_AVAILABILITY)));
			//result.add(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, c.getString(c.getColumnIndex(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES)));
			
			//result.add(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE)));
			//result.add(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND)));
			//result.add(CalendarContract.Calendars.CAN_PARTIALLY_UPDATE, ""+c.getInt(c.getColumnIndex(CalendarContract.Calendars.CAN_PARTIALLY_UPDATE)));
			
			//result.add(CalendarContract.Calendars.CALENDAR_LOCATION, c.getString(c.getColumnIndex(CalendarContract.Calendars.CALENDAR_LOCATION)));

			/*list.put(Calendars.CAL_SYNC1, c.getString(c.getColumnIndex(Calendars.CAL_SYNC1)));
			list.put(Calendars.CAL_SYNC2, c.getString(c.getColumnIndex(Calendars.CAL_SYNC2)));
			list.put(Calendars.CAL_SYNC3,c.getString(c.getColumnIndex(Calendars.CAL_SYNC3)));
			list.put(Calendars.CAL_SYNC4,c.getString(c.getColumnIndex(Calendars.CAL_SYNC4)));
			list.put(Calendars.CAL_SYNC5,c.getString(c.getColumnIndex(Calendars.CAL_SYNC5)));
			list.put(Calendars.CAL_SYNC6,c.getString(c.getColumnIndex(Calendars.CAL_SYNC6)));
			list.put(Calendars.CAL_SYNC7,c.getString(c.getColumnIndex(Calendars.CAL_SYNC7)));
			list.put(Calendars.CAL_SYNC8,c.getString(c.getColumnIndex(Calendars.CAL_SYNC8)));
			list.put(Calendars.CAL_SYNC9,c.getString(c.getColumnIndex(Calendars.CAL_SYNC9)));
			list.put(Calendars.CAL_SYNC10,c.getString(c.getColumnIndex(Calendars.CAL_SYNC10)));*/

            /*StringBuilder sb = new StringBuilder("--CALENDAR DUMP--");
			for (Entry<String, String> s: list.entrySet()) {
				sb.append("("+s.getKey()+"|"+s.getValue()+")");
			}
			sb.append("----------");
			Log.i(C.TAG, sb.toString());
			list.clear();*/
        }

        return result;
    }


    public static List<CalendarEntry> loadFromRemoteCalendar(Context context) {
        List<CalendarEntry> result = new ArrayList<CalendarEntry>();

        //TODO

        return result;
    }
}
