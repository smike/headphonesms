package com.smike.headphonesms;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

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
  public static final int READING_AUDIO_STREAM = AudioManager.STREAM_VOICE_CALL;

  private static final String BLUETOOTH_TIMEOUT_EXTRA = "com.smike.headphonesms.BLUETOOTH_TIMEOUT";
  private static final String QUEUE_MESSAGE_EXTRA = "com.smike.headphonesms.QUEUE_MESSAGE";
  private static final String START_READING_EXTRA = "com.smike.headphonesms.START_READING";
  private static final String STOP_READING_EXTRA = "com.smike.headphonesms.STOP_READING";

  private static final String LOG_TAG =
      HeadphoneSmsApp.LOG_TAG + "." + ReadSmsService.class.getSimpleName();

  private final LocalBinder binder = new LocalBinder();
  private Queue<String> messageQueue = new LinkedList<String>();
  private TimerTask bluetoothTimerTask;

  private TextToSpeech tts;

  private AudioManager audioManager;
  private int systemVolume;

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
      stopSelf();
      return START_NOT_STICKY;
    }

    synchronized(messageQueue) {
      if (intent.hasExtra(BLUETOOTH_TIMEOUT_EXTRA)) {
        if (bluetoothTimerTask != null) {
          audioManager.stopBluetoothSco();
          bluetoothTimerTask.cancel();
          bluetoothTimerTask = null;

          if (HeadphoneSmsApp.shouldRead(false, this)) {
            // If SCO failed but we have another method we can read through, do so.
            startReading(this);
          }
        }
      } else if (intent.hasExtra(STOP_READING_EXTRA)) {
        if (tts != null) {
          // This will trigger onUtteranceCompleted, so we don't have to worry about cleaning up.
          tts.stop();
          messageQueue.clear();
        }

        // We still want to stick around long enough for onUtteranceCompleted to get called, so let
        // it call stopSelf();
      } else if (intent.hasExtra(QUEUE_MESSAGE_EXTRA)) {
        String message = intent.getStringExtra(QUEUE_MESSAGE_EXTRA);
        messageQueue.add(message);

        // if the TTS service is already running, just queue the message
        if (tts == null) {
          if (!SettingsUtil.isPreferSco(this) &&
              (audioManager.isBluetoothA2dpOn() ||
               audioManager.isWiredHeadsetOn())) {
            // We prefer to use non-sco if it's available. The logic is that if you have your
            // headphones on in the car, the whole car shouldn't hear your messages.
            ReadSmsService.startReading(this);
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO &&
              audioManager.isBluetoothScoAvailableOffCall()) {
            Log.i(LOG_TAG, "Starting SCO, will wait until it is connected. sco on: " +
                audioManager.isBluetoothScoOn());
            audioManager.startBluetoothSco();
            bluetoothTimerTask = new TimerTask() {
              @Override
              public void run() {
                ReadSmsService.bluetoothTimeout(ReadSmsService.this);
              }
            };
            Timer timer = new Timer("bluetoothTimeoutTimer");
            timer.schedule(bluetoothTimerTask, 5000);
          } else if (HeadphoneSmsApp.shouldRead(false, this)) {
            // In case we should read anyway (reading is always on)
            ReadSmsService.startReading(this);
          }
        }
      } else if (intent.hasExtra(START_READING_EXTRA)) {
        if (bluetoothTimerTask != null) {
          // Probably triggered by a bluetooth connection. reset;
          bluetoothTimerTask.cancel();
          bluetoothTimerTask = null;
        }

        if (tts == null && !messageQueue.isEmpty()) {
          tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
              tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                  restoreAudio();
                  synchronized (messageQueue) {
                    messageQueue.poll();
                    if (messageQueue.isEmpty()) {
                      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        // Sleep a little to give the bluetooth device a bit longer to finish.
                        try {
                          Thread.sleep(1000);
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
    Log.i(LOG_TAG, "speaking \"" + text + "\"");
    final HashMap<String, String> params = new HashMap<String, String>();
    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
               String.valueOf(READING_AUDIO_STREAM));

    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "valueNotUsed");
    prepareAudio();
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
  }

  private void prepareAudio() {
    audioManager.setStreamSolo(READING_AUDIO_STREAM, true);

    int desiredVolume = SettingsUtil.getVolume(this);

    systemVolume = audioManager.getStreamVolume(READING_AUDIO_STREAM);

    // -1 means use the system volume.
    if (desiredVolume != -1) {
      int boudedDesiredVolume =
          Math.min(desiredVolume, audioManager.getStreamMaxVolume(READING_AUDIO_STREAM));
      Log.i(LOG_TAG, "Temporarily setting volume to " + boudedDesiredVolume);
      audioManager.setStreamVolume(READING_AUDIO_STREAM, boudedDesiredVolume, 0);
    }
  }

  private void restoreAudio() {
    Log.i(LOG_TAG, "Resetting volume to " + systemVolume);
    audioManager.setStreamVolume(READING_AUDIO_STREAM, systemVolume, 0);
    audioManager.setStreamSolo(READING_AUDIO_STREAM, false);
  }

  public static void queueMessage(String message, Context context) {
    Log.i(LOG_TAG, "Queueing message: " + message);
    sendIntent(ReadSmsService.QUEUE_MESSAGE_EXTRA, message, context);
  }

  public static void startReading(Context context) {
    Log.i(LOG_TAG, "Starting to read message");
    sendIntent(ReadSmsService.START_READING_EXTRA, null, context);
  }

  public static void stopReading(Context context) {
    Log.i(LOG_TAG, "Stopping reading of messages");
    sendIntent(ReadSmsService.STOP_READING_EXTRA, null, context);
  }

  public static void bluetoothTimeout(Context context) {
    Log.i(LOG_TAG, "Timedout waiting for bluetooth.");
    sendIntent(ReadSmsService.BLUETOOTH_TIMEOUT_EXTRA, null, context);
  }

  private static void sendIntent(String extraName, String extraValue, Context context) {
    Intent readSmsIntent = new Intent(context, ReadSmsService.class);
    readSmsIntent.putExtra(extraName, extraValue);
    context.startService(readSmsIntent);
  }
}