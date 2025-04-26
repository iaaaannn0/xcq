package com.xcq.ui.renderer;

import com.xcq.ui.model.ContactTreeModel;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jxmpp.jid.Jid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ContactTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Logger logger = LoggerFactory.getLogger(ContactTreeCellRenderer.class);
    private static final Color BLINK_COLOR = Color.RED;
    private final Map<Jid, Presence.Mode> presenceMap = new HashMap<>();
    private final Map<Jid, String> nicknameMap = new HashMap<>();
    private final Map<String, Color> jidColorMap = new HashMap<>();
    private final Map<String, Boolean> blinkingJids = new HashMap<>();
    private final ContactTreeModel model;
    private final Set<String> blinkingContacts;
    private final Timer blinkTimer;
    private boolean isBlinking = false;
    private static final int BLINK_INTERVAL = 500; // 闪烁间隔（毫秒）

    public ContactTreeCellRenderer(ContactTreeModel model) {
        this.model = model;
        this.blinkingContacts = new HashSet<>();
        this.blinkTimer = new Timer(true);
        startBlinkTimer();
    }

    private void startBlinkTimer() {
        blinkTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                isBlinking = !isBlinking;
                SwingUtilities.invokeLater(() -> {
                    if (!blinkingContacts.isEmpty()) {
                        model.reload();
                    }
                });
            }
        }, 0, BLINK_INTERVAL);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof RosterEntry) {
                RosterEntry entry = (RosterEntry) userObject;
                Jid jid = entry.getJid();
                String jidStr = jid.toString();
                
                // 设置昵称
                String nickname = nicknameMap.get(jid);
                String displayName = nickname != null ? nickname : entry.getName();
                if (displayName == null) {
                    displayName = jidStr;
                }
                
                // 设置状态图标
                Presence.Mode mode = presenceMap.get(jid);
                String statusIcon;
                if (mode == null) {
                    statusIcon = "⚫"; // 离线
                } else {
                    switch (mode) {
                        case available:
                            statusIcon = "🟢"; // 在线
                            break;
                        case away:
                        case xa:
                            statusIcon = "🟡"; // 离开
                            break;
                        case dnd:
                            statusIcon = "🔴"; // 忙碌
                            break;
                        default:
                            statusIcon = "⚫"; // 离线
                    }
                }
                
                setText(statusIcon + " " + displayName);
                
                // 设置颜色
                if (blinkingContacts.contains(jidStr) && isBlinking) {
                    setForeground(BLINK_COLOR);
                } else {
                    Color color = jidColorMap.getOrDefault(jidStr, Color.BLACK);
                    setForeground(selected ? getTextSelectionColor() : color);
                }
            } else if (userObject instanceof RosterGroup) {
                RosterGroup group = (RosterGroup) userObject;
                setText("👥 " + group.getName() + " (" + group.getEntries().size() + ")");
            }
        }

        return this;
    }

    public void updatePresence(Jid jid, Presence.Mode mode) {
        presenceMap.put(jid, mode);
    }

    public void updateNickname(Jid jid, String nickname) {
        nicknameMap.put(jid, nickname);
    }

    public void setJidColor(String jid, Color color) {
        jidColorMap.put(jid, color);
    }

    public void startBlinking(String jid) {
        if (!blinkingContacts.contains(jid)) {
            blinkingContacts.add(jid);
            logger.debug("Started blinking for contact: {}", jid);
        }
    }

    public void stopBlinking(String jid) {
        if (blinkingContacts.remove(jid)) {
            logger.debug("Stopped blinking for contact: {}", jid);
        }
    }

    public void stopAllBlinking() {
        blinkingContacts.clear();
        blinkTimer.cancel();
    }

    public void cleanup() {
        blinkTimer.cancel();
    }
} 