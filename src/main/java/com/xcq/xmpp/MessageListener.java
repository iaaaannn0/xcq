package com.xcq.xmpp;

import org.jivesoftware.smack.packet.Message;

public interface MessageListener {
    void onMessage(Message message);
} 