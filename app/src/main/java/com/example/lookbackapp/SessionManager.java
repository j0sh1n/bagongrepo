package com.example.lookbackapp;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {

    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    SharedPreferences.Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "LookBackApp";

    private static final String IS_LOGIN = "IsLoggedIn";
    public static final String KEY_ID = "userid";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_USER_TYPE = "user_type";
    public static final String KEY_NOTIFICATION_ID = "notification_id";
    public static final String KEY_STATUS = "status";


    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String user_id, String password, String email, String user_type) {
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);

        // Storing name in pref
        editor.putString(KEY_ID, user_id);
        editor.putString(KEY_PASSWORD, password);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_USER_TYPE, user_type);

        // commit changes
        editor.commit();
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getID() {
        return pref.getString(KEY_ID, "");
    }

    public String getType() {
        return pref.getString(KEY_USER_TYPE, "");
    }

    public String getKeyNotificationId(){
        return pref.getString(KEY_NOTIFICATION_ID, "");
    }

    public String getStatus(){
        return pref.getString(KEY_STATUS, "negative");
    }


    // Get user details
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        // user name
        user.put(KEY_ID, pref.getString(KEY_ID, ""));
        user.put(KEY_PASSWORD, pref.getString(KEY_PASSWORD, ""));
        user.put(KEY_EMAIL, pref.getString(KEY_EMAIL, ""));
        user.put(KEY_USER_TYPE, pref.getString(KEY_USER_TYPE, ""));

        // return user
        return user;
    }

    public void addNotif(String id){
        editor.putString(KEY_NOTIFICATION_ID, id);
        editor.commit();
    }

    public void addStatus(String status){
        editor.putString(KEY_STATUS, status);
        editor.commit();
    }


    // Get Login State
    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }

    public void logout(){
        editor.clear();
        editor.commit();
    }
}

