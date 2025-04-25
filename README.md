# XCQ 即时通讯客户端

XCQ 是一个基于 XMPP 协议的跨平台即时通讯客户端，灵感来源于经典的 ICQ 通讯软件。它提供了简洁的用户界面和丰富的即时通讯功能，支持多平台运行。

## 功能特性

- 🔐 基于 XMPP 协议的安全通信
- 👥 完整的联系人管理系统
  - 分组管理
  - 在线状态显示
  - 联系人搜索（支持拼音、昵称、JID）
  - VCard 个人资料查看
- 💬 强大的即时通讯功能
  - 文本消息收发
  - 表情符号支持
  - 图片消息
  - 文件传输
  - 消息历史记录
- 🔔 智能的消息提醒
  - 声音提醒
  - 窗口闪烁
  - 任务栏通知
  - 联系人头像闪烁
- 🎨 现代化的用户界面
  - Swing 原生界面
  - 自定义渲染器
  - 主题支持
- 💾 本地数据存储
  - SQLite 数据库
  - 聊天记录管理
  - 配置持久化

## 技术架构

### 核心组件

1. **通信层**
   - XMPP 协议实现 (Smack 库)
   - 消息收发管理
   - 连接状态维护

2. **数据层**
   - SQLite 数据库
   - 消息存储
   - 配置管理

3. **界面层**
   - Swing UI 组件
   - 自定义渲染器
   - 事件处理系统

### 主要类说明

- `XMPPClient`: XMPP 连接和消息处理核心类
- `ContactWindow`: 主窗口，联系人列表管理
- `ChatWindow`: 聊天窗口，消息收发界面
- `ContactTreeModel`: 联系人树模型
- `ChatDatabase`: 消息存储管理
- `Configuration`: 配置管理

## 开发环境搭建

### 系统要求

- JDK 11 或更高版本
- Maven 3.6 或更高版本
- SQLite 3.x

### 构建步骤

1. 克隆代码仓库：
```bash
git clone https://github.com/yourusername/xcq.git
cd xcq
```

2. 安装依赖：
```bash
mvn clean install
```

3. 运行应用：
```bash
mvn clean package
java -jar target/xcq-client-1.0-SNAPSHOT.jar
```

## 项目结构

```
xcq/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── xcq/
│   │   │           ├── core/        # 核心功能
│   │   │           ├── db/          # 数据库操作
│   │   │           ├── ui/          # 用户界面
│   │   │           ├── util/        # 工具类
│   │   │           └── xmpp/        # XMPP 实现
│   │   └── resources/              # 资源文件
│   └── test/                       # 测试代码
├── pom.xml                         # Maven 配置
└── README.md                       # 项目文档
```

## 实现原理

### 消息处理流程

1. **消息发送**
   - 用户输入消息
   - 消息预处理（去除空格等）
   - XMPP 协议打包
   - 发送到服务器
   - 本地存储

2. **消息接收**
   - XMPP 服务器推送
   - 消息解析
   - 本地存储
   - UI 更新
   - 通知提醒

### 联系人管理

1. **在线状态**
   - 定期心跳检测
   - Presence 状态更新
   - UI 实时刷新

2. **分组管理**
   - 树形结构设计
   - 动态更新
   - 拖拽支持

### 数据存储

1. **消息存储**
   - SQLite 数据库
   - 消息表设计
   - 索引优化

2. **配置管理**
   - JSON 格式
   - 文件持久化
   - 实时同步

## 开发指南

### 添加新功能

1. **创建新的消息类型**
   - 扩展 `Message` 类
   - 实现序列化接口
   - 添加处理器

2. **自定义 UI 组件**
   - 继承 Swing 组件
   - 实现自定义渲染
   - 注册事件监听

### 代码规范

1. **命名规范**
   - 类名：大驼峰
   - 方法名：小驼峰
   - 常量：全大写下划线

2. **注释规范**
   - 类注释：功能描述
   - 方法注释：参数说明
   - 关键代码注释

### 测试规范

1. **单元测试**
   - JUnit 测试框架
   - 测试用例设计
   - 覆盖率要求

2. **集成测试**
   - 模拟 XMPP 服务器
   - 场景测试
   - 性能测试

## 部署指南

### 打包部署

1. **生成可执行 JAR**
```bash
mvn clean package
```

2. **运行应用**
```bash
java -jar target/xcq-client-1.0-SNAPSHOT.jar
```

### 配置说明

1. **系统配置**
   - `config.json`: 全局配置
   - `logging.properties`: 日志配置

