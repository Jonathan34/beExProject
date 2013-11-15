package com.bedroid.beEx.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import com.bedroid.beEx.entity.CalendarEntity;

import java.util.Date;
import java.util.TimeZone;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.Attendee;
import microsoft.exchange.webservices.data.AttendeeCollection;
import microsoft.exchange.webservices.data.MeetingResponseType;
import microsoft.exchange.webservices.data.ServiceLocalException;

import static java.lang.Long.*;

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
            CalendarContract.Calendars.CALENDAR_COLOR
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 1;
    private static final int PROJECTION_COLOR_INDEX = 2;


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
        ContentValues values = CalendarEntity.getContentValuesFromAppointment(cal_id, appointment);

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
}
