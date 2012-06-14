package com.smike.headphonesms;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.smike.headphonesms.SettingsUtil.ActivationMode;

public class HeadphoneSmsApp extends BroadcastReceiver {
  public static final String LOG_TAG = HeadphoneSmsApp.class.getSimpleName();

  private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";
  private static final String CALL_ACTION = "android.intent.action.PHONE_STATE";

  public void onReceive(Context context, Intent intent) {
    Bundle bundle = intent.getExtras();
    if (bundle == null) {
      Log.w(LOG_TAG, "Bundle is null. Doing nothing.");
      return;
    }

    if (shouldRead(true, context)) {
      if (intent.getAction().equals(SMS_ACTION)) {
        receivedSmsAction(bundle, context);
      } else if (intent.getAction().equals(CALL_ACTION)) {
        receivedCallAction(bundle, context);
      } else {
        Log.w(LOG_TAG, "Received unrecognized action broadcast: " + intent.getAction());
      }
    } else {
      Log.i(LOG_TAG, "Headset not connected or disabled, doing nothing");
    }
  }

  private void receivedSmsAction(Bundle bundle, Context context) {
    TelephonyManager telephonyManager =
        (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    // Don't start reading text messages when we are on the phone.
    if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
      Log.i(LOG_TAG, "Not reading SMS because there is an ongoing call.");
      return;
    }

    Log.i(LOG_TAG, "SMS received, reading SMS.");
    Object pdus[] = (Object[]) bundle.get("pdus");
    for (Object pdu : pdus) {
      SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
      String from = getContactNameFromNumber(smsMessage.getDisplayOriginatingAddress(),
                                             context.getContentResolver());

      String text = context.getString(R.string.speech_receivedSms) + " " + from + ": "
          + smsMessage.getDisplayMessageBody();
      ReadSmsService.queueMessage(text, context);
    }
  }

  private void receivedCallAction(Bundle bundle, Context context) {
    String state = bundle.getString(TelephonyManager.EXTRA_STATE);
    if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
      Log.i(LOG_TAG, "Call received, announcing caller.");
      String phonenumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
      String from = getContactNameFromNumber(phonenumber, context.getContentResolver());

      String text = context.getString(R.string.speech_receivedCall) + " " + from + ".";
      ReadSmsService.queueMessage(text, context);
    } else if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
      Log.i(LOG_TAG, "Call answered, stopping reading.");
      ReadSmsService.stopReading(context);
    }
  }

  private String getContactNameFromNumber(String number, ContentResolver contentResolver) {
    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

    Cursor cursor = null;
    try {
      cursor = contentResolver.query(uri, new String[]{ PhoneLookup.DISPLAY_NAME }, null, null, null);

      if (cursor.isAfterLast()) {
        // If nothing was found, return the number....
        Log.w(LOG_TAG, "Unable to look up incoming number in contacts");
        return number;
      }

      // ...otherwise return the first entry.
      cursor.moveToFirst();
      int nameFieldColumnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
      String contactName = cursor.getString(nameFieldColumnIndex);
      return contactName;
    } finally {
      cursor.close();
    }
  }

  public static boolean shouldRead(boolean canUseSco, Context context) {
    if (!SettingsUtil.isEnabled(context)) {
      return false;
    }

    if (SettingsUtil.getActivationMode(context) == ActivationMode.ALWAYS_ON) {
      return true;
    }

    // else if (activationModeString.equals(context.getString(R.string.activationModeValue_headphonesOnly))) {
    AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    return (canUseSco && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO &&
            audioManager.isBluetoothScoAvailableOffCall()) ||
        audioManager.isBluetoothA2dpOn() ||
        audioManager.isWiredHeadsetOn();
  }
}