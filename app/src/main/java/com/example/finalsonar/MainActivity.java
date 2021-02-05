package com.example.finalsonar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;


public class MainActivity extends AppCompatActivity {

    private Button B_StopButton, B_StartButton;
    private CheckBox CB_Specific_time;
    private EditText ET_seconds_input;
    private TextView TV_seconds;
    private int recording_duration;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager x = new AudioManager(getApplicationContext());

        //Setup specific recording duration
        ET_seconds_input = findViewById(R.id.Recording_Duration);
        TV_seconds = findViewById(R.id.text_seconds);
        CB_Specific_time = findViewById(R.id.Enable_specific_time);
        CB_Specific_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CB_Specific_time.isChecked()) {
                    ET_seconds_input.setVisibility(View.VISIBLE);
                    TV_seconds.setVisibility(View.VISIBLE);
                }
                else
                {
                    ET_seconds_input.setVisibility(View.GONE);
                    TV_seconds.setVisibility(View.GONE);
                }
            }
        });




        //Setup the start button to start recording
        B_StartButton = findViewById(R.id.start_button);
        B_StartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    x.PlayRecordAudio();

                if (CB_Specific_time.isChecked()) {
                        String duration = ET_seconds_input.getEditableText().toString();
                        recording_duration = Integer.parseInt(duration) * 1000;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                x.stopRecord();
                                try {
                                    x.saveWaveFiles("5alsana");
                                    System.out.println("5alsana");
                                } catch (OutOfMemoryError | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        },recording_duration);

                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        //When we press button the recording stops and saves the file

        B_StopButton = findViewById(R.id.stop_button);
        B_StopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                x.stopRecord();
                try {
                    x.saveWaveFiles("5alsana");
                    System.out.println("5alsana");
                } catch (OutOfMemoryError | IOException e) {
                    e.printStackTrace();
                }
            }
        });


        /*

        try {
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
         */

        //The code below Enables python
        /*
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        PyObject pyf = py.getModule("test");
        PyObject obj = pyf.callAttr("func");

        TextView Hello;
        ImageView specto;

        Hello = findViewById(R.id.welcometext);
        Hello.setText(obj.toString());
        */

    }
}
