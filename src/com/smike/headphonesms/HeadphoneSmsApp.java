package com.smike.headphonesms;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

public class HeadphoneSmsApp extends BroadcastReceiver {
  private static final String LOG_TAG = "HeadphoneSmsApp";
  private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";
  private static final String CALL_ACTION = "android.intent.action.PHONE_STATE";

  public void onReceive(Context context, Intent intent) {
    if (shouldRead(context)) {
      Bundle bundle = intent.getExtras();
      if (bundle == null) {
        Log.w(LOG_TAG, "Bundle is null. Doing nothing.");
        return;
      }

      ArrayList<String> messages = new ArrayList<String>();
      if (intent.getAction().equals(SMS_ACTION)) {
        TelephonyManager telephonyManager =
            (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        // Don't start reading text messages when we are on the phone.
        if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
          Log.i(LOG_TAG, "Not reading SMS because the there is an ongoing call.");
          return;
        }

        Log.i(LOG_TAG, "SMS received, reading SMS.");
        Object pdus[] = (Object[]) bundle.get("pdus");
        for (Object pdu : pdus) {
          SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
          String from = getContactNameFromNumber(smsMessage.getDisplayOriginatingAddress(),
                                                 context.getContentResolver());

          String text = "Received SMS from " + from + ": " + smsMessage.getDisplayMessageBody();
          messages.add(text);
        }
      } else if (intent.getAction().equals(CALL_ACTION)) {
        String state = bundle.getString(TelephonyManager.EXTRA_STATE);
        if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
          Log.i(LOG_TAG, "Call received, announcing caller.");
          String phonenumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
          String from = getContactNameFromNumber(phonenumber, context.getContentResolver());

          String text = "Receiving call from " + from + ".";
          messages.add(text);
        } else if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
          Log.i(LOG_TAG, "Call answered, stopping reading.");
          Intent readSmsIntent = new Intent(context, ReadSmsService.class);
          readSmsIntent.putExtra(ReadSmsService.STOP_READING_EXTRA, "");
          context.startService(readSmsIntent);
          return;
        }
      } else {
        Log.w(LOG_TAG, "Received unrecognized action broadcast: " + intent.getAction());
      }

      if (!messages.isEmpty()) {
        Intent readSmsIntent = new Intent(context, ReadSmsService.class);
        readSmsIntent.putStringArrayListExtra(ReadSmsService.MESSAGES_EXTRA, messages);
        context.startService(readSmsIntent);
      }
    } else {
      Log.i(LOG_TAG, "Headset not connected, doing nothing");
    }
  }

  private String getContactNameFromNumber(String number, ContentResolver contentResolver) {
    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    Cursor cursor =
        contentResolver.query(uri, new String[]{ PhoneLookup.DISPLAY_NAME }, null, null, null);

    if (cursor.isAfterLast()) {
      // If nothing was found, return the number....
      Log.w(LOG_TAG, "Unable to look up incoming number in contacts");
      return number;
    }

    // ...otherwise return the first entry.
    cursor.moveToFirst();
    int nameFieldColumnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
    String contactName = cursor.getString(nameFieldColumnIndex);
    cursor.close();
    return contactName;
  }

  private boolean shouldRead(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    if (!sharedPreferences.getBoolean(context.getString(R.string.prefsKey_enabled), false)) {
      return false;
    }

    String activationModeString =
        sharedPreferences.getString(context.getString(R.string.prefsKey_activationMode), null);
    if (activationModeString.equals(context.getString(R.string.activationModeValue_always))) {
      return true;
    }

    // else if (activationModeString.equals(context.getString(R.string.activationModeValue_headphonesOnly))) {
    AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    return audioManager.isBluetoothA2dpOn() || audioManager.isWiredHeadsetOn();
  }
}