2. **数据存储**
   - 数据库文件：`data/chat.db`
   - 配置文件：`data/config.json`

## 常见问题

### 1. 连接问题

#### 1.1 无法连接到服务器
- **现象**：启动时提示"连接失败"或"无法连接到服务器"
- **可能原因**：
  - 网络连接不稳定
  - 服务器地址或端口配置错误
  - 防火墙阻止连接
- **解决方案**：
  1. 检查网络连接是否正常
  2. 验证服务器配置信息：
     ```json
     {
       "xmpp": {
         "host": "your.server.com",
         "port": 5222,
         "domain": "your.server.com"
       }
     }
     ```
  3. 检查防火墙设置，确保 5222 端口（默认XMPP端口）开放
  4. 尝试使用 telnet 测试连接：
     ```bash
     telnet your.server.com 5222
     ```

#### 1.2 登录认证失败
- **现象**：提示"认证失败"或"用户名密码错误"
- **解决方案**：
  1. 确认用户名格式正确（user@domain.com）
  2. 重置密码
  3. 检查服务器日志中的具体错误信息

#### 1.3 连接不稳定
- **现象**：经常断线或显示"重连中"
- **解决方案**：
  1. 调整心跳包间隔：
     ```json
     {
       "connection": {
         "pingInterval": 30,
         "reconnectDelay": 5
       }
     }
     ```
  2. 检查网络质量
  3. 更新到最新版本的客户端

### 2. 消息问题

#### 2.1 消息发送失败
- **现象**：消息显示红色感叹号或发送失败
- **解决方案**：
  1. 检查网络连接
  2. 确认接收方在线状态
  3. 查看错误日志：`logs/xcq.log`
  4. 尝试重新发送消息

#### 2.2 消息同步异常
- **现象**：
  - 消息历史记录不完整
  - 重复消息
  - 消息顺序错乱
- **解决方案**：
  1. 检查数据库完整性：
     ```bash
     sqlite3 data/chat.db
     .tables
     .schema messages
     ```
  2. 修复数据库索引：
     ```sql
     REINDEX idx_timestamp;
     REINDEX idx_participants;
     ```
  3. 清理并重建缓存：
     ```bash
     rm -rf cache/*
     ```

#### 2.3 图片和文件传输失败
- **现象**：无法发送或接收文件
- **解决方案**：
  1. 检查文件大小是否超过限制（默认20MB）
  2. 确认存储空间充足
  3. 验证文件类型是否支持
  4. 调整文件传输配置：
     ```json
     {
       "fileTransfer": {
         "maxSize": 20971520,
         "allowedTypes": ["jpg", "png", "pdf", "doc", "docx"]
       }
     }
     ```

### 3. 界面问题

#### 3.1 界面显示异常
- **现象**：
  - 字体显示乱码
  - 表情符号无法显示
  - 窗口大小异常
- **解决方案**：
  1. 检查系统字体设置
  2. 更新 Java 运行环境
  3. 重置界面配置：
     ```bash
     rm data/ui_config.json
     ```
  4. 调整 DPI 设置：
     ```json
     {
       "ui": {
         "scaling": 1.0,
         "font": "Microsoft YaHei",
         "fontSize": 12
       }
     }
     ```

#### 3.2 通知提醒失效
- **现象**：收到新消息时没有提醒
- **解决方案**：
  1. 检查系统通知权限
  2. 确认声音文件存在：`resources/sounds/message.wav`
  3. 验证通知设置：
     ```json
     {
       "notifications": {
         "sound": true,
         "flash": true,
         "desktop": true
       }
     }
     ```

### 4. 性能问题

#### 4.1 启动缓慢
- **现象**：程序启动时间超过5秒
- **解决方案**：
  1. 清理聊天记录：
     ```sql
     DELETE FROM messages WHERE timestamp < strftime('%s', 'now', '-30 days') * 1000;
     ```
  2. 优化数据库：
     ```sql
     VACUUM;
     ```
  3. 调整 JVM 参数：
     ```bash
     java -Xmx512m -jar xcq-client.jar
     ```

#### 4.2 内存占用过高
- **现象**：内存使用超过500MB
- **解决方案**：
  1. 限制消息历史记录缓存：
     ```json
     {
       "chat": {
         "maxHistoryMessages": 1000,
         "messageCleanupDays": 30
       }
     }
     ```
  2. 定期清理缓存
  3. 调整内存限制：
     ```bash
     java -Xmx256m -Xms128m -jar xcq-client.jar
     ```

