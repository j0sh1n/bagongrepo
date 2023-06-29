package com.example.lookbackapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class BackGroundService extends Service {
    @Nullable
    @Override


    public IBinder onBind(Intent intent) {
        return null;
    }

    String CHANNEL_ID = "100";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd MM yyyy");
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String Sdate = formatter.format(calendar.getTime());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){
            NotificationChannel channel= new NotificationChannel(CHANNEL_ID,"My Notification",NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        //notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);


        SessionManager sessionManager = new SessionManager(this);
        String type = sessionManager.getType();
        String id = sessionManager.getID();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference db = firebaseDatabase.getReference("CovidPositive" + "/" + Sdate); //check for covid positive patient

        //attach an action when clicking the notification
        Intent intents= new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intents, PendingIntent.FLAG_IMMUTABLE);


        if (type.equals("management")) { // if user type is management
            db.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String current_notif_id = sessionManager.getKeyNotificationId(); // get the current positive patient to avoid redundant notification
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (dataSnapshot.child("visited").exists()) {
                            String list = (String) dataSnapshot.child("visited").getValue();
                            if (list.contains(id)) {// check if user is a close contact

                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle("Covid Positive Alert!");
                                builder.setContentText("A Covid Positive was in your store. Kindly coordinate to the local authorities.");
                                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
                                builder.setContentIntent(pendingIntent);
                                builder.setAutoCancel(true);

                                NotificationManagerCompat managerCompat=NotificationManagerCompat.from(getApplicationContext());
                                managerCompat.notify(100,builder.build());

                                getContacts(Sdate, dataSnapshot.getKey()); //get the users visited in the store
                                if (!current_notif_id.equals(dataSnapshot.getKey())) {//
                                    sessionManager.addNotif(dataSnapshot.getKey());//keep the recent notification
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        } else {
            db.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String current_notif_id = sessionManager.getKeyNotificationId();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (dataSnapshot.child("contacts").exists()) { //same process with the management
                            String list = (String) dataSnapshot.child("contacts").getValue();
                            if (list.contains(id)) {
                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle("Covid Positive Alert!");
                                builder.setContentText("You're registered as a close contact to a Covid Positive Patient. Kindly report to the local authorities.");
                                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                                builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
                                builder.setContentIntent(pendingIntent);
                                builder.setAutoCancel(true);
                                NotificationManagerCompat managerCompat=NotificationManagerCompat.from(getApplicationContext());

                                if (!dataSnapshot.getKey().equals(id)) // it wont notify if you're the positive patient
                                    managerCompat.notify(100,builder.build());

                                if (!current_notif_id.equals(dataSnapshot.getKey())) {
                                    sessionManager.addNotif(dataSnapshot.getKey());
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


        }


        return START_STICKY;
    }

    //get users entered the building
    private void getContacts(String sdate, String key) {

        SessionManager sessionManager = new SessionManager(this);
        String id = sessionManager.getID();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference db = firebaseDatabase.getReference("Management" + "/" + id + "/History/" + sdate);
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String list = (String) snapshot.getValue();
                    updateDb(list, key, sdate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    //create contact list
    private void updateDb(String list, String key, String date) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference db = firebaseDatabase.getReference("CovidPositive" + "/" + date + "/" + key);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("contacts", list);
        db.updateChildren(hashMap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
