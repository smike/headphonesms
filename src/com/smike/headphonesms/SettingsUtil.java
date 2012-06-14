package com.smike.headphonesms;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsUtil {
  public enum ActivationMode {
    ALWAYS_ON,
    HEADPHONES_ONLY
  }

  private static SharedPreferences getSharedPreferences(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static boolean isEnabled(Context context) {
    return getSharedPreferences(context).getBoolean(
        context.getString(R.string.prefsKey_enabled), false);
  }

  public static ActivationMode getActivationMode(Context context) {
    String activationMode = getSharedPreferences(context).getString(
        context.getString(R.string.prefsKey_activationMode), null);
    if (activationMode.equals(context.getString(R.string.activationModeValue_always))) {
      return ActivationMode.ALWAYS_ON;
    } else {
      return ActivationMode.HEADPHONES_ONLY;
    }
  }

  public static int getVolume(Context context) {
    return getSharedPreferences(context).getInt(
        context.getString(R.string.prefsKey_volume), -1);
  }

  public static boolean isPreferSco(Context context) {
    return getSharedPreferences(context).getBoolean(
        context.getString(R.string.prefsKey_preferSco), false);
  }
}
