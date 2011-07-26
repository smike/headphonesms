package com.smike.headphonesms;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle state){
     super.onCreate(state);
     addPreferencesFromResource(R.xml.preferences);
  }
}
