package com.smike.headphonesms;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class PrefsBackupAgent extends BackupAgentHelper {
  // This is the android default. Sadly there's no robust way to get this programaticaly
  private static final String PREFS_NAME = "com.smike.headphonesms_preferences";

  // A key to uniquely identify the set of backup data
  private static final String PREFS_BACKUP_KEY = "prefs";

  // Allocate a helper and add it to the backup agent
  @Override
  public void onCreate() {
    SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS_NAME);
    addHelper(PREFS_BACKUP_KEY, helper);
  }
}
