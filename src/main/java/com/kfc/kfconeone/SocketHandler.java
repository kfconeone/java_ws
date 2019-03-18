package com.kfc.kfconeone;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

@Component
public class SocketHandler extends TextWebSocketHandler {

//    @Resource
//    private RootRepository rootRepository;

    public static Map<String,WebSocketSession> sessionMap = new HashMap<>();
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException {

        System.out.println(message.getPayload());
        if(message.getPayload().equalsIgnoreCase(("KeepAlive")))
        {
            Map<String,Object> res = new HashMap<>();
            res.put("messageType","KEEP_ALIVE");
            session.sendMessage(new TextMessage(new Gson().toJson(res)));
        }

//        session.sendMessage(new TextMessage(new Gson().toJson(rootRepository.findByTableId("Room001").detail.toString())));

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        System.out.println("Connected");
        String uuid = UUID.randomUUID().toString();
        sessionMap.put(uuid,session);
        System.out.println(uuid);

        Map<String,Object> res = new HashMap<>();
//        Resource resource = new ClassPathResource("/zone.properties");
//        Properties props = PropertiesLoaderUtils.loadProperties(resource);
        res.put("messageType","CONNECTED");
        res.put("sessionId",uuid);
//        res.put("zone",props.getProperty("zone"));
        session.sendMessage(new TextMessage(new Gson().toJson(res)));
    }
}