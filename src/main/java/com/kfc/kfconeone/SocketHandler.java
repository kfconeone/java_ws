package com.kfc.kfconeone;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.RootRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

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

        ArrayList<String> removeList = new ArrayList<>();
        if(sessionMap.size() > 3)
        {
            for (String sessionName : sessionMap.keySet())
            {
                if(!sessionMap.get(sessionName).isOpen())
                {
                    removeList.add(sessionName);
                }
            }

            for (String sessionName : removeList)
            {
                sessionMap.remove(sessionName);
            }
        }

        Map<String,Object> res = new HashMap<>();
//        Resource resource = new ClassPathResource("/zone.properties");
//        Properties props = PropertiesLoaderUtils.loadProperties(resource);
        res.put("tableId","CONNECT");
        res.put("sessionId",uuid);
//        res.put("zone",props.getProperty("zone"));
        session.sendMessage(new TextMessage(new Gson().toJson(res)));
    }
}