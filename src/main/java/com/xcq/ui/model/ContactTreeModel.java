package com.xcq.ui.model;

import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.BareJid;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.Timer;
import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

public class ContactTreeModel extends DefaultTreeModel {
    private DefaultMutableTreeNode rootNode;
    private DefaultMutableTreeNode noGroupNode;
    private Map<String, DefaultMutableTreeNode> groupNodes;
    private Map<String, Timer> blinkTimers;
    private Map<String, Boolean> blinkStates;
    private JTree tree;

    public ContactTreeModel() {
        super(new DefaultMutableTreeNode("联系人"));
        rootNode = (DefaultMutableTreeNode) getRoot();
        noGroupNode = new DefaultMutableTreeNode("未分组");
        groupNodes = new HashMap<>();
        blinkTimers = new HashMap<>();
        blinkStates = new HashMap<>();
        rootNode.add(noGroupNode);
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }

    public JTree getTree() {
        return tree;
    }

    public void updateContacts(Iterable<RosterGroup> groups, Iterable<RosterEntry> ungroupedEntries) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        root.removeAllChildren();

        // 添加分组
        for (RosterGroup group : groups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            root.add(groupNode);

            // 添加分组中的联系人
            for (RosterEntry entry : group.getEntries()) {
                groupNode.add(new DefaultMutableTreeNode(entry));
            }
        }

        // 添加未分组的联系人
        DefaultMutableTreeNode ungroupedNode = new DefaultMutableTreeNode("未分组");
        boolean hasUngrouped = false;
        for (RosterEntry entry : ungroupedEntries) {
            ungroupedNode.add(new DefaultMutableTreeNode(entry));
            hasUngrouped = true;
        }
        if (hasUngrouped) {
            root.add(ungroupedNode);
        }

        // 按字母顺序排序节点
        sortNodes(root);
        
        // 通知树模型数据已更新
        reload();
    }

    public void updateFilteredContacts(Iterable<RosterGroup> groups, Iterable<RosterEntry> ungroupedEntries, String filter) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        root.removeAllChildren();

        // 添加符合过滤条件的分组联系人
        for (RosterGroup group : groups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            List<RosterEntry> filteredEntries = new ArrayList<>();
            
            // 过滤分组中的联系人
            for (RosterEntry entry : group.getEntries()) {
                if (matchesFilter(entry, filter)) {
                    filteredEntries.add(entry);
                }
            }
            
            // 只有当分组中有符合条件的联系人时才添加分组
            if (!filteredEntries.isEmpty()) {
                root.add(groupNode);
                for (RosterEntry entry : filteredEntries) {
                    groupNode.add(new DefaultMutableTreeNode(entry));
                }
            }
        }

        // 添加符合过滤条件的未分组联系人
        DefaultMutableTreeNode ungroupedNode = new DefaultMutableTreeNode("未分组");
        boolean hasUngrouped = false;
        for (RosterEntry entry : ungroupedEntries) {
            if (matchesFilter(entry, filter)) {
                ungroupedNode.add(new DefaultMutableTreeNode(entry));
                hasUngrouped = true;
            }
        }
        if (hasUngrouped) {
            root.add(ungroupedNode);
        }

        // 按字母顺序排序节点
        sortNodes(root);
        
        // 通知树模型数据已更新
        reload();
    }

    private boolean matchesFilter(RosterEntry entry, String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        String name = entry.getName();
        if (name == null) {
            name = entry.getJid().toString();
        }
        return name.toLowerCase().contains(filter.toLowerCase());
    }

    private void sortNodes(DefaultMutableTreeNode node) {
        if (node.getChildCount() > 0) {
            // 对子节点进行排序
            Vector<TreeNode> children = new Vector<>();
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                children.add((TreeNode) e.nextElement());
            }

            Collections.sort(children, new Comparator<TreeNode>() {
                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                    String s1 = o1.toString();
                    String s2 = o2.toString();
                    return s1.compareTo(s2);
                }
            });

            // 清除现有子节点并按排序后的顺序重新添加
            node.removeAllChildren();
            for (TreeNode child : children) {
                node.add((DefaultMutableTreeNode) child);
                // 递归排序子节点
                sortNodes((DefaultMutableTreeNode) child);
            }
        }
    }

    public void startBlink(String jid) {
        if (blinkTimers.containsKey(jid)) {
            return;
        }

        blinkStates.put(jid, true);
        Timer timer = new Timer(500, e -> {
            blinkStates.put(jid, !blinkStates.get(jid));
            reload(); // 重新加载树以更新显示
        });
        timer.start();
        blinkTimers.put(jid, timer);
    }

    public void stopBlink(String jid) {
        Timer timer = blinkTimers.remove(jid);
        if (timer != null) {
            timer.stop();
        }
        blinkStates.remove(jid);
        reload();
    }

    public boolean isBlinking(String jid) {
        return blinkStates.containsKey(jid) && blinkStates.get(jid);
    }

    public Contact findContact(String jid) {
        // 遍历所有节点查找联系人
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) root.getChildAt(i);
            for (int j = 0; j < groupNode.getChildCount(); j++) {
                DefaultMutableTreeNode contactNode = (DefaultMutableTreeNode) groupNode.getChildAt(j);
                Object userObject = contactNode.getUserObject();
                if (userObject instanceof Contact) {
                    Contact contact = (Contact) userObject;
                    if (contact.getJid().equals(jid)) {
                        return contact;
                    }
                }
            }
        }
        return null;
    }

    public static class Contact {
        private final String jid;
        private final String name;
        private final String group;

        public Contact(String jid, String name, String group) {
            this.jid = jid;
            this.name = name != null ? name : jid;
            this.group = group;
        }

        public String getJid() {
            return jid;
        }

        public String getName() {
            return name;
        }

        public String getGroup() {
            return group;
        }

        @Override
        public String toString() {
            return name;
        }
    }
} 