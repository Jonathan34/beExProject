package com.bedroid.beEx.adapter;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.entity.People;
import com.bedroid.beEx.helper.CalendarHelper;

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

    /**
     * Get Appointments from the stock calendar
     * @return map with calendar entries
     */
    @Override
    public HashMap<String, CalendarEntry> getAppointments() {
        HashMap<String, CalendarEntry> result = new HashMap<String, CalendarEntry>();

        ContentResolver cr = mContext.getContentResolver();
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[] {mAccount.name, "com.bedroid.beEx.account"};

        Uri instanceUri = CalendarContract.Events.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, mAccount.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.bedroid.beEx.account")
                .build();

        Cursor c = cr.query(instanceUri, null, selection, selectionArgs, null);
        while (c.moveToNext()) {
            CalendarEntry ce = new CalendarEntry();

            ce.setEventId(c.getString(c.getColumnIndex(CalendarContract.Events._ID)));
            ce.setCalendarId(c.getLong(c.getColumnIndex(CalendarContract.Events.CALENDAR_ID)));
            ce.setColor(c.getInt(c.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR)));

            String tz = c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE));
            String tze = c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_END_TIMEZONE));
            ce.setTimeZone(TimeZone.getTimeZone(tz));
            if(tze != null) //tze=null if event is created from stock calendar
                ce.setEndTimeZone(TimeZone.getTimeZone(tze));
            else
                ce.setEndTimeZone(TimeZone.getTimeZone(tz));

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Long.parseLong(c.getString(c.getColumnIndex(CalendarContract.Events.DTSTART))));
            ce.setStart(cal.getTime());

            cal.setTimeInMillis(Long.parseLong(c.getString(c.getColumnIndex(CalendarContract.Events.DTEND))));
            ce.setEnd(cal.getTime());

            ce.setLocation(c.getString(c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)));
            ce.setUid(c.getString(c.getColumnIndex(CalendarContract.Events.UID_2445)));
            ce.setAllDay(c.getInt(c.getColumnIndex(CalendarContract.Events.ALL_DAY)) == 1 ? true : false);
            ce.setDescription(c.getString(c.getColumnIndex(CalendarContract.Events.DESCRIPTION)));
            ce.setTitle(c.getString(c.getColumnIndex(CalendarContract.Events.TITLE)));

            cal.setTimeInMillis(c.getLong(c.getColumnIndex(CalendarContract.Events.SYNC_DATA1)));
            ce.setLastModificationTime(cal.getTime());

            String organizer = "";
            String adressOrganizer = c.getString(c.getColumnIndex(CalendarContract.Events.ORGANIZER));
            ce.setOrganizer(new People(organizer, adressOrganizer));

            ce.setDirty(c.getLong(c.getColumnIndex(CalendarContract.Events.DIRTY)) != 0);
            ce.setDeleted(c.getLong(c.getColumnIndex(CalendarContract.Events.DELETED)) != 0);

            result.put(ce.getKey(), ce);
        }
        c.close();
        return result;
    }

    /**
     * Update an entry in local calendar
     * @param local the local entry that needs to be udpated
     * @param remote the remote entry with new data to be saved
     * @return true if at least one line is updated
     */
    @Override
    public boolean updateEntry(CalendarEntry local, CalendarEntry remote) {
        if(local == null || remote == null)
            return false;

        return CalendarHelper.updateEntry(mContext, mAccount, local, remote) >= 0;
    }
}
