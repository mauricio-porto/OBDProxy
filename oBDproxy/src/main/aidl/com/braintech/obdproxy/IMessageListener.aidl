package com.braintech.obdproxy;

interface IMessageListener {
    void processMessage(in String from, in String msg);
}