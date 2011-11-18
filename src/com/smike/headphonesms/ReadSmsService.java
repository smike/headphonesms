package com.smike.headphonesms;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class ReadSmsService extends Service {
  public static final String MESSAGES_EXTRA = "com.smike.headphonesms.MESSAGES";
  public static final String STOP_READING_EXTRA = "com.smike.headphonesms.STOP_READING";

  private static final String LOG_TAG = "HeadphoneSmsApp";

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
    synchronized(messageQueue) {
      if (intent.hasExtra(STOP_READING_EXTRA)) {
        messageQueue.clear();
        if (tts != null) {
          // This will trigger onUtteranceCompleted, so we don't have to worry about cleaning up.
          tts.stop();
        }

        // We still want to stick around long enough for onUtteranceCompleted to get called.
        return START_STICKY;
      }

      final List<String> messages = intent.getStringArrayListExtra(MESSAGES_EXTRA);

      if (tts != null) {
        messageQueue.addAll(messages);
      } else {
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
              messageQueue.addAll(messages);
              speak(messageQueue.peek());
            }
          }
        });
      }

      return START_STICKY;
    }
  }

  public void speak(final String text) {
    Log.i(LOG_TAG, "speaking " + text);
    final HashMap<String, String> params = new HashMap<String, String>();
    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
               String.valueOf(AudioManager.STREAM_VOICE_CALL));

    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "valueNotUsed");
    // The first message should clear the queue so we can start speaking right away.
    audioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
  }
}