### 5. 其他问题

#### 5.1 日志查看
- 日志位置：`logs/xcq.log`
- 查看最新日志：
  ```bash
  tail -f logs/xcq.log
  ```
- 日志级别调整：
  ```properties
  log4j.rootLogger=INFO, FILE
  ```

#### 5.2 配置文件位置
- 主配置文件：`data/config.json`
- 用户配置：`~/.xcq/user_config.json`
- 日志配置：`config/logging.properties`

#### 5.3 数据备份
- 备份数据：
  ```bash
  cp -r data/ backup/
  ```
- 恢复数据：
  ```bash
  cp -r backup/* data/
  ```

### 6. 插件系统问题

#### 6.1 插件加载失败
- **现象**：
  - 插件无法正常加载
  - 插件功能无法使用
  - 插件冲突
- **解决方案**：
  1. 检查插件兼容性：
     ```json
     {
       "plugins": {
         "compatibility": {
           "minVersion": "1.0.0",
           "maxVersion": "2.0.0"
         }
       }
     }
     ```
  2. 启用插件调试模式：
     ```bash
     java -Dplugin.debug=true -jar xcq-client.jar
     ```
  3. 查看插件日志：
     ```bash
     tail -f logs/plugins.log
     ```

#### 6.2 插件开发
- **现象**：需要开发自定义插件
- **解决方案**：
  1. 创建插件模板：
     ```java
     public class CustomPlugin implements XCQPlugin {
         @Override
         public void onLoad() {
             // 插件加载时的初始化代码
         }
         
         @Override
         public void onMessage(Message message) {
             // 消息处理逻辑
         }
     }
     ```
  2. 配置插件元数据：
     ```json
     {
       "plugin": {
         "name": "CustomPlugin",
         "version": "1.0.0",
         "author": "Your Name",
         "description": "Plugin description"
       }
     }
     ```
  3. 构建和部署插件：
     ```bash
     mvn package
     cp target/custom-plugin.jar plugins/
     ```

### 7. 国际化问题

#### 7.1 语言设置
- **现象**：
  - 界面语言显示错误
  - 特殊字符显示为乱码
  - 时区显示不正确
- **解决方案**：
  1. 配置语言和区域设置：
     ```json
     {
       "locale": {
         "language": "zh",
         "country": "CN",
         "timezone": "Asia/Shanghai",
         "dateFormat": "yyyy-MM-dd HH:mm:ss"
       }
     }
     ```
  2. 添加语言包：
     ```bash
     cp messages_zh_CN.properties resources/i18n/
     ```
  3. 设置默认编码：
     ```bash
     java -Dfile.encoding=UTF-8 -jar xcq-client.jar
     ```

#### 7.2 多语言支持
- **现象**：需要支持新的语言
- **解决方案**：
  1. 创建新的语言资源文件：
     ```properties
     # messages_ja_JP.properties
     login.title=ログイン
     login.username=ユーザー名
     login.password=パスワード
     ```
  2. 注册语言支持：
     ```java
     ResourceBundle.getBundle("messages", new Locale("ja", "JP"));
     ```
  3. 动态切换语言：
     ```java
     public void changeLanguage(String language, String country) {
         Locale newLocale = new Locale(language, country);
         Locale.setDefault(newLocale);
         // 重新加载UI组件
         refreshUI();
     }
     ```

### 8. 音视频通话问题

#### 8.1 音频问题
- **现象**：
  - 没有声音
  - 声音质量差
  - 回音
- **解决方案**：
  1. 配置音频设置：
     ```json
     {
       "audio": {
         "inputDevice": "default",
         "outputDevice": "default",
         "sampleRate": 44100,
         "echoCancellation": true,
         "noiseSuppression": true
       }
     }
     ```
  2. 检查音频设备：
     ```java
     public void listAudioDevices() {
         Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
         for (Mixer.Info info : mixerInfos) {
             System.out.println(info.getName());
         }
     }
     ```
  3. 调整音量级别：
     ```bash
     java -jar xcq-client.jar --audio-test
     ```

#### 8.2 视频问题
- **现象**：
  - 摄像头无法启动
  - 视频画面卡顿
  - 画面质量差
