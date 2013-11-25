package com.bedroid.beEx.adapter;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.entity.People;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class AndroidAdapter extends GenericAdapter {

    public AndroidAdapter(Context c, Account a) {
        super(c, a);
    }

    @Override
    public IAdapter createInstance(Context c,  Account a) {
        return new AndroidAdapter(c, a);
    }

    @Override
    public HashMap<String, CalendarEntry> getAppointments() {
        /****TEST*****/
        // Compute number of dirty events in the calendar
        final String[] EVENTS_PROJECTION = new String[]{
                CalendarContract.Events._ID,
        };
        String dirtyWhere = CalendarContract.Events.CALENDAR_ID + "=" + 1
                + " AND " + CalendarContract.Events.DIRTY + "=1";
        Cursor dirtyCursor = mContext.getContentResolver().query(
                CalendarContract.Events.CONTENT_URI, null, dirtyWhere,
                null, null);
        int dirtyCount = 0;
        try {
            dirtyCount = dirtyCursor.getCount();
        } finally {
            dirtyCursor.close();
        }
        /****TEST*****/



        HashMap<String, CalendarEntry> result = new HashMap<String, CalendarEntry>();

        ContentResolver cr = mContext.getContentResolver();
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[] {mAccount.name, "com.bedroid.beEx.account"/*CalendarContract.ACCOUNT_TYPE_LOCAL*/};

        Uri instanceUri = CalendarContract.Events.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, mAccount.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.bedroid.beEx.account")
                .build();

        Cursor c = cr.query(instanceUri, null, selection, selectionArgs, null);
        while (c.moveToNext()) {
            CalendarEntry ce = new CalendarEntry();

            ce.setCalendarId(c.getLong(c.getColumnIndex(CalendarContract.Events._ID)));
            ce.setColor(c.getInt(c.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR)));

            String tz = c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE));
            String tze = c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_END_TIMEZONE));
            ce.setTimeZone(TimeZone.getTimeZone(tz));
            ce.setEndTimeZone(TimeZone.getTimeZone(tze));

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Long.parseLong(c.getString(c.getColumnIndex(CalendarContract.Events.DTSTART))));
            ce.setStart(cal.getTime());
            //Works: ce.setStart(new Date(Long.parseLong(c.getString(c.getColumnIndex(CalendarContract.Events.DTSTART)))));

            cal.setTimeInMillis(Long.parseLong(c.getString(c.getColumnIndex(CalendarContract.Events.DTEND))));
            ce.setEnd(cal.getTime());
            //Works: ce.setEnd(new Date(Long.parseLong(c.getString(c.getColumnIndex(CalendarContract.Events.DTSTART)))));

            //ce.setDuration(c.getString(c.getColumnIndex(CalendarContract.Events.DURATION)));
            ce.setLocation(c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)));
            ce.setUid(c.getString(c.getColumnIndex(CalendarContract.Events.UID_2445)));
            ce.setAllDay(c.getInt(c.getColumnIndex(CalendarContract.Events.ALL_DAY)) == 1 ? true : false);
            ce.setDescription(c.getString(c.getColumnIndex(CalendarContract.Events.DESCRIPTION)));
            ce.setTitle(c.getString(c.getColumnIndex(CalendarContract.Events.TITLE)));
            ce.setStatus(c.getString(c.getColumnIndex(CalendarContract.Events.STATUS)));

            cal.setTimeInMillis(c.getLong(c.getColumnIndex(CalendarContract.Events.SYNC_DATA1)));
            ce.setLastModificationTime(cal.getTime());

            String organizer = "";
            String adressOrganizer = c.getString(c.getColumnIndex(CalendarContract.Events.ORGANIZER));
            ce.setOrganizer(new People(organizer, adressOrganizer));

            long dirty = c.getLong(c.getColumnIndex(CalendarContract.Events.DIRTY));
            ce.setDirty(dirty != 0);
            ce.setDeleted(c.getLong(c.getColumnIndex(CalendarContract.Events.DELETED)) != 0);

            result.put(ce.getKey(), ce);

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
        c.close();
        return result;
    }
}
