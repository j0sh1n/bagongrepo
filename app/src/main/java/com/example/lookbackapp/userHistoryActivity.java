package com.example.lookbackapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class userHistoryActivity extends AppCompatActivity {

    int mYear,mMonth,mDay, dateFrom, dateTo;

    ImageView qrScanBtn, settingsBtn, notifBtn, filterBtn;
    ListView history;
    FirebaseAuth fAuth;
    FirebaseDatabase fbDb;
    DatabaseReference dbRef;
    String userId, fileName;
    Button searchBtn, printToPdfBtn;
    EditText textSearch, editTextFrom, editTextTo;
    LinearLayout layoutFilter, layoutSearch;
    Boolean filterOn;
    String listOfPlaces;


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_history);
        ActivityCompat.requestPermissions(userHistoryActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        fbDb        = FirebaseDatabase.getInstance("https://lookbackapp-2a576-default-rtdb.asia-southeast1.firebasedatabase.app/");
        dbRef       = fbDb.getReference("Users");
        fAuth       = FirebaseAuth.getInstance();
        userId      = fAuth.getCurrentUser().getUid();
        qrScanBtn   = (ImageView) findViewById(R.id.btnTarget);
        settingsBtn = (ImageView) findViewById(R.id.btnSetting);
        fileName    = userId + ".txt";
        history     = (ListView) findViewById(R.id.listViewHistory);
        notifBtn    = (ImageView) findViewById(R.id.btnStat);
        searchBtn   = (Button) findViewById(R.id.btnSearch);
        textSearch   = (EditText) findViewById(R.id.editTextSearch);
        layoutSearch = (LinearLayout) findViewById(R.id.layoutSearch);
        layoutFilter = (LinearLayout) findViewById(R.id.layoutFilter);
        filterBtn   = (ImageView) findViewById(R.id.filterBtn);
        filterOn    = false;
        editTextFrom = (EditText) findViewById(R.id.editTextFrom);
        editTextTo  = (EditText) findViewById(R.id.editTextTo);
        printToPdfBtn = (Button) findViewById(R.id.printToPdfBtn);

        retrieveFromDB(userId);

        filterBtn.setClickable(true);
        editTextFrom.setKeyListener(null);
        editTextTo.setEnabled(false);
        editTextTo.setKeyListener(null);

        printToPdfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                print(listOfPlaces);
                pdfExport(view);
            }
        });

        editTextFrom.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    editTextTo.setText(null);
                    setFrom();
                }
            }
        });

        editTextTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    setTo();
                }
            }
        });

        editTextFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTextTo.setText(null);
                setFrom();
            }
        });

        editTextTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTo();
            }
        });

        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!filterOn){
                    filterBtn.setAlpha(1.0F);
                    layoutSearch.setVisibility(View.INVISIBLE);
                    layoutFilter.setVisibility(View.VISIBLE);
                    filterOn = true;
                }
                else {
                    filterBtn.setAlpha(.5F);
                    layoutSearch.setVisibility(View.VISIBLE);
                    layoutFilter.setVisibility(View.INVISIBLE);
                    filterOn = false;
                }
            }
        });
        
        textSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    history.setVisibility(view.INVISIBLE);
                }
                else{
                    history.setVisibility(view.VISIBLE);
                }
            }
        });

        qrScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(userHistoryActivity.this, userQrScannerActivity.class));
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(userHistoryActivity.this, userSettingsActivity.class));
            }
        });

        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(userHistoryActivity.this, userNotificationActivity.class));
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(filterOn){
                    if(TextUtils.isEmpty(editTextFrom.getText().toString()) | TextUtils.isEmpty((editTextTo.getText().toString()))){
                        Toast.makeText(userHistoryActivity.this, "One or Both Fields are Empty", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        searchDB(userId);
                    }
                }
                else{
                    searchDB(userId);
                }
                String searchValue = textSearch.getText().toString();
                if (!filterOn){
                    Toast.makeText(userHistoryActivity.this, "Searching results for \"" + searchValue + "\"", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(userHistoryActivity.this, "Filtering Dates", Toast.LENGTH_SHORT).show();
                }

            }
        });

        textSearch.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String toSearch = textSearch.getText().toString();
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    print("Enter Pressed");
                    hideKeyboard(userHistoryActivity.this);
                    searchDB(toSearch);
                    return true;
                }
                return false;
            }
        });
    }

    public void pdfExport(View view){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy hh-mm-ssaa");
        String dateToday = formatter.format(date);
        PdfDocument PdfDocument = new PdfDocument();
        PdfDocument.PageInfo PageInfo = new PdfDocument.PageInfo.Builder(2480, 3508, 1).create();
        PdfDocument.Page Page = PdfDocument.startPage(PageInfo);

        Paint Paint = new Paint();
        int x = 100;
        int y = 250;
        for(String line: listOfPlaces.split("\n")) {
            Page.getCanvas().drawText(line, x, y, Paint);
            y += Paint.descent() - Paint.ascent();
        }
        PdfDocument.finishPage(Page);

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/Download/" + dateToday + ".pdf";
        File file = new File(filePath);
        try {
            PdfDocument.writeTo(new FileOutputStream(file));
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "ERROR ON PDF", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this, "PDF EXPORTED SUCCESSFULLY", Toast.LENGTH_SHORT).show();
        PdfDocument.close();

    }


    public void setFrom(){
        EditText editTextFrom = (EditText) findViewById(R.id.editTextFrom);
        Calendar myCalendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // myCalendar.add(Calendar.DATE, 0);
                String myFormat = "yyyy-MM-dd"; //In which you need put here
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                editTextFrom.setText(sdf.format(myCalendar.getTime()));
            }
        };
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        // Launch Date Picker Dialog
        DatePickerDialog dpd = new DatePickerDialog(userHistoryActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        // Display Selected date in textbox
                        if (year < mYear)
                            view.updateDate(mYear,mMonth,mDay);

                        if (monthOfYear < mMonth && year == mYear)
                            view.updateDate(mYear,mMonth,mDay);

                        if (dayOfMonth < mDay && year == mYear && monthOfYear == mMonth)
                            view.updateDate(mYear,mMonth,mDay);

                        editTextFrom.setText(dayOfMonth + "/"
                                + (monthOfYear + 1) + "/" + year);
                        dateFrom = 0;
                        dateFrom = dayOfMonth;
                        dateFrom = dateFrom + ((monthOfYear + 1) * 30);
                        dateFrom = dateFrom + (year * 360);

                    }
                }, mYear, mMonth, mDay);
        dpd.getDatePicker().setMinDate(0);
        dpd.show();
        editTextTo.setEnabled(true);
    }

    public void setTo(){
        EditText editTextTo = (EditText) findViewById(R.id.editTextTo);
        Calendar myCalendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // myCalendar.add(Calendar.DATE, 0);
                String myFormat = "yyyy-MM-dd"; //In which you need put here
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
                editTextFrom.setText(sdf.format(myCalendar.getTime()));
            }
        };
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        // Launch Date Picker Dialog
        DatePickerDialog dpd = new DatePickerDialog(userHistoryActivity.this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        // Display Selected date in textbox

                        if (year < mYear)
                            view.updateDate(mYear,mMonth,mDay);

                        if (monthOfYear < mMonth && year == mYear)
                            view.updateDate(mYear,mMonth,mDay);

                        if (dayOfMonth < mDay && year == mYear && monthOfYear == mMonth)
                            view.updateDate(mYear,mMonth,mDay);

                        editTextTo.setText(dayOfMonth + "/"
                                + (monthOfYear + 1) + "/" + year);
                        dateTo = 0;
                        dateTo = dayOfMonth;
                        dateTo = dateTo + ((monthOfYear + 1) * 30);
                        dateTo = dateTo + (year * 360);

                        if (dateFrom > dateTo){
                            Toast.makeText(userHistoryActivity.this, "FROM can't be greater than TO", Toast.LENGTH_SHORT).show();
                            editTextTo.setText(null);
                        }
                    }
                }, mYear, mMonth, mDay);
        dpd.getDatePicker().setMinDate(0);
        dpd.show();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void print(String printThis){
        System.out.println(printThis);
    }

    public void searchDB(String userID){
        String toSearch = textSearch.getText().toString();
        //Toast.makeText(this, "SEARCHING", Toast.LENGTH_SHORT).show();
        dbRef.child(userID).child("HISTORY").child("List").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String data = snapshot.getValue().toString();
                    searchToList(data);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    int x;
    ArrayList<String> historyList = new ArrayList<>();
    public void searchToList(String data){
        x = 0;
        historyList = new ArrayList<>();
        String histories[] = data.split("\n");
        DatabaseReference dbRefPush = dbRef.child(userId).child("HISTORY");
        int len = histories.length;
        listOfPlaces = "";
        for (int i = len-1; i >= 0 ; i--){
            dbRefPush.child(histories[i]).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    x++;
                    if(snapshot.exists()){
                        String data = snapshot.getValue().toString();
                        data = searchStringFix(data);
                        if (data != null){
                            addArrayList(data);
                        }
                        if (x == len){
                            putArrayList(historyList);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    public String searchStringFix(String data){
        String toSearch = textSearch.getText().toString();
        String toSearchDate1 = editTextFrom.getText().toString();
        String toSearchDate2 = editTextTo.getText().toString();
        String temp = data;
        //print(temp);
        StringBuilder temp1 = new StringBuilder();
        temp1.append(temp);
        temp1.reverse();
        temp = temp1.toString();
        //print(temp);
        String[] temp2 = temp.split(",",2);
        StringBuilder temp3 = new StringBuilder();
        String[] temp4 = {"",""};
        if (temp2.length != 1){
            temp3.append(temp2[0]);
            temp3.reverse();
            print(temp3.toString());
            if (filterOn == true){
                String[] temp3Split = temp3.toString().split("\\s");
                print(temp3Split[1]);
                String[] temp3Split2 = temp3Split[1].split("/");
                temp3Split2[0] = temp3Split2[0].substring(5);
                System.out.println(temp3Split2[0]);
                int split1, split2, split3, sum;
                split1 = Integer.parseInt(temp3Split2[0]);
                split2 = (Integer.parseInt(temp3Split2[1]) * 30) - 30;
                split3 = (Integer.parseInt(temp3Split2[2]) * 360) - 360;
                sum = split1 + split2 + split3;
                String[] from = toSearchDate1.split("/");
                int sFrom1, sFrom2, sFrom3, sFromSum;
                sFrom1 = Integer.parseInt(from[0]);
                sFrom2 = (Integer.parseInt(from[1]) * 30) - 30;
                sFrom3 = (Integer.parseInt(from[2]) * 360) - 360;
                sFromSum = sFrom1 + sFrom2 + sFrom3;
                String[] to = toSearchDate2.split("/");
                int sTo1, sTo2, sTo3, sToSum;
                sTo1 = Integer.parseInt(to[0]);
                sTo2 = (Integer.parseInt(to[1]) * 30) - 30;
                sTo3 = (Integer.parseInt(to[2]) * 360) - 360;
                sToSum = sTo1 + sTo2 + sTo3;
                if (sum >= sFromSum & sum <= sToSum){
                    temp4[1] = temp3.toString();
                }
            }
            temp2[0] = temp3.toString();
            temp3 = new StringBuilder();
            temp3.append(temp2[1]);
            temp3.reverse();
            if (temp3.toString().toLowerCase().contains(toSearch.toLowerCase()) && !filterOn){
                temp4[0] = temp3.toString();
            }
            temp2[1] = temp3.toString();
            data = "NAME : " + temp2[1].substring(6, temp2[1].length()) + "\nDATE : " + temp2[0].substring(6, temp2[0].length()-1);
        }
        if (temp4[0] != "" | temp4[1] != ""){
            print(data);
            listOfPlaces = listOfPlaces + data + "\n";
            return data;
        }
        else{
            return null;
        }
    }

    public void retrieveFromDB(String userID){
        dbRef.child(userID).child("HISTORY").child("List").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String data = snapshot.getValue().toString();
                    print(snapshot.getValue().toString());
                    saveLocal(data, fileName);
                    fileToList(data);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void saveLocal(String data, String fileName){
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fileToList(String data){
        System.out.println(data);
        String histories[] = data.split("\n");
        DatabaseReference dbRefPush = dbRef.child(userId).child("HISTORY");
        int len = histories.length;
        listOfPlaces = "";
        for (int i = len-1; i >= 0 ; i--){
            dbRefPush.child(histories[i]).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    x++;
                    if(snapshot.exists()){
                        String data = snapshot.getValue().toString();
                        data = dataStringFix(data);
                        addArrayList(data);
                        if (x == len){
                            putArrayList(historyList);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    public String dataStringFix(String data){
        String temp = data;
        StringBuilder temp1 = new StringBuilder();
        temp1.append(temp);
        temp1.reverse();
        temp = temp1.toString();
        String[] temp2 = temp.split(",",2);
        StringBuilder temp3 = new StringBuilder();
        if (temp2.length != 1){
            temp3.append(temp2[0]);
            temp3.reverse();
            temp2[0] = temp3.toString();
            temp3 = new StringBuilder();
            temp3.append(temp2[1]);
            temp3.reverse();
            temp2[1] = temp3.toString();
            data = "NAME : " + temp2[1].substring(6, temp2[1].length()) + "\nDATE : " + temp2[0].substring(6, temp2[0].length()-1);
            listOfPlaces = listOfPlaces + data + "\n";
        }
        return data;
    }

    public void addArrayList(String data){
        historyList.add(data);
    }

    public void putArrayList(ArrayList<String> phistoryList){
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, phistoryList);
        history.setAdapter(arrayAdapter);
    }

    @Override
    public void onBackPressed() {
    }

}