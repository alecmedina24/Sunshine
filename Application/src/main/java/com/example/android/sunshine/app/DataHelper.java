package com.example.android.sunshine.app;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by alecmedina on 3/14/16.
 */
public class DataHelper {

    private static final String TAG = DataHelper.class.getSimpleName();
    private static final String COUNT_PATH = "/count";
    private static final String COUNT_KEY = "count";

    private GoogleApiClient mGoogleApiClient;
    private Context context;
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    public DataHelper(Context context) {
        this.context = context;
        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
    }

    public void createGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) context)
                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) context)
                .build();
    }

    public void connectGoogleApiClient() {
        mGoogleApiClient.connect();
    }

    public void disconnectGoogleApiClient() {
        mGoogleApiClient.disconnect();
    }

    public void createDataItem() {
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);
    }

    public void addDataItemListener() {
        Wearable.DataApi.addListener(mGoogleApiClient, (DataApi.DataListener) context);
    }

    public void removeDataItemListener() {
        Wearable.DataApi.removeListener(mGoogleApiClient, (DataApi.DataListener) context);
    }

    private class DataItemGenerator implements Runnable {

        private int count;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt("number", count++);
            putDataMapRequest.getDataMap().putString(COUNT_KEY, "weather");
            putDataMapRequest.getDataMap().putString("date", "today");
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

}
