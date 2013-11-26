package com.bedroid.beEx.entity;

import java.util.AbstractCollection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import android.content.ContentValues;
import android.provider.CalendarContract;
import android.support.v7.appcompat.R;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.AttendeeCollection;
import microsoft.exchange.webservices.data.MessageBody;
import microsoft.exchange.webservices.data.TimeSpan;
import microsoft.exchange.webservices.data.TimeZoneDefinition;

public class CalendarEntry {

    //add field to equals
    //add field to loadFrom...()
    //add field to getContentValues...()
    private String subject;

    private Date startDate;
    private Date endDate;
    private boolean allday;
    private TimeZone timeZone;
    private TimeSpan duration;

    private long calendar_id;
    private int color;

    private String location;
    private String description;
    private String uid;
    private People organizer;
    private Date lastModificationTime;

    private boolean isDirty = false;
    private boolean deleted = false;
    private TimeZone startTimeZone;
    private TimeZone endTimeZone;

    private HashSet<People> mRequiredPeople = new HashSet<People>();
    private HashSet<People> mOptionalPeople = new HashSet<People>();
    private HashSet<People> mResources = new HashSet<People>();
    private String eventId;

    public String getUid()  { return this.uid; }

    public void setUid(String uid) { this.uid = uid; }

    public long getCalendarId() { return calendar_id; }

    public void setCalendarId(long calendarId) { this.calendar_id = calendarId; }

    public String getTitle() { return subject; }

    public void setTitle(String title) { this.subject = title; }

    public boolean getAllDay(){ return allday; }

    private Date allDayDate(Date in) {
        if(in != null) {
            if(this.getAllDay()) {
                Calendar calender = Calendar.getInstance();
                calender.setTime(in);
                calender.set(Calendar.HOUR_OF_DAY, 0);
                calender.set(Calendar.MINUTE, 0);
                calender.set(Calendar.SECOND, 0);
                calender.set(Calendar.MILLISECOND, 0);
                return calender.getTime();
            }
            return in;
            //dtstart.timezone = Time.TIMEZONE_UTC;
            //date.normalize(true);
        }
        return null;
    }

    public void setAllDay(boolean allDay) {
        this.allday = allDay;
        if(this.allday) {
            this.setStart(allDayDate(this.getStart()));
            this.setEnd(allDayDate(this.getEnd()));
        }
    }

    /*public String getWhen() { return appointment.getWhen(); }*/

    public Date getStart() { return this.startDate; }

    public void setStart(Date dt) { this.startDate = dt; }

    public Date getEnd()  { return this.endDate; }

