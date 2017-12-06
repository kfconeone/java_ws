package com.kfc.kfconeone.RTDB;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kfc.kfconeone.SocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController     //必加Annotation，告知Spring這個Class是Controller
public class mainController {

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping("/hello")   //建立URI，也可以放在class前面
    public @ResponseBody String Hello(@RequestParam(value="name", defaultValue="World") String name) {
        //ResponseBody、RequestParam都算是重要的Annotation，告知Spring要如何處置req和res
        //像這個例子就是把字串"Hello"放置在Response的Body回傳
        return "Hello";
    }

    @RequestMapping(path = "/Create" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Create(@RequestBody String _req) {

        Map<String,String> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);
        String tableId = req.get("tableId").getAsString();
        if(rootRepository.findByTableId(tableId) != null)
        {
            res.put("result","001 table exists");
            return res;
        }

        Root newRoot = new Root();

        newRoot.detail = gson.fromJson(req.get("detail").toString(),Object.class);
        newRoot.tableId = req.get("tableId").getAsString();

        rootRepository.save(newRoot);


        res.put("result","000 success");

        return res;
    }

    @RequestMapping(path = "/Read" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Read(@RequestBody String _req) {

        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        Root mObject = rootRepository.findByTableId(req.get("tableId").getAsString());

        Map<String,Object> res = new HashMap<>();
        res.put("result","000");
        res.put("detail",mObject.detail);
        return res;
    }

    @RequestMapping(path = "/Update" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Update(@RequestBody String _req) throws InterruptedException, IOException {

        Map<String,String> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        Root mObject = rootRepository.findByTableId(req.get("tableId").getAsString());
        mObject.detail = gson.fromJson(req.get("detail").toString(),Object.class);

        for (Object sessionName :mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionName.toString()))
            {
                WebSocketSession session = ((WebSocketSession) SocketHandler.sessionMap.get(sessionName.toString()));
                if(session.isOpen())
                {
                    session.sendMessage(new TextMessage(mObject.detail.toString()));
                }
                else
                {
                    mObject.sessionIds.remove(sessionName);
                }
            }
        }
        rootRepository.save(mObject);


        res.put("result","success");
        return res;
    }

    @RequestMapping(path = "/Subscribe" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Register(@RequestBody String _req) {

        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        Root mObject = rootRepository.findByTableId(req.get("tableId").getAsString());
        if(mObject.sessionIds == null)
        {
            mObject.sessionIds = new ArrayList();
        }
        mObject.sessionIds.add(req.get("sessionId").getAsString());
        rootRepository.save(mObject);

        Map<String,String> res = new HashMap<>();
        res.put("result","success");
        return res;
    }
}
