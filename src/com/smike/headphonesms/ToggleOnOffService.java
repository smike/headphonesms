package com.smike.headphonesms;

import android.app.Service;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class ToggleOnOffService extends Service {
  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String key = getString(R.string.prefsKey_enabled);

    Editor editor = sharedPreferences.edit();
    editor.putBoolean(key, !SettingsUtil.isEnabled(getApplicationContext()));
    editor.commit();

    OnOffAppWidgetProvider.update(getApplicationContext());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
      BackupManager.dataChanged(getPackageName());
    }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return null;
  }
}