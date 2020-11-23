package com.example.finalsonar;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;


public class AudioManager{

    public int SAMPLE_RATE = 44100;
    private static final int minSize = 4*4096;
    public double freq = 19000;
    public double sampleRate = 44100;
    public double amplitude = 1.0;
    public double length = 0.5;
    private GenerateSignal Gensignal;
    private boolean isrecording = true;

    public AudioManager() {
        Gensignal = new GenerateSignal(freq,sampleRate);
    }

    //public final AudioTrack at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);


    public void PlayAudio() {

        final AudioTrack player = new AudioTrack.Builder()
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

        //final AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10*minSize);

        final AudioRecord recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(10*minSize)
                .build();


        final byte[] audio = Gensignal.generateAudio();

        Thread detectionThread = new Thread(new Runnable() {

            private int sampleCounter;
            @Override
            public void run() {

                final byte[] buffer = new byte[minSize];
                while(isrecording){
                    int samplesRead = recorder.read(buffer, 0, minSize);
                    if(samplesRead != minSize) {
                        Log.e("AUDIO_MANAGER", "Samples read not equal minSize (" + samplesRead + "). Might be loosing data!");
                    }

                    //size of bufferDouble is buffer.length/2
                    final double[] bufferDouble = convertToDouble(buffer, samplesRead);

//                    if(sampleCounter >= 44100) {
//                        if(featureDetector != null) {
//                            featureDetector.checkForFeatures(bufferDouble, true);
//                        }
//                    }
//                    //omit first second
//                    sampleCounter+=samplesRead;
                }

                recorder.stop();
                recorder.release();
            }
        });


        Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                player.play();
                while (isrecording) {
                    player.write(audio, 0, audio.length);
                }
                player.stop();
                player.release();
            }
        });

        recorder.startRecording();
        isrecording = true;
        detectionThread.start();

        playThread.start();
    }

    public void stopRecord() {
        isrecording = false;
    }

    private double[] convertToDouble(byte[] buffer, int bytesRead) {

        //from http://stackoverflow.com/questions/5774104/android-audio-fft-to-retrieve-specific-frequency-magnitude-using-audiorecord
        double[] bufferDouble = new double[buffer.length/2];
        final int bytesPerSample = 2; // As it is 16bit PCM
        final double amplification = 1.0; // choose a number as you like
        for (int index = 0, floatIndex = 0; index < bytesRead - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = buffer[index + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            bufferDouble[floatIndex] = sample32;
        }

        return bufferDouble;
    }
}

