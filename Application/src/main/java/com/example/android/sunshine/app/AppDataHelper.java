package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by alecmedina on 3/14/16.
 */
public class AppDataHelper implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = AppDataHelper.class.getSimpleName();
    private static final String COUNT_PATH = "/count";
    private static final String COUNT_KEY = "count";

    private GoogleApiClient mGoogleApiClient;
    private Context context;
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private boolean mResolvingError = false;


    public AppDataHelper(Context context) {
        this.context = context;
        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    public void createDataItem() {
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            Bitmap resizedBitmap = bitmap.createScaledBitmap(bitmap, 120, 120, true);
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private class DataItemGenerator implements Runnable {

        private int count;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt("number", count++);
            putDataMapRequest.getDataMap().putDouble("high", 25);
            putDataMapRequest.getDataMap().putDouble("low", 16);

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.art_clear);
            Asset asset = createAssetFromBitmap(bitmap);
            putDataMapRequest.getDataMap().putAsset("weatherImage", asset);

            putDataMapRequest.setUrgent();
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.v(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());

      }
                        }
                    });
        }
    }
    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
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
                result.startResolutionForResult((Activity) context, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
    }
}
