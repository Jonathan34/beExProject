package com.bedroid.beEx.adapter;

import android.accounts.Account;
import android.content.Context;

import com.bedroid.beEx.entity.CalendarEntry;

import java.util.HashMap;

public abstract class GenericAdapter implements IAdapter{

    protected Context mContext = null;
    protected Account mAccount = null;

    public GenericAdapter(Context c, Account a) {
        mContext = c;
        mAccount = a;
    };

    @Override
    public abstract IAdapter createInstance(Context c, Account a);

    @Override
    public abstract HashMap<String, CalendarEntry> getAppointments();
}