    public void setEnd(Date dt) { this.endDate = dt; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description;}

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return this.color;
    }

    /*public void setDuration(TimeSpan duration) { this.duration = duration; }

    public TimeSpan getDuration() { return this. duration; }*/

    public void setLocation(String location) { this.location = location; }

    public String getLocation() { return location; }

    @Override
    //public String toString() { return getTitle() + " by " + getOrganizer().getName() + ": " + getStart().toString(); }
    public String toString() { return getKey(); }

    @Override
    public boolean equals(Object aThat) {
        //check for self-comparison
        if ( this == aThat ) return true;

        //use instanceof instead of getClass here for two reasons
        //1. if need be, it can match any supertype, and not just one class;
        //2. it renders an explict check for "that == null" redundant, since
        //it does the check for null already - "null instanceof [type]" always
        //returns false. (See Effective Java by Joshua Bloch.)
        if ( !(aThat instanceof CalendarEntry) ) return false;
        //Alternative to the above line :
        //if ( aThat == null || aThat.getClass() != this.getClass() ) return false;

        //cast to native object is now safe
        CalendarEntry that = (CalendarEntry)aThat;

        //now a proper field-by-field evaluation can be made
        boolean res = (this.getTitle() == null && that.getTitle() == null ? true : this.getTitle().equals(that.getTitle()));
        res = res && this.getStart().equals(that.getStart());
        res = res && this.getEnd().equals(that.getEnd());
        res = res && this.getAllDay() == that.getAllDay();
                //&& this.getColor() == that.getColor()
        res = res && (this.getLocation() == null && that.getLocation() == null ? true : this.getLocation().equals(that.getLocation()));
        res = res && this.getKey().equals(that.getKey());
        res = res && (this.getDescription() == null && that.getDescription() == null ? true : this.getDescription().equals(that.getDescription()));
        res = res && this.getTimeZone().equals(that.getTimeZone());
        return res;
                //&& this.getDuration() == that.getDuration()
                //&& this.getCalendarId() == that.getCalendarId()
                //&& this.getLastModificationTime() == that.getLastModificationTime()
    }

    /*public String getLocalHash() {
        ArrayList<String> contents = new ArrayList<String>(4);
        contents.add(getTitle() == null ? "no title" : getTitle());
        contents.add(getStart() == null ? "no start" : "start " + getStart().getTime());
        contents.add(getEnd() == null ? "no end" : "end " + getEnd().getTime());
        contents.add(getAllDay() ? "AllDay" : "Not AllDay");
        contents.add(getDescription() == null ? "no Description" : getDescription());
        contents.add(getLocation() == null ? "no EventLocation" : getLocation());
        //contents.add(getrRule() == null ? "no rRule" : getrRule());
        //contents.add(getReminderTime() == -1 ? "no Reminder" : Integer.toString(getReminderTime()));
        return CalendarEntry.join("|", contents.toArray());
    }*/

    /*public static String join(final String delimiter, final Object[] objects)
    {
        if (objects.length == 0) return "";

        StringBuilder buffer = new StringBuilder(objects[0].toString());

        for (int i = 1; i < objects.length; i++)
            buffer.append(delimiter).append(objects[i]);

        return buffer.toString();
    }*/

    //to build a calendar entry
    public static ContentValues getContentValues(CalendarEntry ce) {
        ContentValues values = new ContentValues();

        //String when = ce.getWhen();
        Date start = ce.getStart();
        Date end = ce.getEnd();

        values.put(CalendarContract.Events.DTSTART, ce.getAllDay() ? end.getTime() :start.getTime());
        values.put(CalendarContract.Events.DTEND, end.getTime());
        String tz = ce.getTimeZone().getID();
        values.put(CalendarContract.Events.EVENT_TIMEZONE, ce.getTimeZone().getID());
        values.put(CalendarContract.Events.EVENT_END_TIMEZONE, ce.getTimeZone().getID());
        //values.put(CalendarContract.Events.EVENT_START_TIMEZONE, ce.getTimeZone().getID());

        //test UID
        values.put(CalendarContract.Events.UID_2445, ce.getUid());
        values.put(CalendarContract.Events.SYNC_DATA1, ce.getLastModificationTime().getTime());

        values.put(CalendarContract.Events.TITLE, ce.getTitle());
        values.put(CalendarContract.Events.DESCRIPTION, ce.getDescription());
        //values.put(CalendarContract.Events.CALENDAR_COLOR, ce.getColor());
        values.put(CalendarContract.Events.ORGANIZER, ce.getOrganizer().getEmail());
        values.put(CalendarContract.Events.EVENT_LOCATION, ce.getLocation());
        values.put(CalendarContract.Events.DIRTY, /*ce.isDirty() ? 1 : */0);
        values.put(CalendarContract.Events.DELETED, /*ce.isDeleted() ? 1 : */0);

        //TODO values.put(CalendarContract.Events.SELF_ATTENDEE_STATUS, ce.getMyResponseType().ordinal());

        values.put(CalendarContract.Events.CALENDAR_ID, ce.getCalendarId());
        values.put(CalendarContract.Events.ALL_DAY, ce.getAllDay() ? 1 : 0);


        //Compute duration
        /*Time end = new Time(ce.getEnd());
        end.parse(dtendProperty.getValue());
        long durationMillis = end.toMillis(false)
                - start.toMillis(false );
        long durationSeconds = (durationMillis / 1000);
        if (start.allDay && (durationSeconds % 86400) == 0) {
            return "P" + (durationSeconds / 86400) + "D"; // Server wants this instead of P86400S
        } else {
            return "P" + durationSeconds + "S";
        }*/

        /*SimpleDateFormat.
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ce.getDuration().getTotalMilliseconds());
        String duration = android.text.format.DateUtils.getRelativeTimeSpanString(context, ce.getDuration().getTotalMilliseconds());

        values.put(CalendarContract.Events.DURATION, duration);*/
        //


        //values.put(CalendarContract.Events.LAST_DATE, )

        return values;
    }

    public void setOrganizer(People organizer) {
        this.organizer = organizer;
    }

    public People getOrganizer() {
        return this.organizer;
    }

    public String getKey() {
        //return getOrganizer().getEmail() + " :: " +  getTitle() + " at " +  getStart().toString();
        return getUid();
    }

    public void setLastModificationTime(Date lastModificationTime) {
        this.lastModificationTime = lastModificationTime;
    }

    public Date getLastModificationTime() {
        return this.lastModificationTime;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setStartTimeZone(TimeZone startTimeZone) {
        this.startTimeZone = startTimeZone;
    }

    public TimeZone getStartTimeZone() {
        return startTimeZone;
    }

    public void setEndTimeZone(TimeZone endTimeZone) {
        this.endTimeZone = endTimeZone;
    }

    public TimeZone getEndTimeZone() {
        return endTimeZone;
    }

    public void addRequiredPeople(People p) {
        if(!mRequiredPeople.contains(p))
            mRequiredPeople.add(p);
    }

    public void addOptionalPeople(People p) {
        if(!mOptionalPeople.contains(p))
            mOptionalPeople.add(p);
    }

    public void addResource(People p) {
        if(!mResources.contains(p))
            mResources.add(p);
    }

    public HashSet<People> getRequiredPeople() {
        return mRequiredPeople;
    }

    public HashSet<People> getOptionalPeople() {
        return mOptionalPeople;
    }

    public HashSet<People> getResources() {
        return mResources;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }

    /*public static CalendarEntry createFromAppointment(Appointment appointment) {
        CalendarEntry ce = new CalendarEntry();
        TimeZone timeZone;
        timeZone = ce.setTimezone(TimeZone.getTimeZone(appointment.getTimeZone()));

        TimeZone timeZone = TimeZone.getTimeZone(appointment.getTimeZone());

        String when = appointment.getWhen();
        Date start = appointment.getStart();
        Date end = appointment.getEnd();

        values.put(CalendarContract.Events.DTSTART, start.getTime());
        values.put(CalendarContract.Events.DTEND, end.getTime());
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
        values.put(CalendarContract.Events.TITLE, appointment.getSubject());
        values.put(CalendarContract.Events.DESCRIPTION, "descr");
        values.put(CalendarContract.Events.CALENDAR_ID, cal_id);
        values.put(CalendarContract.Events.ALL_DAY, appointment.getIsAllDayEvent() ? 1 : 0);
        //values.put(CalendarContract.Events.SYNC_DATA1, appointment.getId().getUniqueId());
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
    }*/
}
