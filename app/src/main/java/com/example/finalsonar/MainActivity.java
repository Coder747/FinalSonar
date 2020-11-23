package com.example.finalsonar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileNotFoundException;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    Button StopButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager x = new AudioManager(getApplicationContext());

        //When we press button the recording stops
        StopButton = findViewById(R.id.stop_button);
        StopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                x.stopRecord();
            }
        });


        try {
            x.PlayRecordAudio();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            x.saveWaveFiles("5alsana");
            System.out.println("5alsana");
        } catch (OutOfMemoryError | IOException e) {
            e.printStackTrace();
        }


    }
}
