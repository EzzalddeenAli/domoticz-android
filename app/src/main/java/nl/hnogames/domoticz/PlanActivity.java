/*
 * Copyright (C) 2015 Domoticz - Mark Heinis
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package nl.hnogames.domoticz;

import android.os.Bundle;
import android.view.MenuItem;

import com.ftinc.scoop.Scoop;

import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import hugo.weaving.DebugLog;
import nl.hnogames.domoticz.app.AppCompatAssistActivity;
import nl.hnogames.domoticz.fragments.Dashboard;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticzapi.Containers.ConfigInfo;
import nl.hnogames.domoticzapi.Utils.ServerUtil;

public class PlanActivity extends AppCompatAssistActivity {
    private ServerUtil mServerUtil;
    private Timer autoRefreshTimer = null;
    private Dashboard dash;
    private SharedPrefUtil mSharedPrefs;
    private Toolbar toolbar;

    @DebugLog
    public ConfigInfo getConfig() {
        return mServerUtil != null && mServerUtil.getActiveServer() != null ?
                mServerUtil.getActiveServer().getConfigInfo(this) :
                null;
    }

    private void setupAutoRefresh() {
        if (mSharedPrefs.getAutoRefresh() && autoRefreshTimer == null) {
            autoRefreshTimer = new Timer("autorefresh", true);
            autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                @DebugLog
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        @DebugLog
                        public void run() {
                            dash.refreshFragment();
                        }
                    });
                }
            }, 0, (mSharedPrefs.getAutoRefreshTimer() * 1000));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPrefs = new SharedPrefUtil(this);
        // Apply Scoop to the activity
        Scoop.getInstance().apply(this);

        if (!UsefulBits.isEmpty(mSharedPrefs.getDisplayLanguage()))
            UsefulBits.setDisplayLanguage(this, mSharedPrefs.getDisplayLanguage());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_graph);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            //noinspection SpellCheckingInspection
            String selectedPlan = bundle.getString("PLANNAME");
            //noinspection SpellCheckingInspection
            int selectedPlanID = bundle.getInt("PLANID");
            this.setTitle(selectedPlan);

            dash = new Dashboard();
            dash.selectedPlan(selectedPlanID, selectedPlan);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.replace(R.id.main, dash);
            tx.commit();
            setupAutoRefresh();
        } else this.finish();
    }

    private void stopAutoRefreshTimer() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.cancel();
            autoRefreshTimer.purge();
            autoRefreshTimer = null;
        }
    }

    public ServerUtil getServerUtil() {
        if (mServerUtil == null)
            mServerUtil = new ServerUtil(this);
        return mServerUtil;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @DebugLog
    public void onDestroy() {
        stopAutoRefreshTimer();
        super.onDestroy();
    }

    @Override
    @DebugLog
    public void onPause() {
        stopAutoRefreshTimer();
        super.onPause();
    }

    @Override
    @DebugLog
    public void onBackPressed() {
        stopAutoRefreshTimer();
        this.finish();
    }

    @Override
    @DebugLog
    public void onResume() {
        super.onResume();
        setupAutoRefresh();
    }
}