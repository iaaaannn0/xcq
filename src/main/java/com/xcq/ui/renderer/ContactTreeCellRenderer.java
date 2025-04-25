package com.xcq.ui.renderer;

import com.xcq.ui.model.ContactTreeModel;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jxmpp.jid.Jid;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ContactTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Color BLINK_COLOR = new Color(255, 0, 0);
    private final Map<Jid, Presence.Mode> presenceMap = new HashMap<>();
    private final Map<Jid, String> nicknameMap = new HashMap<>();
    private final Map<String, Color> jidColorMap = new HashMap<>();
    private final Map<String, Boolean> blinkingJids = new HashMap<>();
    private final ContactTreeModel treeModel;
    private Timer blinkTimer;
    private boolean blinkState = false;

    public ContactTreeCellRenderer(ContactTreeModel treeModel) {
        this.treeModel = treeModel;
        
        // 创建闪烁定时器
        blinkTimer = new Timer(500, e -> {
            blinkState = !blinkState;
            // 通知树重绘
            if (treeModel != null && treeModel.getTree() != null) {
                treeModel.getTree().repaint();
            }
        });
        blinkTimer.start();
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
                if (blinkingJids.getOrDefault(jidStr, false) && blinkState) {
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
        blinkingJids.put(jid, true);
        if (!blinkTimer.isRunning()) {
            blinkTimer.start();
        }
    }

    public void stopBlinking(String jid) {
        blinkingJids.remove(jid);
        if (blinkingJids.isEmpty()) {
            blinkTimer.stop();
        }
    }

    public void stopAllBlinking() {
        blinkingJids.clear();
        blinkTimer.stop();
    }
} 