/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.android.glass.sample.compass;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import com.google.android.glass.sample.compass.model.Landmarks;
import com.google.android.glass.sample.compass.util.MathUtils;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class CompassActivity extends Activity {
    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class CompassBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readHeadingAloud() {
            float heading = mOrientationManager.getHeading();

            Resources res = getResources();
            String[] spokenDirections = res.getStringArray(R.array.spoken_directions);
            String directionName = spokenDirections[MathUtils.getHalfWindIndex(heading)];

            int roundedHeading = Math.round(heading);
            int headingFormat;
            if (roundedHeading == 1) {
                headingFormat = R.string.spoken_heading_format_one;
            } else {
                headingFormat = R.string.spoken_heading_format;
            }

            String headingText = res.getString(headingFormat, roundedHeading, directionName);
            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private final CompassBinder mBinder = new CompassBinder();

    private OrientationManager mOrientationManager;
    private Landmarks mLandmarks;
    private TextToSpeech mSpeech;

    private CompassRenderer mRenderer;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });

        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mOrientationManager = new OrientationManager(sensorManager, locationManager);
        mLandmarks = new Landmarks(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        mRenderer = new CompassRenderer(this, mOrientationManager, mLandmarks);

        SurfaceView sV = new SurfaceView(this);

        sV.getHolder().addCallback(mRenderer);

        setContentView(sV);

    }

    @Override
    public void onDestroy() {
        mSpeech.shutdown();

        mSpeech = null;
        mOrientationManager = null;
        mLandmarks = null;

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compass, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.read_aloud:
                mBinder.readHeadingAloud();
                return true;
            case R.id.stop:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
