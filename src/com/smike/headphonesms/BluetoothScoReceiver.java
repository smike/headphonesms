package com.smike.headphonesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

public class BluetoothScoReceiver extends BroadcastReceiver {
  private static final String LOG_TAG = BluetoothScoReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Bundle bundle = intent.getExtras();
    if (bundle == null) {
      Log.w(LOG_TAG, "Bundle is null. Doing nothing.");
      return;
    }

    if (intent.getAction().equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED) ||
        intent.getAction().equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
      receivedScoStateUpdate(bundle, context);
    } else {
      Log.w(LOG_TAG, "Received unrecognized action broadcast: " + intent.getAction());
    }
  }

  private void receivedScoStateUpdate(Bundle bundle, Context context) {
    int previousState = bundle.getInt(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE);
    int state = bundle.getInt(AudioManager.EXTRA_SCO_AUDIO_STATE);
    if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED ||
        state == AudioManager.SCO_AUDIO_STATE_ERROR) {
      AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      audioManager.stopBluetoothSco();

      // No SCO, but fall back to other methods if available.
      if (HeadphoneSmsApp.shouldRead(false, context)) {
        ReadSmsService.startReading(context);
      } else {
        ReadSmsService.stopReading(context);
      }

    } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
      ReadSmsService.startReading(context);
    }
    Log.i(LOG_TAG, "sco state = " + previousState + " " + state);
  }
}
