package com.smike.headphonesms;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class PrefsBackupAgent extends BackupAgentHelper {
  // A key to uniquely identify the set of backup data
  private static final String PREFS_BACKUP_KEY = "prefs";

  // Allocate a helper and add it to the backup agent
  @Override
  public void onCreate() {
    SharedPreferencesBackupHelper helper =
        new SharedPreferencesBackupHelper(this, getDefaultPrefsName());
    addHelper(PREFS_BACKUP_KEY, helper);
  }

  private String getDefaultPrefsName() {
    // TODO(smike): Is there a better way to get this?
    return getPackageName() + "_preferences";
  }
}
