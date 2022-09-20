package com.kabouzeid.trebl.ui.activities;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.util.NavigationUtil;
import com.kabouzeid.trebl.util.PreferenceUtil;

import me.tankery.lib.circularseekbar.CircularSeekBar;


public class EqualizerActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, AdapterView.OnItemSelectedListener {

    final int MAX_SLIDERS = 5; // Must match the XML layout
    ImageView systemEq;
    CircularSeekBar bassBar = null;
    CircularSeekBar surroundBar = null;
    SwitchCompat enabled = null;
    Button flat = null;
    int min_level = 0;
    int max_level = 100;
    SeekBar[] sliders = new SeekBar[MAX_SLIDERS];
    int num_sliders = 0;

    int currentEqProfile = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        Window window = this.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        currentEqProfile = PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("currentEqProfile", 0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_baseline_arrow_back_24);
            assert upArrow != null;
            upArrow.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        enabled = findViewById(R.id.enabled);
        enabled.setHighlightColor(Color.WHITE);

        flat = findViewById(R.id.flat);
        flat.setOnClickListener(this);

        systemEq = findViewById(R.id.system_eq);
        systemEq.setOnClickListener(v -> NavigationUtil.openEqualizer(EqualizerActivity.this));

        bassBar = findViewById(R.id.seekbar_bass);
        surroundBar = findViewById(R.id.seekbar_virtualizer);

        sliders[0] = findViewById(R.id.slider_1);
        sliders[1] = findViewById(R.id.slider_2);
        sliders[2] = findViewById(R.id.slider_3);
        sliders[3] = findViewById(R.id.slider_4);
        sliders[4] = findViewById(R.id.slider_5);

        try {
            Equalizer eq = new Equalizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id",0));
            num_sliders = eq.getNumberOfBands();
            short[] r = eq.getBandLevelRange();
            min_level = r[0];
            max_level = r[1];
            for (int i = 0; i < num_sliders && i < MAX_SLIDERS; i++) {
                sliders[i].setOnSeekBarChangeListener(this);
                //slider_labels[i].setText(formatBandLabel(freq_range));
            }
            bassBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                @Override
                public void onProgressChanged(@Nullable CircularSeekBar circularSeekBar, float v, boolean b) {
                    try {
                        BassBoost bassBoost = new BassBoost(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
                        if (enabled.isChecked()) {
                        bassBoost.setEnabled(v > 0);
                        short m = (short) (v*10);
                        bassBoost.setStrength(m);
                        bassBoost.release();
                        }
                        PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("bassLevel" + currentEqProfile, (int)v);
                    }catch (Exception ignored){

                    }

                }

                @Override
                public void onStopTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {

                }

                @Override
                public void onStartTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {

                }
            });
            surroundBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                @Override
                public void onProgressChanged(@Nullable CircularSeekBar circularSeekBar, float v, boolean b) {
                    try {
                        Virtualizer virtualizer = new Virtualizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
                        if (enabled.isChecked()) {
                        virtualizer.setEnabled(v > 0);
                        short m = (short) (v*10);
                        virtualizer.setStrength(m);
                        virtualizer.release();
                        }
                        PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("vzLevel" + currentEqProfile, (int)v);
                    }catch (Exception ignored){

                    }

                }

                @Override
                public void onStopTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {

                }

