package com.example.finalsonar;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class AudioManager{

    public int number_of_recording = 1;
    private Context ctx;
    private static String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "FinalSonar" + File.separator;

    public int SAMPLE_RATE = 44100;
    private static final int minSize = 4*4096;
    public double freq = 19000;
    public double sampleRate = 44100;
    private final GenerateSignal Gensignal;
    public static boolean isrecording = true;

    private DataOutputStream dosSend;
    private DataOutputStream dosRec;
    private File tempFileRec;

    public AudioManager(Context ctx){
        this.ctx = ctx;
        Gensignal = new GenerateSignal(freq,sampleRate);
        new File(fileDir).mkdirs();
    }

    //public final AudioTrack at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);


    public void PlayRecordAudio() throws FileNotFoundException {

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

        tempFileRec = new File(ctx.getExternalCacheDir().getAbsolutePath() + "/temp_rec.raw");
        File tempFileSend = new File(ctx.getExternalCacheDir().getAbsolutePath() + "/temp_send.raw");

        if(tempFileRec.exists())
            tempFileRec.delete();

        if(tempFileSend.exists())
            tempFileSend.delete();

        dosRec = new DataOutputStream(new FileOutputStream(tempFileRec));
        dosSend = new DataOutputStream(new FileOutputStream(tempFileSend));


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

                    writeByteBufferToStream(buffer, dosRec);

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
                try {
                    dosRec.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        Thread playThread = new Thread(() -> {

            player.play();
            while (isrecording) {
                player.write(audio, 0, audio.length);
                writeByteBufferToStream(audio, dosSend);
            }
            player.stop();
            player.release();
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

    public void saveWaveFiles(String waveFileName) throws IOException {

        saveWave(waveFileName + String.valueOf(number_of_recording) , tempFileRec);
        number_of_recording++;  //increment number of recordings to stop overwriting
        //saveWave(waveFileName + "_send", tempFileSend);

    }


    private void saveWave(String fileName, File fileToSave) throws IOException {
        int dataLength = (int) fileToSave.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(fileToSave));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            File file = new File(fileDir + fileName + ".wav");
            output = new DataOutputStream(new FileOutputStream(file, false));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + dataLength); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, dataLength); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());

            MediaScannerConnection.scanFile(ctx,
                    new String[]{file.toString()}, null,
                    (path, uri) -> {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    });

        }

        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}

