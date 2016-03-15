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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;

/**
 * Listens to DataItems and Messages from the local node.
 */
public class WearableDataHelper implements GoogleApiClient.ConnectionCallbacks, DataApi.DataListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DataLayerListenerServic";
    private static final String COUNT_PATH = "/count";
    private GoogleApiClient mGoogleApiClient;
    private double high;
    private double low;
    private DataChangedCallback dataChangedCallback;
//    private InputStream assetInputStream;

    public WearableDataHelper(Context context, DataChangedCallback dataChangedCallback) {
        this.dataChangedCallback = dataChangedCallback;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public interface DataChangedCallback {
        void updateWeather(double high, double low);
        void updateImage(Bitmap weatherImage);
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    public void addDataItemListener() {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    public void removeDataItemListener() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG, "onConnected(): Successfully connected to Google API client");
        addDataItemListener();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    public void loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        // convert asset into a file descriptor, upon result of Fd, decode the inputStream
        // and callback to watchFace to set image
        Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).setResultCallback(new ResultCallback<DataApi.GetFdForAssetResult>() {
            @Override
            public void onResult(DataApi.GetFdForAssetResult getFdForAssetResult) {
                InputStream assetInputStream = getFdForAssetResult.getInputStream();
                Bitmap image = BitmapFactory.decodeStream(assetInputStream);
                dataChangedCallback.updateImage(image);
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "onDataChanged: " + dataEvents);

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (COUNT_PATH.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                high = dataMapItem.getDataMap().getDouble("high");
                low = dataMapItem.getDataMap().getDouble("low");
                Asset asset = dataMapItem.getDataMap().getAsset("weatherImage");
                loadBitmapFromAsset(asset);
                dataChangedCallback.updateWeather(high, low);
                Log.v(TAG, "data = " + high);
                Log.v(TAG, "data = " + low);
            }
        }
    }
}
