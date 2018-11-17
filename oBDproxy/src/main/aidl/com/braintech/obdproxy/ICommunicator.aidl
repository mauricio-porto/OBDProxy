package com.braintech.obdproxy;

import com.braintech.obdproxy.IMessageListener;
import com.braintech.obdproxy.IRosterListener;

interface ICommunicator {
	void start();
	void stop();
	boolean sendMessage(in String to, in String msg);
	void startMessageListener(in IMessageListener listener);
    void stopMessageListener();
	void startRosterListener(in IRosterListener listener);
	void stopRosterListener();
}