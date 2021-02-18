package org.helllabs.android.xmp;

import org.helllabs.android.xmp.util.ModInfo;

public final class Xmp {

    // Return codes
    public static final int XMP_END = 1;            // End of module reached

    // Sample format flags
    public static final int FORMAT_MONO = 1 << 2;

    // player parameters
    public static final int PLAYER_AMP = 0;            // Amplification factor
    public static final int PLAYER_MIX = 1;            // Stereo mixing
    public static final int PLAYER_INTERP = 2;        // Interpolation type
    public static final int PLAYER_DSP = 3;            // DSP effect flags
    public static final int PLAYER_CFLAGS = 5;      // Current module flags
    public static final int PLAYER_VOLUME = 7;        // Player volume (for audio focus duck)
    public static final int PLAYER_DEFPAN = 10;        // Default pan separation

    // Interpolation types
    public static final int INTERP_NEAREST = 0;        // Nearest neighbor
    public static final int INTERP_LINEAR = 1;        // Linear (default)
    public static final int INTERP_SPLINE = 2;        // Cubic spline

    // Player flags
    public static final int FLAGS_A500 = 1 << 3;

    // DSP effect types
    public static final int DSP_LOWPASS = 1 << 0;    // Lowpass filter effect

    // Limits
    public static final int MAX_CHANNELS = 64;        // Max number of channels in module


    private Xmp() {

    }

    public static native boolean init(int rate, int ms);

    public static native void deinit();

    public static native boolean testModule(String name, ModInfo info);

    public static native int loadModule(String name);

    public static native int releaseModule();

    public static native int startPlayer(int rate);

    public static native int endPlayer();

    public static native int playAudio();

    public static native int dropAudio();

    public static native boolean stopAudio();

    public static native boolean restartAudio();

    public static native boolean hasFreeBuffer();

    public static native int fillBuffer(boolean loop);

    public static native int nextPosition();

    public static native int prevPosition();

    public static native int setPosition(int num);

    public static native int stopModule();

    public static native int restartModule();

    public static native int seek(int time);

    public static native int time();

    public static native int mute(int chn, int status);

    public static native void getInfo(int[] values);

    public static native int getPlayer(int parm);

    public static native void setPlayer(int parm, int val);

    public static native int getLoopCount();

    public static native void getModVars(int[] vars);

    public static native String getVersion();

    public static native String getModName();

    public static native String getModType();

    public static native String getComment();

    public static native String[] getFormats();

    public static native String[] getInstruments();

    public static native void getChannelData(int[] volumes, int[] finalvols, int[] pans, int[] instruments, int[] keys, int[] periods);

    public static native void getPatternRow(int pat, int row, byte[] rowNotes, byte[] rowInstruments);

    public static native void getSampleData(boolean trigger, int ins, int key, int period, int chn, int width, byte[] buffer);

    public static native boolean setSequence(int seq);

    public static native void getSeqVars(int[] vars);

    public static native int getVolume();

    public static native int setVolume(int vol);

    static {
        System.loadLibrary("xmp-jni");
    }
}
