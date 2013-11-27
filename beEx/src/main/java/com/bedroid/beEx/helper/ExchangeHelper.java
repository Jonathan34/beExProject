package com.bedroid.beEx.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.CalendarFolder;
import microsoft.exchange.webservices.data.CalendarView;
import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.ServiceRemoteException;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

public class ExchangeHelper {
    private static final String TAG = "ExchangeHelper";

    private static ExchangeHelper mInstance;
    private Context mContext;
    private ExchangeService mService;

    private ExchangeHelper() {
        mService = null;
        mInstance = null;
        mContext = null;
    }

    public ExchangeService getExchangeService(Context context) throws URISyntaxException {
        if(mService == null)
            mService = connectToExchange(context);
        return mService;
    }

    public ExchangeService connectToExchange(Context context) throws URISyntaxException {
        if(mService != null)
            return mService;
        mService = null;

        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType("com.bedroid.beEx.account");
        if(accounts != null && accounts.length > 0) {
            Account a = accounts[0];
            System.out.println(a.toString());

            mService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
            ExchangeCredentials credentials = new WebCredentials(a.name, am.getPassword(a));
            mService.setCredentials(credentials);
            URI url = new URI(am.getUserData(a, "SERVER_URL"));
            mService.setUrl(url);
        }
        return mService;
    }

    public ExchangeService connectToExchange(/*String serverUrl, */String email, String password) throws Exception {
        if(mService != null)
            return mService;
        mService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        mService.setTraceEnabled(true);
        ExchangeCredentials credentials = new WebCredentials(email, password);
        mService.setCredentials(credentials);
        //URI url = new URI(serverUrl);
        //service.setUrl(url);
        mService.autodiscoverUrl(email);
        return mService;
    }

    /*public FindItemsResults<Appointment> getCalendarTest() throws Exception {
        if(mService == null) {
            Log.e(TAG, "getCalendarTest: null service");
            return null;
        }
        //CALENDAR
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = dateFormat.parse("2013-11-10 00:00:00");
        Date endTime = dateFormat.parse("2013-11-15 00:00:00");

        CalendarView view = new CalendarView(startTime, endTime);

        CalendarFolder folder = CalendarFolder.bind(mService, WellKnownFolderName.Calendar);
        return folder.findAppointments(view);
    }*/

    public void initializeContext(Context context) {
        mContext = context;
    }

    public static ExchangeHelper getInstance() {
        if(mInstance == null)
            mInstance = new ExchangeHelper();
        return mInstance;
    }


    public boolean isConnectionValid(String email, String password) {
        ExchangeHelper eh = ExchangeHelper.getInstance();
        try {
            mService = eh.connectToExchange(email, password);
            return true;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (ServiceRemoteException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
