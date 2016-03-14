/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends Activity implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    /**
     * Request code for launching the Intent to resolve Google Play services errors.
     */
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private boolean mResolvingError = false;

    private DataHelper dataHelper;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        dataHelper = new DataHelper(this);
        dataHelper.createGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            dataHelper.connectGoogleApiClient();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        dataHelper.createDataItem();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            dataHelper.disconnectGoogleApiClient();
        }
        super.onStop();
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        dataHelper.addDataItemListener();
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                dataHelper.connectGoogleApiClient();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            dataHelper.removeDataItemListener();
        }
    }

    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
    }

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
    }
}
