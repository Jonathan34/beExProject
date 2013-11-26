package com.bedroid.beEx.adapter;

import android.accounts.Account;
import android.content.Context;

import com.bedroid.beEx.entity.CalendarEntry;

import java.util.HashMap;

public abstract interface IAdapter {

    public IAdapter createInstance(Context c, Account a);

    public abstract HashMap<String, CalendarEntry> getAppointments();

    public abstract boolean updateEntry(CalendarEntry local, CalendarEntry remote);
}
