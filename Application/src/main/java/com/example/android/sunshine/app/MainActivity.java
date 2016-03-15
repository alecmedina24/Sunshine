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
import android.os.Bundle;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends Activity {

    private AppDataHelper dataHelper;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        dataHelper = new AppDataHelper(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        dataHelper.connect();
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
        dataHelper.disconnect();
        super.onStop();
    }
}
