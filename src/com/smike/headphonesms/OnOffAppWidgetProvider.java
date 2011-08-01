package com.smike.headphonesms;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class OnOffAppWidgetProvider extends AppWidgetProvider {
  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
  }

  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    // Perform this loop procedure for each App Widget that belongs to this provider
    for (int i = 0; i < appWidgetIds.length; i++) {
      int appWidgetId = appWidgetIds[i];

      // Get the layout for the App Widget and attach an on-click listener to the button
      RemoteViews views = buildViews(appWidgetId, context);

      // Tell the AppWidgetManager to perform an update on the current app widget
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  private RemoteViews buildViews(int appWidgetId, Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean enabled =
        sharedPreferences.getBoolean(context.getString(R.string.prefsKey_enabled), false);

    // Create an Intent to launch ExampleActivity
    Intent intent = new Intent(context, ToggleOnOffService.class);
    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.onoff_widget);
    remoteViews.setOnClickPendingIntent(R.id.onoff_widget_button, pendingIntent);

    int imageResource = enabled ? R.drawable.onoff_widget_on : R.drawable.onoff_widget_off;
    remoteViews.setImageViewResource(R.id.onoff_widget_button, imageResource);
    return remoteViews;
  }

  public static void update(Context context) {
    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
    ComponentName widgetComponent = new ComponentName(context, OnOffAppWidgetProvider.class);
    int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

    Intent updateIntent = new Intent();
    updateIntent.setClass(context, OnOffAppWidgetProvider.class);
    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
    context.sendBroadcast(updateIntent);
  }
}