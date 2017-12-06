package com.kfc.kfconeone;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.RootRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {

    @Autowired
    private RootRepository rootRepository;
    public static Map sessionMap = new HashMap<String,WebSocketSession>();
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException {

        System.out.println(message.getPayload());

        session.sendMessage(new TextMessage(new Gson().toJson(rootRepository.findByTableId("Room001").detail.toString())));

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        System.out.println("Connected");
        String uuid = UUID.randomUUID().toString();
//        SessionHandler.getSessionMap().put(uuid,session);
        sessionMap.put(uuid,session);
        System.out.println(uuid);
//        sessionsList.put(session.getId(),session);
        session.sendMessage(new TextMessage(String.format("{sessionId:\"%s\"}",uuid)));
    }
}