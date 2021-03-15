package com.example.mp3playercw2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Environment;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Initialise our service variable
    private MP3Service.MyBinder myService = null;
    // Flag for destroying activity while stopped
    int flag = 0;
    // Initialise our service connection
    private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("g53mdp", "MainActivity onServiceConnected");
                myService = (MP3Service.MyBinder) service;
                myService.registerCallback(callback);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("g53mdp", "MainActivity onServiceDisconnected");
                myService.unregisterCallback(callback);
                myService = null;
            }
        };

    // Set up the callback
    ICallback callback = new ICallback() {
        @Override
        public void counterEvent(final int counter) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Display current duration in minutes and seconds
                    final TextView currentSec = (TextView)findViewById(R.id.curSec);
                    int minutes = (counter / 1000) / 60;
                    int seconds = (counter / 1000) % 60;
                    if (seconds < 10) {
                        currentSec.setText("" + minutes + ":0" + seconds);
                    } else {
                        currentSec.setText("" + minutes + ":" + seconds);
                    }
                    // Display Full duration in minutes and seconds
                    final TextView maxSec = (TextView)findViewById(R.id.MaxSec);
                    int fullMinutes = (myService.GetMusicDuration() / 1000) / 60;
                    int fullSeconds = (myService.GetMusicDuration() / 1000) % 60;
                    if (fullSeconds < 10) {
                        maxSec.setText("" + fullMinutes + ":0" + fullSeconds);
                    } else {
                        maxSec.setText("" + fullMinutes + ":" + fullSeconds);
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Bind to our mp3 service with flag 0, meaning if no service exists don't create new one
        this.bindService(new Intent(this, MP3Service.class), serviceConnection, 0);
        // Locate the listView
        final ListView lv = (ListView) findViewById(R.id.listView);
        // Load the contents of the Music Directory
        File musicDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/");
        File list[] = musicDir.listFiles();
        // Set up an adapter to select from the list
        lv.setAdapter(new ArrayAdapter<File>(this, android.R.layout.simple_list_item_1, list));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng) {
                File selectedFromList = (File) (lv.getItemAtPosition(myItemInt));
                Log.d("g53mdp Main", selectedFromList.getAbsolutePath());
                // Intent to start our service with the file path
                Intent mIntent = new Intent(MainActivity.this,MP3Service.class);
                Bundle bundle = new Bundle();
                bundle.putString("fileString",selectedFromList.getAbsolutePath());
                mIntent.putExtras(bundle);
                // Start our service using start service so that it doesn't kill it after activity destroyed
                startService(mIntent);
                // Also bind to our mp3 service
                bindService(new Intent(MainActivity.this, MP3Service.class), serviceConnection, Context.BIND_AUTO_CREATE);
                // Set our flag to represent that we disconnected from the service
                flag = 0;
            }
        });
    }

    public void OnPlay(View v) {
        if (myService != null && myService.GetMusicState() == MP3Player.MP3PlayerState.PAUSED){
            myService.PlaySong();
        } else if(myService != null && myService.GetMusicState() == MP3Player.MP3PlayerState.PLAYING) {
            Log.d("g53mdp Main", "Already playing.");
            Toast.makeText(getApplicationContext(),"Already playing!",Toast.LENGTH_SHORT).show();
        } else {
            Log.d("g53mdp Main", "Error no track selected to play");
            Toast.makeText(getApplicationContext(),"Error no track selected to play!",Toast.LENGTH_SHORT).show();
        }
    }

    public void OnPause(View v) {
        if (myService != null && myService.GetMusicState() == MP3Player.MP3PlayerState.PAUSED){
            Log.d("g53mdp Main", "Already Paused");
            Toast.makeText(getApplicationContext(),"Already Paused!",Toast.LENGTH_SHORT).show();
        } else if(myService != null && myService.GetMusicState() == MP3Player.MP3PlayerState.PLAYING){
            myService.PauseSong();
        } else {
            Log.d("g53mdp Main", "Error no track selected to pause");
            Toast.makeText(getApplicationContext(),"Error no track selected to pause!",Toast.LENGTH_SHORT).show();
        }
    }

    public void OnStop(View v) {
        if (myService != null && (myService.GetMusicState() == MP3Player.MP3PlayerState.PAUSED || myService.GetMusicState() == MP3Player.MP3PlayerState.PLAYING)){
            // Stop the song
            myService.StopSong();
            // Stop the service
            stopService(new Intent(MainActivity.this,MP3Service.class));
            // Unbind from service
            unbindService(serviceConnection);
            // Get rid of the callback
            myService.unregisterCallback(callback);
            // Reset the currentSec and maxSec fields to 0
            final TextView currentSec = (TextView)findViewById(R.id.curSec);
            currentSec.setText("0:00");
            final TextView maxSec = (TextView)findViewById(R.id.MaxSec);
            maxSec.setText("0:00");
            // Set our flag to represent we disconnected from the service
            flag = 1;
        } else {
            Log.d("g53mdp Main", "Error no track to stop");
            Toast.makeText(getApplicationContext(),"Error no track selected to stop!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy(){
        Log.d("g53mdp Main", "onDestroy");
        super.onDestroy();
        // Unbind from the service unless already unbound(checked by the flag)
        if(serviceConnection!=null && flag == 0) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }
}
