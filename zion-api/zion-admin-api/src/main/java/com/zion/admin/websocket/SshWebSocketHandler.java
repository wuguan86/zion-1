package com.zion.admin.websocket;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.zion.system.entity.SysServer;
import com.zion.system.service.SysServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH WebSocket 处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshWebSocketHandler extends TextWebSocketHandler {

    private final SysServerService serverService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储会话信息
    private final Map<String, SshSessionInfo> sshSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("SSH WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode json = objectMapper.readTree(payload);
        String type = json.has("type") ? json.get("type").asText() : "";

        switch (type) {
            case "connect":
                handleConnect(session, json);
                break;
            case "data":
                handleData(session, json);
                break;
            case "resize":
                handleResize(session, json);
                break;
            default:
                log.warn("未知的消息类型: {}", type);
        }
    }

    /**
     * 处理连接请求
     */
    private void handleConnect(WebSocketSession session, JsonNode json) {
        Long serverId = json.has("serverId") ? json.get("serverId").asLong() : null;
        if (serverId == null) {
            sendError(session, "服务器ID不能为空");
            return;
        }

        SysServer server = serverService.getById(serverId);
        if (server == null) {
            sendError(session, "服务器不存在");
            return;
        }

        if (server.getStatus() != 1) {
            sendError(session, "服务器已禁用");
            return;
        }

        // 获取终端大小
        int cols = json.has("cols") ? json.get("cols").asInt() : 80;
        int rows = json.has("rows") ? json.get("rows").asInt() : 24;

        try {
            // 创建 SSH 连接
            SshClient client = SshClient.setUpDefaultClient();
            client.start();

            ClientSession sshSession = client.connect(server.getUsername(), server.getHost(), server.getPort())
                    .verify(Duration.ofSeconds(10))
                    .getSession();

            // 密码认证
            if (server.getAuthType() == 1 && server.getPassword() != null) {
                sshSession.addPasswordIdentity(server.getPassword());
            }

            sshSession.auth().verify(Duration.ofSeconds(10));

            if (!sshSession.isAuthenticated()) {
                sendError(session, "SSH认证失败");
                client.stop();
                return;
            }

            // 创建 Shell 通道
            ChannelShell channel = sshSession.createShellChannel();
            channel.setPtyType("xterm-256color");
            channel.setPtyColumns(cols);
            channel.setPtyLines(rows);
            channel.open().verify(Duration.ofSeconds(10));

            // 获取输入输出流
            InputStream inputStream = channel.getInvertedOut();
            OutputStream outputStream = channel.getInvertedIn();

            // 保存会话信息
            SshSessionInfo info = new SshSessionInfo();
            info.client = client;
            info.session = sshSession;
            info.channel = channel;
            info.inputStream = inputStream;
            info.outputStream = outputStream;
            sshSessions.put(session.getId(), info);

            // 启动读取线程
            Thread readThread = new Thread(() -> readFromSsh(session, inputStream));
            readThread.setDaemon(true);
            readThread.start();
            info.readThread = readThread;

            // 更新最后连接时间
            SysServer updateServer = new SysServer();
            updateServer.setId(serverId);
            updateServer.setLastConnectTime(LocalDateTime.now());
            serverService.updateById(updateServer);

            // 发送连接成功消息
            sendMessage(session, "{\"type\":\"connected\"}");
            log.info("SSH连接成功: {} -> {}@{}:{}", session.getId(), server.getUsername(), server.getHost(), server.getPort());

        } catch (Exception e) {
            log.error("SSH连接失败: {}", e.getMessage());
            sendError(session, "SSH连接失败: " + e.getMessage());
        }
    }

    /**
     * 处理数据传输
     */
    private void handleData(WebSocketSession session, JsonNode json) {
        SshSessionInfo info = sshSessions.get(session.getId());
        if (info == null || info.outputStream == null) {
            return;
        }

        String data = json.has("data") ? json.get("data").asText() : "";
        try {
            info.outputStream.write(data.getBytes(StandardCharsets.UTF_8));
            info.outputStream.flush();
        } catch (IOException e) {
            log.error("写入SSH数据失败: {}", e.getMessage());
        }
    }

    /**
     * 处理终端大小调整
     */
    private void handleResize(WebSocketSession session, JsonNode json) {
        SshSessionInfo info = sshSessions.get(session.getId());
        if (info == null || info.channel == null) {
            return;
        }

        int cols = json.has("cols") ? json.get("cols").asInt() : 80;
        int rows = json.has("rows") ? json.get("rows").asInt() : 24;

        try {
            info.channel.sendWindowChange(cols, rows);
        } catch (IOException e) {
            log.error("调整终端大小失败: {}", e.getMessage());
        }
    }

    /**
     * 从 SSH 读取数据
     */
    private void readFromSsh(WebSocketSession session, InputStream inputStream) {
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                if (!session.isOpen()) {
                    break;
                }
                String data = new String(buffer, 0, len, StandardCharsets.UTF_8);
                String jsonMsg = objectMapper.writeValueAsString(Map.of("type", "data", "data", data));
                session.sendMessage(new TextMessage(jsonMsg));
            }
        } catch (Exception e) {
            if (session.isOpen()) {
                log.error("读取SSH数据失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("SSH WebSocket 连接关闭: {}", session.getId());
        closeSshSession(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("SSH WebSocket 传输错误: {}", exception.getMessage());
        closeSshSession(session.getId());
    }

    /**
     * 关闭 SSH 会话
     */
    private void closeSshSession(String sessionId) {
        SshSessionInfo info = sshSessions.remove(sessionId);
        if (info != null) {
            try {
                if (info.channel != null) {
                    info.channel.close();
                }
                if (info.session != null) {
                    info.session.close();
                }
                if (info.client != null) {
                    info.client.stop();
                }
            } catch (Exception e) {
                log.error("关闭SSH会话失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String error) {
        try {
            String jsonMsg = objectMapper.writeValueAsString(Map.of("type", "error", "message", error));
            sendMessage(session, jsonMsg);
        } catch (Exception e) {
            log.error("发送错误消息失败: {}", e.getMessage());
        }
    }

    /**
     * SSH 会话信息
     */
    private static class SshSessionInfo {
        SshClient client;
        ClientSession session;
        ChannelShell channel;
        InputStream inputStream;
        OutputStream outputStream;
        Thread readThread;
    }
}
