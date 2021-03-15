package com.example.mp3playercw2;

import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.os.Binder;
import android.os.IBinder;
import android.os.Build;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.content.Intent;
import android.util.Log;

public class MP3Service extends Service {

    private final String CHANNEL_ID = "100";
    int NOTIFICATION_ID = 001;

    RemoteCallbackList<MyBinder> remoteCallbackList = new RemoteCallbackList<MyBinder>();
    private final IBinder binder = new MyBinder();
    public MP3Player mp3 = new MP3Player();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service", "onCreated");
        // Set up the thread for displaying and fetching current progress of the track
        Thread displayProgress = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        int counter = mp3.getProgress();
                        doCallbacks(counter);
                    }
                } catch (InterruptedException e) {
                    Log.d("service", "Error in thread!");
                }
            }
        };
        displayProgress.start();
        // Notification Code
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel name";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
        // Intent to go back to the main activity
        Intent intent = new Intent(MP3Service.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // Build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("MP3Service")
                .setContentText("Return to the activity (MP3 App)")
                .setContentIntent(pendingIntent)
                //.addAction(R.drawable.ic_launcher_foreground, "Kill Service", pendingKillServiceIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("service", "service onBind");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "service onStartCommand");
        //Log.d("service", "intent: " + intent +" flag: " + flags + " startID: " + startId);
        if(intent.hasExtra("fileString")){
            mp3.stop();
            mp3.load(intent.getExtras().getString("fileString"));
        }
        return Service.START_STICKY;
    }

    public void doCallbacks(int count) {
        final int n = remoteCallbackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            remoteCallbackList.getBroadcastItem(i).callback.counterEvent(count);
        }
        remoteCallbackList.finishBroadcast();
    }

    public class MyBinder extends Binder implements IInterface {

        ICallback callback;

        @Override
        public IBinder asBinder() {
            return this;
        }

        void registerCallback(ICallback callback) {
            this.callback = callback;
            remoteCallbackList.register(MyBinder.this);
        }
        void unregisterCallback(ICallback callback) {
            remoteCallbackList.unregister(MyBinder.this);
        }

        void PlaySong() {
            mp3.play();
            Log.d("service", "play");
        }
        void PauseSong(){
            mp3.pause();
            Log.d("service", "pause");
        }
        void StopSong() {
            mp3.stop();
            Log.d("service", "stop");
        }
        MP3Player.MP3PlayerState GetMusicState(){
            return mp3.getState();
        }
        int GetMusicDuration(){
            return mp3.getDuration();
        }
    }

    @Override
    public void onDestroy() {
        Log.d("service", "service onDestroy");
        super.onDestroy();
        stopForeground(true);
        mp3.stop();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("service", "service onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("service", "service onUnbind");
        //Log.d("service", "intent: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
        stopSelf();
    }
}