- **解决方案**：
  1. 配置视频参数：
     ```json
     {
       "video": {
         "camera": "0",
         "resolution": "720p",
         "frameRate": 30,
         "bitrate": 1500000
       }
     }
     ```
  2. 视频设备测试：
     ```java
     public void testVideoDevice() {
         Webcam webcam = Webcam.getDefault();
         if (webcam != null) {
             webcam.open();
             // 进行测试
             webcam.close();
         }
     }
     ```
  3. 性能优化：
     ```json
     {
       "videoOptimization": {
         "adaptiveBitrate": true,
         "qualityPreference": "BALANCED",
         "hardwareAcceleration": true
       }
     }
     ```

### 9. 系统集成问题

#### 9.1 第三方系统集成
- **现象**：需要与其他系统集成
- **解决方案**：
  1. 配置 API 接入：
     ```json
     {
       "api": {
         "endpoint": "https://api.example.com",
         "version": "v1",
         "auth": {
           "type": "Bearer",
           "token": "your_api_token"
         }
       }
     }
     ```
  2. 实现数据同步：
     ```java
     public void syncWithExternalSystem() {
         // 定期同步数据
         ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
         executor.scheduleAtFixedRate(this::doSync, 0, 1, TimeUnit.HOURS);
     }
     ```
  3. 错误处理：
     ```java
     public void handleIntegrationError(Exception e) {
         logger.error("集成错误: " + e.getMessage());
         notifyAdmin("系统集成异常", e.toString());
     }
     ```

#### 9.2 数据导入导出
- **现象**：需要批量导入或导出数据
- **解决方案**：
  1. 导出数据格式：
     ```json
     {
       "export": {
         "format": "JSON",
         "compression": true,
         "encryption": {
           "enabled": true,
           "algorithm": "AES"
         }
       }
     }
     ```
  2. 批量导入：
     ```java
     public void importData(File file) {
         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
             String line;
             while ((line = reader.readLine()) != null) {
                 processImportLine(line);
             }
         }
     }
     ```
  3. 进度监控：
     ```java
     public void monitorProgress(long total, long current) {
         int progress = (int) ((current * 100) / total);
         updateProgressBar(progress);
     }
     ```

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交变更
4. 发起 Pull Request


## 核心实现代码

### 1. XMPP 连接管理

```java
public class XMPPClient {
    private XMPPTCPConnection connection;
    private ChatManager chatManager;
    
    public void connect(String host, int port, String username, String password) {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
            .setHost(host)
            .setPort(port)
            .setUsernameAndPassword(username, password)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
            .build();
            
        connection = new XMPPTCPConnection(config);
        connection.connect();
        connection.login();
        
        chatManager = ChatManager.getInstanceFor(connection);
        setupMessageListeners();
    }
    
    private void setupMessageListeners() {
        chatManager.addIncomingListener((from, message, chat) -> {
            // 处理接收到的消息
            String body = message.getBody();
            String senderJid = from.asBareJid().toString();
            ChatDatabase.getInstance().saveMessage(senderJid, connection.getUser().asBareJid().toString(), body, false);
            notifyMessageReceived(senderJid, body);
        });
    }
    
    public void sendMessage(String recipientJid, String messageBody) {
        try {
            EntityBareJid jid = JidCreate.entityBareFrom(recipientJid);
            Chat chat = chatManager.chatWith(jid);
            chat.send(messageBody);
            
            // 保存发送的消息
            ChatDatabase.getInstance().saveMessage(
                connection.getUser().asBareJid().toString(),
                recipientJid,
                messageBody,
                true
            );
        } catch (Exception e) {
            logger.error("发送消息失败", e);
        }
    }
}
```

### 2. 联系人管理

```java
public class ContactWindow extends JFrame {
    private JTree contactTree;
    private ContactTreeModel treeModel;
    
    public void initializeContactList() {
        Roster roster = Roster.getInstanceFor(xmppClient.getConnection());
        roster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                refreshContacts();
            }
            
            @Override
            public void presenceChanged(Presence presence) {
                String jid = presence.getFrom().asBareJid().toString();
                updateContactPresence(jid, presence);
            }
            // ... 其他监听器方法
        });
    }
    
    private void refreshContacts() {
        SwingUtilities.invokeLater(() -> {
            treeModel.reload();
            contactTree.repaint();
        });
    }
    
    private void updateContactPresence(String jid, Presence presence) {
        SwingUtilities.invokeLater(() -> {
            treeModel.updatePresence(jid, presence);
            contactTree.repaint();
        });
    }
}
```

### 3. 消息存储

