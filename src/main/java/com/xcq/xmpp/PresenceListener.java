package com.xcq.xmpp;

import org.jivesoftware.smack.packet.Presence;

public interface PresenceListener {
    void onPresenceChange(Presence presence);
} 