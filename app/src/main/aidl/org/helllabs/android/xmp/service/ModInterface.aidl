package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.service.PlayerCallback;


interface ModInterface {
	boolean deleteFile();
	boolean getAllSequences();
	boolean getLoop();
	boolean hasComment();
	boolean isPaused();
	boolean setSequence(int seq);
	boolean toggleAllSequences();
	boolean toggleLoop();
	int mute(int chn, int status);
	int time();
	String getFileName();
	String getModName();
	String getModType();
	String[] getInstruments();
	void add(in List<String> fileList);
	void allowRelease();
	void currentPlayTime(int time);
	void getChannelData(out int[] volumes, out int[] finalvols, out int[] pans, out int[] instruments, out int[] keys, out int[] periods);
	void getInfo(out int[] values);
	void getModVars(out int[] vars);
	void getPatternRow(int pat, int row, out byte[] rowNotes, out byte[] rowInstruments);
	void getSampleData(boolean trigger, int ins, int key, int period, int chn, int width, out byte[] buffer);
	void getSeqVars(out int[] vars);
	void nextSong();
	void pause();
	void play(in List<String> fileList, int start, boolean shuffle, boolean loopList, boolean keepFirst);
	void prevSong();
	void seek(in int seconds);
	void stop();
	
	void registerCallback(PlayerCallback cb);
	void unregisterCallback(PlayerCallback cb);
}