package com.smike.headphonesms;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class ReadSmsService extends Service {
  private static final String QUEUE_MESSAGE_EXTRA = "com.smike.headphonesms.QUEUE_MESSAGE";
  private static final String START_READING_EXTRA = "com.smike.headphonesms.START_READING";
  private static final String STOP_READING_EXTRA = "com.smike.headphonesms.STOP_READING";

  private static final String LOG_TAG = ReadSmsService.class.getSimpleName();

  private final LocalBinder binder = new LocalBinder();
  private Queue<String> messageQueue = new LinkedList<String>();

  private TextToSpeech tts;

  private AudioManager audioManager;

  public class LocalBinder extends Binder {
    ReadSmsService getService() {
      return ReadSmsService.this;
    }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return binder;
  }

  @Override
  public void onCreate() {
    audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      // Service restarted after suspension. Nothing to do.
      return START_STICKY;
    }

    synchronized(messageQueue) {
      if (intent.hasExtra(STOP_READING_EXTRA)) {
        messageQueue.clear();
        if (tts != null) {
          // This will trigger onUtteranceCompleted, so we don't have to worry about cleaning up.
          tts.stop();
        }

        // We still want to stick around long enough for onUtteranceCompleted to get called, so let
        // it call stopSelf();
      } else if (intent.hasExtra(QUEUE_MESSAGE_EXTRA)) {
        String message = intent.getStringExtra(QUEUE_MESSAGE_EXTRA);
        messageQueue.add(message);

        // if the TTS service is already running, just queue the message
        if (tts == null) {
          if (audioManager.isBluetoothA2dpOn() ||
              audioManager.isWiredHeadsetOn()) {
            // We prefer to use non-sco if it's available. The logic is that if you have your
            // headphones on in the car, the whole car shouldn't hear your messages.
            ReadSmsService.startReading(this);
          } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO &&
              audioManager.isBluetoothScoAvailableOffCall()) {
            Log.i(LOG_TAG, "Starting SCO, will wait until it is connected.");
            audioManager.startBluetoothSco();
          }
        }
      } else if (intent.hasExtra(START_READING_EXTRA)) {
        if (tts == null && !messageQueue.isEmpty()) {
          tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
              tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                  audioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                  synchronized (messageQueue) {
                    messageQueue.poll();
                    if (messageQueue.isEmpty()) {
                      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        // Sleep a little to give the bluetooth device a bit longer to finish.
                        try {
                          Thread.sleep(500);
                        } catch (InterruptedException e) {
                          Log.w(LOG_TAG, e.toString());
                        }
                        audioManager.stopBluetoothSco();
                      }
                      tts.shutdown();
                      tts = null;
                      ReadSmsService.this.stopSelf();
                      Log.i(LOG_TAG, "Nothing else to speak. Shutting down TTS, stopping service.");
                    } else {
                      Log.i(LOG_TAG, "Speaking next message.");
                      speak(messageQueue.peek());
                    }
                  }
                }
              });

              synchronized (messageQueue) {
                speak(messageQueue.peek());
              }
            }
          });
        }
      }

      return START_STICKY;
    }
  }

  private void speak(final String text) {
    // The first message should clear the queue so we can start speaking right away.
    Log.i(LOG_TAG, "speaking " + text);
    final HashMap<String, String> params = new HashMap<String, String>();
    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
               String.valueOf(AudioManager.STREAM_VOICE_CALL));

    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "valueNotUsed");
    audioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
  }

  public static void queueMessage(String message, Context context) {
    Log.i(LOG_TAG, "Queueing message: " + message);
    Intent readSmsIntent = new Intent(context, ReadSmsService.class);
    readSmsIntent.putExtra(ReadSmsService.QUEUE_MESSAGE_EXTRA, message);
    context.startService(readSmsIntent);
  }

  public static void startReading(Context context) {
    Log.i(LOG_TAG, "Starting to read message");
    Intent readSmsIntent = new Intent(context, ReadSmsService.class);
    readSmsIntent.putExtra(ReadSmsService.START_READING_EXTRA, "");
    context.startService(readSmsIntent);
  }

  public static void stopReading(Context context) {
    Log.i(LOG_TAG, "Stopping reading of messages");
    Intent readSmsIntent = new Intent(context, ReadSmsService.class);
    readSmsIntent.putExtra(ReadSmsService.STOP_READING_EXTRA, "");
    context.startService(readSmsIntent);
  }
}