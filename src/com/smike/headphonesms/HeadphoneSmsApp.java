package com.smike.headphonesms;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;
import android.util.Log;

public class HeadphoneSmsApp extends BroadcastReceiver {
  public static final String MESSAGES_EXTRA = "com.smike.headphonesms.MESSAGES";

  private static final String LOG_TAG = "HeadphoneSmsApp";
  private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(ACTION)) {
      AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      if (audioManager.isBluetoothA2dpOn() || audioManager.isWiredHeadsetOn()) {
        Log.i(LOG_TAG, "Headset connected, reading SMS");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
          Object pdus[] = (Object[]) bundle.get("pdus");
          ArrayList<String> messages = new ArrayList<String>();
          for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            String from = getContactNameFromNumber(smsMessage.getDisplayOriginatingAddress(),
                                                   context.getContentResolver());

            String text = "Received SMS from " + from + ". " + smsMessage.getDisplayMessageBody();

            messages.add(text);
          }

          Intent readSmsIntent = new Intent(context, ReadSmsService.class);
          readSmsIntent.putStringArrayListExtra(MESSAGES_EXTRA, messages);
          context.startService(readSmsIntent);
        }
      } else {
        Log.i(LOG_TAG, "Headset not connected, doing nothing");
      }
    } else {
      Log.w(LOG_TAG, "Received unrecognized action broadcast: " + intent.getAction());
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
}