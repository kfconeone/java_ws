package com.kfc.kfconeone;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.RootRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Component
public class SocketHandler extends TextWebSocketHandler {

    @Autowired
    private RootRepository rootRepository;

    public static Map<String,WebSocketSession> sessionMap = new HashMap<>();
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
        sessionMap.put(uuid,session);
        System.out.println(uuid);

        Map<String,Object> res = new HashMap<>();
        Resource resource = new ClassPathResource("/zone.properties");
        Properties props = PropertiesLoaderUtils.loadProperties(resource);
        res.put("event","connect");
        res.put("sessionId",uuid);
        res.put("zone",props.getProperty("zone"));
        session.sendMessage(new TextMessage(new Gson().toJson(res)));
    }
}