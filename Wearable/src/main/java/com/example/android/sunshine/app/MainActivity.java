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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MainActivity extends CanvasWatchFaceService {

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MainActivity.Engine> mWeakReference;

        public EngineHandler(MainActivity.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    public class Engine extends CanvasWatchFaceService.Engine implements WearableDataHelper.DataChangedCallback {

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        float mXOffset;
        float mYOffset;

        private WearableDataHelper wearableDataHelper;
        private TextView mDateView;
        private TextView mHighView;
        private TextView mLowView;
        private TextView mTimeView;
        private ImageView mWeatherImageHolder;
        private LinearLayout mBackground;
        private View mLayout;
        private int mHeight;
        private int mWidth;
        private final Point displaySize = new Point();

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

//            wearableDataHelper = new WearableDataHelper(getApplicationContext(), this);
//            wearableDataHelper.connect();

            setWatchFaceStyle(new WatchFaceStyle.Builder(MainActivity.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = MainActivity.this.getResources();

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = inflater.inflate(R.layout.watch_face_circular, null);

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(displaySize);

            mWidth = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                    View.MeasureSpec.EXACTLY);
            mHeight = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                    View.MeasureSpec.EXACTLY);

            mTimeView = (TextView) mLayout.findViewById(R.id.time);
            mDateView = (TextView) mLayout.findViewById(R.id.date);
            mHighView = (TextView) mLayout.findViewById(R.id.high_temp);
            mLowView = (TextView) mLayout.findViewById(R.id.low_temp);
            mWeatherImageHolder = (ImageView) mLayout.findViewById(R.id.weather_image);
            mBackground = (LinearLayout) mLayout.findViewById(R.id.background);

//            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();

//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//            String lastSyncKey = resources.getString(R.string.pref_last_wear_sync);
//            long lastSync = prefs.getLong(lastSyncKey, 0);
//            Log.v("Wearable", "last sync = " + lastSync);

//            wearableDataHelper.createSyncMessage();
            Intent intent = new Intent(getApplicationContext(), WearableSyncService.class);
            intent.setAction(Intent.ACTION_SYNC);
            startService(intent);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            wearableDataHelper.removeDataItemListener();
            wearableDataHelper.disconnect();
//            Intent intent = new Intent(getApplicationContext(), WearableSyncService.class);
//            intent.setAction(Intent.ACTION_SYNC);
//            stopService(intent);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MainActivity.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MainActivity.this.unregisterReceiver(mTimeZoneReceiver);
//            wearableDataHelper.disconnect();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
//            Resources resources = MainActivity.this.getResources();
//            boolean isRound = insets.isRound();
//            mXOffset = resources.getDimension(isRound
//                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
//            float textSize = resources.getDimension(isRound
//                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

//            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                mBackground.setBackgroundColor(Color.BLACK);
                mHighView.setVisibility(View.INVISIBLE);
                mLowView.setVisibility(View.INVISIBLE);
                mWeatherImageHolder.setVisibility(View.INVISIBLE);
            } else {
                mBackground.setBackgroundColor(getResources().getColor(R.color.watch_face_background));
                mHighView.setVisibility(View.VISIBLE);
                mLowView.setVisibility(View.VISIBLE);
                mWeatherImageHolder.setVisibility(View.VISIBLE);
            }

            mLayout.measure(mWidth, mHeight);
            mLayout.layout(0, 0, mLayout.getMeasuredWidth(),
                    mLayout.getMeasuredHeight());

            canvas.drawColor(Color.BLACK);
            mLayout.draw(canvas);

            Long time = System.currentTimeMillis();
            String text = String.format("%tk:%tM", time, time);
            mTimeView.setText(text);
            String mDate = String.format("%tb %te, %tY", time, time, time);
            mDateView.setText(mDate);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void updateWeather(double high, double low) {
            mHighView.setText(String.format(getResources().getString(R.string.format_temperature), high));
            mLowView.setText(String.format(getResources().getString(R.string.format_temperature), low));
        }

        @Override
        public void updateImage(Bitmap weatherImage) {
            mWeatherImageHolder.setImageBitmap(weatherImage);
        }
    }
}