                @Override
                public void onStartTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {

                }
            });
            eq.release();
        } catch (Exception ignored) {
        }

        String[] strings = {"Profile 1", "Profile 2", "Profile 3", "Profile 4", "Profile 5"};
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>
                (this, R.layout.item_spinner,
                        strings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(currentEqProfile);
        spinner.setOnItemSelectedListener(this);

        if(!PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsBoolean("turnEqualizer", false)){
            for (int i = 0; i < num_sliders && i < MAX_SLIDERS; i++) {
                sliders[i].setEnabled(false);
                sliders[i].getProgressDrawable().setAlpha(150);
            }
            bassBar.setEnabled(false);
            bassBar.setAlpha(0.5f);
            surroundBar.setEnabled(false);
            surroundBar.setAlpha(0.5f);
            spinner.setEnabled(false);
            spinner.setAlpha(0.5f);
            flat.setEnabled(false);
            flat.setAlpha(0.5f);
        }else{
            for (int i = 0; i < num_sliders && i < MAX_SLIDERS; i++) {
                sliders[i].setEnabled(true);
                sliders[i].getProgressDrawable().setAlpha(255);
            }
            bassBar.setEnabled(true);
            bassBar.setAlpha(1.0f);
            surroundBar.setEnabled(true);
            surroundBar.setAlpha(1.0f);
            spinner.setEnabled(true);
            spinner.setAlpha(1.0f);
            flat.setEnabled(true);
            flat.setAlpha(1.0f);
        }

        enabled.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                // saving setting for equalizer
                PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("turnEqualizer", b);

                if(!b){
                    for (int i = 0; i < num_sliders && i < MAX_SLIDERS; i++) {
                        sliders[i].setEnabled(false);
                        sliders[i].getProgressDrawable().setAlpha(150);
                    }
                    bassBar.setEnabled(false);
                    bassBar.setAlpha(0.5f);
                    surroundBar.setEnabled(false);
                    surroundBar.setAlpha(0.5f);
                    spinner.setEnabled(false);
                    spinner.setAlpha(0.5f);
                    flat.setEnabled(false);
                    flat.setAlpha(0.5f);
                }else{
                    for (int i = 0; i < num_sliders && i < MAX_SLIDERS; i++) {
                        sliders[i].setEnabled(true);
                        sliders[i].getProgressDrawable().setAlpha(255);
                    }
                    bassBar.setEnabled(true);
                    bassBar.setAlpha(1.0f);
                    surroundBar.setEnabled(true);
                    surroundBar.setAlpha(1.0f);
                    spinner.setEnabled(true);
                    spinner.setAlpha(1.0f);
                    flat.setEnabled(true);
                    flat.setAlpha(1.0f);
                }

                // Crash can be expected in case no audio session is active. In that case we only
                // need to turn equalizer on in settings
                try {
                    Equalizer eq = new Equalizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
                    BassBoost bassBoost = new BassBoost(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
                    Virtualizer virtualizer = new Virtualizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
                    eq.setEnabled(b);
                    bassBoost.setEnabled(b);
                    virtualizer.setEnabled(b);
                    if (b) {
                        for (int i = 0; i < 5; i++) {
                            eq.setBandLevel((short) i, (short) PreferenceUtil.getInstance(EqualizerActivity.this)
                                    .readSharedPrefsInt("profile" + currentEqProfile + "Band" + i, 0));
                        }
                        short basslev = (short)PreferenceUtil.getInstance(EqualizerActivity.this).
                                readSharedPrefsInt("bassLevel" + currentEqProfile, 0);

                        short surroundlev = (short) PreferenceUtil.getInstance(EqualizerActivity.this).
                                readSharedPrefsInt("vzLevel" + currentEqProfile, 0);

                        bassBoost.setStrength((short) (basslev*10)); //circular seekbar is not in correct range (1-1000) it is in 1-100
                        virtualizer.setStrength((short) (surroundlev*10));
                    }
                    eq.release();
                    bassBoost.release();
                    virtualizer.release();
                } catch (Exception ignored) {
                }
            }
        });
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int level,
                                  boolean fromTouch) {
        try {
            Equalizer eq = new Equalizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
                int new_level = min_level + (max_level - min_level) * level / 100;
                for (int i = 0; i < num_sliders; i++) {
                    if (sliders[i] == seekBar) {
                        if (eq.getEnabled()) {
                            eq.setBandLevel((short) i, (short) new_level);
                        }
                        PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("profile" + currentEqProfile + "Band" + i, new_level);
                        break;
                    }
                }
            eq.release();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void updateSliders() {
        try {
            for (int i = 0; i < num_sliders; i++) {
                int level = (short) PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("profile" + currentEqProfile + "Band" + i, 0);
                int pos = 100 * level / (max_level - min_level) + 50;
                sliders[i].setProgress(pos);
            }
        } catch (Exception ignored) {
        }
    }

    public void updateBassBoost() {
        try {
            bassBar.setProgress((short)PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("bassLevel" + currentEqProfile, 0));
        } catch (Exception ignored) {
        }
    }

    public void updateVirtualizer() {
        try {
            surroundBar.setProgress((short) PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("vzLevel" + currentEqProfile, 0));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClick(View view) {
        if (view == flat) {
            setFlat();
        }
    }

    public void updateUI() {
        updateSliders();
        updateBassBoost();
        updateVirtualizer();
        enabled.setChecked(PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsBoolean("turnEqualizer", false));
    }

    public void setFlat() {
        try {
            Equalizer eq = new Equalizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
            BassBoost bassBoost = new BassBoost(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
            Virtualizer virtualizer = new Virtualizer(0, PreferenceUtil.getInstance(EqualizerActivity.this).readSharedPrefsInt("audio_session_id", 0));
            bassBoost.setEnabled(false);
            bassBoost.setStrength((short) 0);
            virtualizer.setEnabled(false);
            virtualizer.setStrength((short) 0);
            for (int i = 0; i < num_sliders; i++) {
                eq.setBandLevel((short) i, (short) 0);
                PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("profile" + currentEqProfile + "Band" + i, 0);
            }
            PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("bassLevel" + currentEqProfile, 0);
            PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("vzLevel" + currentEqProfile, 0);
            updateUI();
            eq.release();
            bassBoost.release();
            virtualizer.release();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        /*
         * Spinner Profile Selection
         */
        if (currentEqProfile != position) {
            PreferenceUtil.getInstance(EqualizerActivity.this).writeSharedPrefs("currentEqProfile", position);
            currentEqProfile = position;
        }
        Log.d("Equalizer", currentEqProfile + "profile");
        updateUI();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