```java
public class ChatDatabase {
    private static final String DB_URL = "jdbc:sqlite:data/chat.db";
    
    public void saveMessage(String senderJid, String receiverJid, String message, boolean isLocal) {
        String sql = "INSERT INTO messages (sender_jid, receiver_jid, message_body, is_local, timestamp) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, senderJid);
            pstmt.setString(2, receiverJid);
            pstmt.setString(3, message);
            pstmt.setBoolean(4, isLocal);
            pstmt.setLong(5, System.currentTimeMillis());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("保存消息失败", e);
        }
    }
    
    public List<ChatMessage> getChatHistory(String user1Jid, String user2Jid) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = """
            SELECT * FROM messages 
            WHERE (sender_jid = ? AND receiver_jid = ?) 
               OR (sender_jid = ? AND receiver_jid = ?)
            ORDER BY timestamp ASC
        """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user1Jid);
            pstmt.setString(2, user2Jid);
            pstmt.setString(3, user2Jid);
            pstmt.setString(4, user1Jid);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new ChatMessage(
                    rs.getString("sender_jid"),
                    rs.getString("receiver_jid"),
                    rs.getString("message_body"),
                    rs.getBoolean("is_local"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            logger.error("获取聊天记录失败", e);
        }
        return messages;
    }
}
```

### 4. 配置管理

```java
public class Configuration {
    private static final String CONFIG_FILE = "data/config.json";
    private Map<String, Object> properties;
    private static Configuration instance;
    
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }
    
    public void loadSettings() {
        try {
            Gson gson = new Gson();
            String json = Files.readString(Paths.get(CONFIG_FILE));
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            properties = gson.fromJson(json, type);
        } catch (IOException e) {
            logger.error("加载配置失败", e);
            properties = new HashMap<>();
        }
    }
    
    public void saveSettings() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(properties);
            Files.writeString(Paths.get(CONFIG_FILE), json);
        } catch (IOException e) {
            logger.error("保存配置失败", e);
        }
    }
    
    public <T> T get(String key, T defaultValue) {
        Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(value.toString());
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(value.toString());
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(value.toString());
            }
            return (T) value;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
```

### 5. 聊天窗口

```java
public class ChatWindow extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;
    private String recipientJid;
    
    public ChatWindow(String recipientJid) {
        this.recipientJid = recipientJid;
        initializeUI();
        loadChatHistory();
    }
    
    private void initializeUI() {
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        
        // 添加表情按钮
        JButton emojiButton = new JButton("😊");
        emojiButton.addActionListener(e -> showEmojiPanel());
        
        // 布局设置
        setLayout(new BorderLayout());
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(emojiButton, BorderLayout.WEST);
        bottomPanel.add(messageField, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            XMPPClient.getInstance().sendMessage(recipientJid, message);
            appendMessage(XMPPClient.getInstance().getUsername(), message, true);
            messageField.setText("");
        }
    }
    
    private void appendMessage(String sender, String message, boolean isLocal) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            try {
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String formattedMessage = String.format("[%s] %s: %s\n", 
                    timestamp, sender, message);
                
                Style style = chatArea.addStyle("MessageStyle", null);
                StyleConstants.setForeground(style, isLocal ? Color.BLUE : Color.BLACK);
                
                doc.insertString(doc.getLength(), formattedMessage, style);
                chatArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                logger.error("添加消息到聊天区域失败", e);
            }
        });
    }
}
```

### 6. 表情面板

```java
public class EmojiPanel extends JPanel {
    private static final String[] EMOJIS = {
        "😊", "😂", "🤣", "❤️", "😍", "😒", "👍", "😭", "😘", "🙄"
    };
    
    public EmojiPanel(Consumer<String> emojiSelectCallback) {
        setLayout(new GridLayout(2, 5, 5, 5));
        
        for (String emoji : EMOJIS) {
            JLabel label = new JLabel(emoji);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(20f));
            
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    emojiSelectCallback.accept(emoji);
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    label.setBorder(BorderFactory.createLineBorder(Color.BLUE));
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    label.setBorder(null);
                }
            });
            
            add(label);
        }
    }
}
```

## 数据库表结构

### 消息表 (messages)

```sql
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_jid TEXT NOT NULL,
    receiver_jid TEXT NOT NULL,
    message_body TEXT NOT NULL,
    is_local BOOLEAN NOT NULL,
    timestamp BIGINT NOT NULL,
    
    -- 索引
    INDEX idx_participants (sender_jid, receiver_jid),
    INDEX idx_timestamp (timestamp)
);
```

### 配置表 (settings)

```sql
CREATE TABLE settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

## MIT License

```
MIT License

Copyright (c) 2024 iaaaannn0

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

