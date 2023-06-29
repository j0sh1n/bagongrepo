package com.example.lookbackapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class Activity_ForgotPassword extends AppCompatActivity {

    EditText edit_email_address;
    Button btn_reset;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edit_email_address = findViewById(R.id.edit_email);
        btn_reset = findViewById(R.id.btn_reset);


        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_email_address.getText().toString().length() > 0){
                    reset();
                }else{
                    Toast.makeText(getApplicationContext(), "Please provide your email address.", Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void reset() {

        FirebaseAuth.getInstance().sendPasswordResetEmail(edit_email_address.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Please check your email.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }
}
