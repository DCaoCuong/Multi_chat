package com.caocuong.multi_chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

@Configuration
@EnableWebSocket
public class multiChatConfig implements WebSocketConfigurer {

    @Autowired
    private multiChatHandler chatHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry rgt){
        rgt.addHandler(chatHandler, "/chat_index")
                .setAllowedOrigins("*");
    }
}
