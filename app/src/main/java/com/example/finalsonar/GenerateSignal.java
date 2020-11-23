package com.example.finalsonar;


public class GenerateSignal {

    private double freq;
    private double length;
    private double amplitude;
    private double sampleRate;

    public GenerateSignal(double freq, double sampleRate) {
        this.freq = freq;
        this.length = 0.5;
        this.amplitude = 1.0;
        this.sampleRate = sampleRate;
    }

    public byte[] generateAudio() {

        float[] buffer = new float[(int) (length * sampleRate)];

        for (int sample = 0; sample < buffer.length; sample++) {
            double time = sample / sampleRate;
            double angle = freq * 2.0 * Math.PI * time;
            //make sure we got precise calculations
            angle %= 2.0 * Math.PI;
            buffer[sample] = (float) (amplitude * Math.sin((float) angle));
        }

        final byte[] byteBuffer = new byte[buffer.length * 2];
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i++) {
            final int x = (int) (buffer[bufferIndex++] * 32767.0);
            byteBuffer[i] = (byte) x;
            i++;
            byteBuffer[i] = (byte) (x >>> 8);
        }

        return byteBuffer;
    }

}
