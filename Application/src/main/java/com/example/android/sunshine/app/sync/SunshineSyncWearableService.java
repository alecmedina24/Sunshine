package com.example.android.sunshine.app.sync;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Created by alecmedina on 3/25/16.
 */
public class SunshineSyncWearableService extends IntentService {

    public SunshineSyncWearableService() {
        super("SunshineSyncWearableService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v("WearableService",intent.getStringExtra("extra"));
    }
}
