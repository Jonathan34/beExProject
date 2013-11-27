package com.bedroid.beEx.observer;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

public class CalendarObserver extends ContentObserver {

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public CalendarObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        // do s.th. depending on the handler you might be on the UI thread, so be cautious!
        System.out.println("observed " + uri.toString());
    }

}
