package com.smike.headphonesms;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle state){
    super.onCreate(state);
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  protected void onResume() {
    super.onResume();

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    updateView(sharedPreferences);

    sharedPreferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        OnOffAppWidgetProvider.update(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
          BackupManager.dataChanged(getPackageName());
        }

        onContentChanged();
        updateView(sharedPreferences);
      }
    });
  }

  // TODO(smike): Find a cleaner way to update preferences that might have been changed in
  // elsewhere. There must be a way to do all prefs automatically.
  private void updateView(SharedPreferences sharedPreferences) {
    // enabled is the only setting that could have changed (by the widget).
    boolean enabled = SettingsUtil.isEnabled(this);

    String key = getString(R.string.prefsKey_enabled);
    CheckBoxPreference enabledPreference = (CheckBoxPreference)this.findPreference(key);
    enabledPreference.setChecked(enabled);

    key = getString(R.string.prefsKey_activationMode);
    this.findPreference(key).setEnabled(enabled);

    key = getString(R.string.prefsKey_volume);
    this.findPreference(key).setEnabled(enabled);

    key = getString(R.string.prefsKey_preferSco);
    this.findPreference(key).setEnabled(enabled);
  }
}
