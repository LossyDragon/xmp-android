package org.helllabs.android.xmp.service;

interface PlayerCallback {
	void endModCallback();
	void endPlayCallback(int result);
	void newModCallback();
	void newSequenceCallback();
	void pauseCallback();
}