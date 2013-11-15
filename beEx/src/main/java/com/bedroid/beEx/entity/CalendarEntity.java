package com.bedroid.beEx.entity;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.Time;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.ServiceLocalException;

/**
 * Created by Jon on 11/14/13.
 */
public class CalendarEntity {

    private long calendar_id;
    private Appointment appointment;
    private TimeZone timeZone;

    public CalendarEntity(Appointment appointment, long calendar_id) {
        this.appointment = appointment;
        this.calendar_id = calendar_id;
        this.timeZone = TimeZone.getDefault();
    }

    public String getId() throws ServiceLocalException {
        return appointment.getId().getUniqueId();
    }

    public void setId(long id)
    {
        //appointment.setCa = id;
    }

    public String getUid() throws ServiceLocalException {
        return appointment.getICalUid();
    }

    public void setUid(String uid) throws Exception {
        appointment.setICalUid(uid);
    }

    public long getCalendar_id()
    {
        return calendar_id;
    }

    public void setCalendar_id(int calendarId)
    {
        calendar_id = calendarId;
    }

    public String getTitle() throws ServiceLocalException {
        return appointment.getSubject();
    }

    public void setTitle(String title) throws Exception {
        appointment.setSubject(title);
    }

    public boolean getAllDay() throws ServiceLocalException {
        return appointment.getIsAllDayEvent();
    }

    private Date allDayDate(Date in) throws ServiceLocalException {
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

    public void setAllDay(boolean allDay) throws Exception {
        appointment.setIsAllDayEvent(allDay);
        if(allDay) {
            appointment.setStart(allDayDate(appointment.getStart()));
            appointment.setEnd(allDayDate(appointment.getEnd()));
        }
    }

    public String getWhen() throws ServiceLocalException {
        return appointment.getWhen();
    }

    public Date getStart() throws ServiceLocalException {
        return allDayDate(appointment.getStart());
    }

    /*public void setDtstart(Time dtstart)
    {
        this.dtstart = dtstart;
    }*/

    public Date getEnd() throws ServiceLocalException {
        return allDayDate(appointment.getEnd());
    }

    /*public void setDtend(Time dtend)
    {
        this.dtend = dtend;
    }*/

    public String getDescription() throws ServiceLocalException {
        return appointment.getBody().toString();
    }

    /*public void setDescription(String description)
    {
        this.description = description;
    }*/

    public String getEventLocation() throws ServiceLocalException {
        return appointment.getLocation();
    }

    /*public void setEventLocation(String eventLocation)
    {
        this.eventLocation = eventLocation;
    }*/

    /*public int getVisibility()
    {
        return appointment.getVis;
    }

    public void setVisibility(int visibility)
    {
        this.visibility = visibility;
    }*/

    /*public int getHasAlarm()
    {
        return hasAlarm;
    }

    public void setHasAlarm(int hasAlarm)
    {
        this.hasAlarm = hasAlarm;
    }*/

    /*public String getrRule()
    {
        return rRule;
    }

    public void setrRule(String rRule)
    {
        this.rRule = rRule;
    }*/

    /*public String getexDate()
    {
        return exDate;
    }

    public void setexDate(String exDate)
    {
        this.exDate = exDate;
    }

    public int getReminderTime()
    {
        return reminderTime;
    }

    public void setReminderTime(int reminderTime)
    {
        this.reminderTime = reminderTime;
    }*/

    @Override
    public String toString()
    {
        try {
            return getTitle() + ": " + getStart().toString();
        } catch (ServiceLocalException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLocalHash() throws ServiceLocalException {
        ArrayList<String> contents = new ArrayList<String>(4);
        contents.add(getTitle() == null ? "no title" : getTitle());
        contents.add(getStart() == null ? "no start" : "start " + getStart().getTime());
        contents.add(getEnd() == null ? "no end" : "end " + getEnd().getTime());
        contents.add(getAllDay() ? "AllDay" : "Not AllDay");
        contents.add(getDescription() == null ? "no Description" : getDescription());
        contents.add(getEventLocation() == null ? "no EventLocation" : getEventLocation());
        //contents.add(getrRule() == null ? "no rRule" : getrRule());
        //contents.add(getReminderTime() == -1 ? "no Reminder" : Integer.toString(getReminderTime()));
        return CalendarEntity.join("|", contents.toArray());
    }

    public static String join(final String delimiter, final Object[] objects)
    {
        if (objects.length == 0) return "";

        StringBuilder buffer = new StringBuilder(objects[0].toString());

        for (int i = 1; i < objects.length; i++)
            buffer.append(delimiter).append(objects[i]);

        return buffer.toString();
    }

    public static ContentValues getContentValuesFromAppointment(long cal_id, Appointment appointment) throws ServiceLocalException {
        CalendarEntity ce = new CalendarEntity(appointment, cal_id);

        ContentValues values = new ContentValues();

        String when = ce.getWhen();
        Date start = ce.getStart();
        Date end = ce.getEnd();

        values.put(CalendarContract.Events.DTSTART, ce.getAllDay() ? end.getTime() :start.getTime());
        values.put(CalendarContract.Events.DTEND, end.getTime());
        values.put(CalendarContract.Events.EVENT_TIMEZONE, ce.getTimeZone().getID());
        values.put(CalendarContract.Events.TITLE, ce.getTitle());
        values.put(CalendarContract.Events.DESCRIPTION, "descr");
        values.put(CalendarContract.Events.ORGANIZER, appointment.getOrganizer().getAddress());
        values.put(CalendarContract.Events.SELF_ATTENDEE_STATUS, appointment.getMyResponseType().ordinal());
        values.put(CalendarContract.Events.DESCRIPTION, "descr");

        values.put(CalendarContract.Events.CALENDAR_ID, cal_id);
        values.put(CalendarContract.Events.ALL_DAY, ce.getAllDay() ? 1 : 0);

        return values;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /*public static CalendarEntity createFromAppointment(Appointment appointment) {
        CalendarEntity ce = new CalendarEntity();
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
