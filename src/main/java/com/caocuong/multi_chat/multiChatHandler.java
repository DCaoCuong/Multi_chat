package com.caocuong.multi_chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime; 
import java.time.format.DateTimeFormatter; 

@Component
public class multiChatHandler extends TextWebSocketHandler {
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            onlineUsers.remove(username);
            broadcastSystemMessage(username + " đã rời đi ...", null);
            broadcastOnlineUsers();
        }
    }

    private void broadcast(String message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                System.err.println("Lỗi gửi tin nhắn: " + e.getMessage());
            }
        }
    }

    private void broadcastSystemMessage(String content, String excludeUsername) {
        String message = String.format("SYSTEM|server|all|%s", content);
        for (WebSocketSession s : sessions) {
            try {
                String sessionUsername = (String) s.getAttributes().get("username");
                if (s.isOpen() && (excludeUsername == null || !excludeUsername.equals(sessionUsername))) {
                    s.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                System.err.println("Lỗi gửi tin nhắn trong hệ thống (phía server gặp trục trặc>>>): " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String sender = (String) session.getAttributes().get("username");
        LocalDateTime now = LocalDateTime.now(); 
        String timestamp = now.format(TIMESTAMP_FORMATTER); 

        if (payload.startsWith("/username ")) {
            String newUsername = payload.substring(10).trim();
            if (onlineUsers.containsKey(newUsername)) {
                session.sendMessage(new TextMessage("ERROR|server|" + newUsername + "|Tên này đã tồn tại, chọn tên khác nhé >"));
                return;
            }
            session.getAttributes().put("username", newUsername);
            onlineUsers.put(newUsername, session);
            broadcastSystemMessage(newUsername + " đã tham gia chat |>>>", null);
            broadcastOnlineUsers();
        } else if (sender == null) {
            session.sendMessage(new TextMessage("ERROR|server|unknown|Vui lòng nhập tên trước nè >???"));
            return;
        } else if (payload.startsWith("@")) {
            int spaceIndex = payload.indexOf(" ");
            if (spaceIndex > 1) {
                String recipient = payload.substring(1, spaceIndex).trim();
                String msgContent = payload.substring(spaceIndex + 1).trim();

                WebSocketSession recipientSession = onlineUsers.get(recipient);
                if (recipientSession != null && recipientSession.isOpen()) {
                    String privateMsg = String.format("PRIVATE|%s|%s|%s|%s", sender, recipient, timestamp, msgContent);

                    recipientSession.sendMessage(new TextMessage(privateMsg));
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(privateMsg));
                    }
                } else {
                    String errorMsg = String.format("ERROR|server|%s|USER_OFFLINE:%s", sender, recipient);
                    session.sendMessage(new TextMessage(errorMsg));
                }
            } else {
                String errorMsg = String.format("ERROR|server|%s|Cú pháp không hợp lệ. Tin nhắn riêng: @<người nhận>", sender);
                session.sendMessage(new TextMessage(errorMsg));
            }
        } else {
            String publicMsg = String.format("PUBLIC|%s|all|%s|%s", sender, timestamp, payload);
            broadcast(publicMsg);
        }
    }

    private void broadcastOnlineUsers() {
        String users = String.join(",", onlineUsers.keySet());
        String message = String.format("ONLINE_LIST|server|all|%s", users);
        broadcast(message);
    }
}
