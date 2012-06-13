package com.smike.headphonesms;

import android.content.Context;
import android.media.AudioManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VolumeDialogPreference extends DialogPreference {

  private static final String LOG_TAG =
      HeadphoneSmsApp.LOG_TAG + "." + VolumeDialogPreference.class.getSimpleName();

  private int volumeValue = -1;

  public VolumeDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }


  @Override
  protected void onBindDialogView(View view) {
    String volumeKey = getContext().getString(R.string.prefsKey_volume);
    volumeValue = getSharedPreferences().getInt(volumeKey, -1);

    // -1 means use the default volume.
    boolean use_default = volumeValue == -1;

    CheckBox checkBox = (CheckBox)view.findViewById(R.id.use_default_volume);
    checkBox.setChecked(use_default);

    final SeekBar seekBar = (SeekBar)view.findViewById(R.id.volume);
    AudioManager audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
    seekBar.setMax(audioManager.getStreamMaxVolume(ReadSmsService.READING_AUDIO_STREAM));
    seekBar.setEnabled(!use_default);

    if (!use_default) {
      seekBar.setProgress(volumeValue);
    }

    checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        seekBar.setEnabled(!isChecked);
        VolumeDialogPreference.this.volumeValue = -1;
      }
    });

    seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        VolumeDialogPreference.this.volumeValue = progress;
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    super.onBindDialogView(view);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);

    if (positiveResult) {
      persistInt(volumeValue);
    }
  }
}
