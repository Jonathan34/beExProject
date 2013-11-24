package com.bedroid.beEx.adapter;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;

import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.entity.People;
import com.bedroid.beEx.helper.CalendarHelper;
import com.bedroid.beEx.helper.ExchangeHelper;

import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.Attendee;
import microsoft.exchange.webservices.data.AttendeeCollection;
import microsoft.exchange.webservices.data.BasePropertySet;
import microsoft.exchange.webservices.data.BodyType;
import microsoft.exchange.webservices.data.CalendarFolder;
import microsoft.exchange.webservices.data.CalendarView;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.MessageBody;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.ServiceLocalException;
import microsoft.exchange.webservices.data.WellKnownFolderName;

public class ExchangeAdapter extends GenericAdapter {

    private static final String TAG = "ExchangeAdapter";
    private ExchangeService mService = null;
    private boolean TEST_MODE = false;

    public ExchangeAdapter(Context c, Account a) {
        super(c, a);
    }

    @Override
    public IAdapter createInstance(Context c, Account a) {
        return new ExchangeAdapter(c, a);
    }

    @Override
    public HashMap<String, CalendarEntry> getAppointments() {
        HashMap<String, CalendarEntry> result = new HashMap<String, CalendarEntry>();

        ExchangeHelper eh = ExchangeHelper.getInstance();
        try {
            mService = eh.connectToExchange(mContext);

            FindItemsResults<Appointment> appointments = getCalendarItems();
            for (Appointment appointment : appointments.getItems())
            {
                if(appointment == null) {
                    Log.e(TAG, "Got a null appointment!");
                    continue;
                }

                //CalendarEntry.createFromAppointment(appointment);
                //Test if appointment already exists
                        /*String when = appointment.getWhen();
                        Date start = appointment.getStart();
                        Date end = appointment.getEnd();*/
                PropertySet psPropSet = new PropertySet(BasePropertySet.FirstClassProperties);
                psPropSet.setRequestedBodyType(BodyType.Text);

                Appointment a = Appointment.bind(mService, appointment.getId(), psPropSet);
                CalendarEntry ce = loadFromAppointment(a);
                if(ce != null)
                    result.put(ce.getKey(), ce);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ServiceLocalException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public FindItemsResults<Appointment> getCalendarItems() throws Exception {
        if(mService == null) {
            Log.e(TAG, "getCalendarItems: null service");
            return null;
        }

        //load preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int secsBefore = Integer.parseInt(sharedPrefs.getString("sync_range_before", "604800"));
        int secsAfter = Integer.parseInt(sharedPrefs.getString("sync_range_after", "604800"));

        //CALENDAR
        Calendar cal = Calendar.getInstance();
        if(TEST_MODE == false)
            cal.add(Calendar.SECOND, -secsBefore);
        Date startTime = cal.getTime();

        cal = Calendar.getInstance();
        if(TEST_MODE == false)
            cal.add(Calendar.SECOND, secsAfter);
        else
            cal.set(2013,Calendar.NOVEMBER,26,0,0,0);
        Date endTime = cal.getTime();

        CalendarView view = new CalendarView(startTime, endTime);

        CalendarFolder folder = CalendarFolder.bind(mService, WellKnownFolderName.Calendar);
        /*FindItemsResults<Appointment> results = folder.findAppointments(view);

        for (Appointment appointment : results.getItems())
        {
            System.out.println("appointment======" + appointment.getSubject()) ;
            //find appointments will only give basic properties.
            //in order to get more properties (such as BODY), we need to call call EWS again
            //Appointment appointmentDetailed = Appointment.bind(service, appointment.getId(), );
        }*/
        return folder.findAppointments(view);
    }

    public CalendarEntry loadFromAppointment(Appointment a) throws Exception {
        if (a == null)
            return null;

        CalendarEntry ce = new CalendarEntry();

        String tz = a.getTimeZone();
        ce.setTimeZone(TimeZone.getTimeZone(tz));

        ce.setTitle(a.getSubject());
        //String when = appointment.getWhen();
        ce.setStart(a.getStart());
        ce.setEnd(a.getEnd());
        ce.setDescription(MessageBody.getStringFromMessageBody(a.getBody()));
        ce.setOrganizer(new People(a.getOrganizer().getName(), a.getOrganizer().getAddress()));
        //this.setCalendarId(cal_id);
        ce.setAllDay(a.getIsAllDayEvent());
        ce.setUid(a.getId().getUniqueId());
        ce.setLocation(a.getLocation());
        /*TimeSpan dur = a.getDuration();
        this.setDuration(dur);//SCRUBBING*/

        ce.setLastModificationTime(a.getLastModifiedTime());

        AttendeeCollection req = a.getRequiredAttendees();
        for (Attendee at : req){
            People p = new People(at.getName(), at.getAddress());
            p.setResponseStatus(CalendarHelper.getStatusFormAppointment(at.getResponseType()));
            ce.addRequiredPeople(p);
        }

        AttendeeCollection opt = a.getOptionalAttendees();
        for (Attendee at : opt){
            People p = new People(at.getName(), at.getAddress());
            p.setResponseStatus(CalendarHelper.getStatusFormAppointment(at.getResponseType()));
            ce.addOptionalPeople(p);
        }

        AttendeeCollection resources = a.getRequiredAttendees();
        for (Attendee at : resources){
            People p = new People(at.getName(), at.getAddress());
            p.setResponseStatus(CalendarHelper.getStatusFormAppointment(at.getResponseType()));
            ce.addResource(p);
        }
        /*AttendeeCollection req = this.getRequiredAttendees();
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

        return ce;
    }
}
