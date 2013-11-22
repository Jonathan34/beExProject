package com.bedroid.beEx.adapter;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.bedroid.beEx.entity.CalendarEntry;
import com.bedroid.beEx.helper.ExchangeHelper;

import java.net.URISyntaxException;
import java.util.HashMap;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.BasePropertySet;
import microsoft.exchange.webservices.data.BodyType;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.ServiceLocalException;

/**
 * Created by JDelfour on 11/22/13.
 */
public class ExchangeAdapter extends GenericAdapter {

    private static final String TAG = "ExchangeAdapter";

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
        ExchangeService service = null;
        try {
            service = eh.connectToExchange(mContext);

            FindItemsResults<Appointment> appointments = eh.getCalendarItems();
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

                Appointment a = Appointment.bind(service, appointment.getId(), psPropSet);
                CalendarEntry ce = new CalendarEntry();
                ce.loadFromAppointment(a);

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
}
