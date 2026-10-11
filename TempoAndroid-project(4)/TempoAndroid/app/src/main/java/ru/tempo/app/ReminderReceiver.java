package ru.tempo.app;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) {
    String title = i.getStringExtra("title");
    String text = i.getStringExtra("text");
    NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("tempo_reminders","Напоминания Tempo",NotificationManager.IMPORTANCE_HIGH));
    Notification n = new Notification.Builder(c, "tempo_reminders")
      .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title == null ? "Tempo" : title)
      .setContentText(text == null ? "Скоро событие" : text).setAutoCancel(true).build();
    nm.notify((int)System.currentTimeMillis(), n);
  }
}
