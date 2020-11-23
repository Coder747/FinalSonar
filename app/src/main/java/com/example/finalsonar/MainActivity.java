package com.example.finalsonar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioAttributes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
//import AudioManager.java;

public class MainActivity extends AppCompatActivity {

    public int SAMPLE_RATE = 44100;
    private static final int minSize = 4*4096;
    public double freq = 19000;
    public double sampleRate = 44100;
    public double amplitude = 1.0;
    public double length = 0.5;
    //public final AudioTrack at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);

    public final AudioTrack player = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
            .setBufferSizeInBytes(minSize)
            .build();

    public byte[] generateAudio() {

        float[] buffer = new float[(int) (length * sampleRate)];


        for (int sample = 0; sample < buffer.length; sample++) {
            double time = sample / sampleRate;
            double angle = freq * 2.0 * Math.PI * time;
            //make sure we got precise calculations
            angle %= 2.0*Math.PI;
            buffer[sample] = (float) (amplitude * Math.sin((float)angle));
        }

        final byte[] byteBuffer = new byte[buffer.length * 2];
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i++) {
            final int x = (int) (buffer[bufferIndex++] * 32767.0);
            byteBuffer[i] = (byte) x;
            i++;
            byteBuffer[i] = (byte) (x >>> 8);
        }

        //counter++;
        return byteBuffer;
    }

    private void writeByteBufferToStream(byte[] buffer, DataOutputStream dos){

        try{
            ByteBuffer bytes = ByteBuffer.allocate(buffer.length);
            for(int i=0; i < buffer.length; i+=2){
                byte byte1 = buffer[i];
                byte byte2 = buffer[i+1];
                short newshort = (short) ((byte2 << 8) + (byte1&0xFF));
                bytes.putShort(newshort);
            }
            dos.write(bytes.array());
            dos.flush();

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final byte[] audio = generateAudio();

        Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                player.play();
                while(true){
                    player.write(audio, 0, audio.length);
                }
                //player.stop();
                //player.release();
            }
        });

        playThread.start();

    }
}
