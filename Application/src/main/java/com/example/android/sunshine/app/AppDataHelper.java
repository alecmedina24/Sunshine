package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by alecmedina on 3/14/16.
 */
public class AppDataHelper extends WearableListenerService implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = AppDataHelper.class.getSimpleName();
    private static final String COUNT_PATH = "/count";
    private static final String COUNT_KEY = "count";
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private static final String SYNC = "true";
    private static final long twoWeeks = 1209600000;

    private GoogleApiClient mGoogleApiClient;
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;
    private boolean mResolvingError = false;
    private Context context;


    public AppDataHelper() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

//    public void connect() {
//        mGoogleApiClient.connect();
//    }

//    public void disconnect() {
//        mGoogleApiClient.disconnect();
//    }

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
        private final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
        };
        private static final int INDEX_WEATHER_ID = 0;
        private static final int INDEX_MAX_TEMP = 1;
        private static final int INDEX_MIN_TEMP = 2;

        @Override
        public void run() {
            String locationQuery = Utility.getPreferredLocation(context);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery,
                    System.currentTimeMillis());
            Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION,
                    null, null, null);
            cursor.moveToFirst();
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);

            Resources resources = context.getResources();
            int artResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
            String artUrl = Utility.getArtUrlForWeatherCondition(context, weatherId);

            Bitmap largeIcon;
            try {
                largeIcon = Glide.with(context)
                        .load(artUrl)
                        .asBitmap()
                        .error(artResourceId)
                        .fitCenter()
                        .into(120, 120)
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e("AppDataHelper", "Error retrieving large icon from " + artUrl, e);
                largeIcon = BitmapFactory.decodeResource(resources, artResourceId);
            }

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt("number", count++);
            putDataMapRequest.getDataMap().putDouble("high", high);
            putDataMapRequest.getDataMap().putDouble("low", low);

            Asset asset = createAssetFromBitmap(largeIcon);
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
            cursor.close();
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (messageEvent.getPath().equals(COUNT_PATH)) {
            String message = new String(messageEvent.getData());
            if (message.equals(SYNC)) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String lastSyncKey = context.getString(R.string.pref_last_sync);
                long lastSync = prefs.getLong(lastSyncKey, 0);
                if (lastSync >= twoWeeks || lastSync == 0) {
                    SunshineSyncAdapter.syncImmediately(this);
                }
                createDataItem();
            }
            Log.v("App", "The message is " + message);
        }
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult((Activity) getApplicationContext(),
                        REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
//                connect();
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
    }
}
