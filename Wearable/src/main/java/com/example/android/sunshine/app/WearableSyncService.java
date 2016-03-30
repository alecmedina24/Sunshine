package com.example.android.sunshine.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

/**
 * Created by alecmedina on 3/29/16.
 */
public class WearableSyncService extends Service {

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private final IBinder binder = new SyncBinder();
    private Context context;
    private Long lastSync;
    private WearableDataHelper wearableDataHelper;

    public class SyncBinder extends Binder {
        WearableSyncService getService() {
            return WearableSyncService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastSyncKey = context.getString(R.string.pref_last_wear_sync);
        lastSync = prefs.getLong(lastSyncKey, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (lastSync - System.currentTimeMillis() >= 0) {
            wearableDataHelper = new WearableDataHelper(getApplicationContext(),
                    (WearableDataHelper.DataChangedCallback) this);
            wearableDataHelper.connect();
            wearableDataHelper.createSyncMessage();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
