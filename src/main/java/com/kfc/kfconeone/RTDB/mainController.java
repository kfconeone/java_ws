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

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public mainController(RootRepository _userRepository)
    {
        rootRepository = _userRepository;
    }

    @RequestMapping("/hello")   //建立URI，也可以放在class前面
    public @ResponseBody String Hello(@RequestParam(value="name", defaultValue="World") String name) {
        //ResponseBody、RequestParam都算是重要的Annotation，告知Spring要如何處置req和res
        //像這個例子就是把字串"Hello"放置在Response的Body回傳
        return "Hello" + name;
    }

//    以下是CRUD通用接口
    @RequestMapping(path = "/Create" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Create(@RequestBody String _req) {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);
        String tableId = req.get("tableId").getAsString();
        if(rootRepository.findByTableId(tableId) != null)
        {
            res.put("result","001");
            res.put("message","table exists");
            return res;
        }

        Root newRoot = new Root();

        newRoot.detail = gson.fromJson(req.get("detail").toString(),Object.class);
        newRoot.pushArray = new ArrayList<>();
        newRoot.tableId = req.get("tableId").getAsString();
        rootRepository.save(newRoot);


        res.put("result","000");
        res.put("message","success");
        return res;
    }

    @RequestMapping(path = "/Read" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Read(@RequestBody String _req) {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        res.put("result","000");
        res.put("detail",mObject.detail);
        return res;
    }

    @RequestMapping(path = "/Update" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Update(@RequestBody String _req) throws InterruptedException, IOException {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }
        mObject.detail = gson.fromJson(req.get("detail").toString(),Object.class);

        ArrayList<String> removeList = new ArrayList<>();
        for (String sessionName :mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionName))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionName);
                if(session.isOpen())
                {
                    Map<String,Object> msg = new HashMap<>();
                    msg.put("tableId",req.get("tableId"));
                    msg.put("detail",mObject.detail);
                    session.sendMessage(new TextMessage(new Gson().toJson(msg)));
                }
                else
                {
                    removeList.add(sessionName);
                }
            }
            else
            {
                removeList.add(sessionName);
            }
        }
        mObject.sessionIds.removeAll(removeList);

        rootRepository.save(mObject);


        res.put("result","000");
        res.put("message","success");
        return res;
    }




    @RequestMapping(path = "/Delete" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Delete(@RequestBody String _req) throws InterruptedException, IOException {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        Map<String,Object> resToPlayer = new HashMap<>();
        resToPlayer.put("result","100");
        resToPlayer.put("tableId",req.get("tableId"));
        resToPlayer.put("message","table is remove");
        for (Object sessionName :mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionName.toString()))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionName.toString());
                if(session.isOpen())
                {
                    session.sendMessage(new TextMessage(new Gson().toJson(resToPlayer)));
                }
            }
        }
        rootRepository.delete(mObject.id);


        res.put("result","000");
        res.put("message","success");
        return res;
    }


//    以下是依照桌內是否有玩家決定刪桌
@RequestMapping(path = "/DeleteBySessions" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
public @ResponseBody
Map DeleteBySessions() throws InterruptedException, IOException {

    Map<String,Object> res = new HashMap<>();

    //先檢查所有的sessions有沒有已經斷線的，已經斷線的就直接移除
    for(String sessionKey : SocketHandler.sessionMap.keySet())
    {
        if(!SocketHandler.sessionMap.get(sessionKey).isOpen())
        {
            System.out.println(sessionKey);
            SocketHandler.sessionMap.remove(sessionKey);
        }
    }

    List<Root> allTable = rootRepository.findAll();
    for(Root table : allTable)
    {
        ArrayList<String> tempSessionList = new ArrayList<>();
        for(String sessionId : table.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionId))
            {
                tempSessionList.add(sessionId);
            }
        }

        if(tempSessionList.isEmpty())
        {
            rootRepository.delete(table);
        }
    }

    res.put("result","000");
    res.put("message","success");
    return res;
}

//    以下是Client端要使用的接口
    @RequestMapping(path = "/Subscribe" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Subscribe(@RequestBody String _req) {
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }


        if (!mObject.sessionIds.contains(req.get("sessionId").getAsString()))
        {
            mObject.sessionIds.add(req.get("sessionId").getAsString());
        }
        else
        {
            res.put("result","001");
            res.put("message","already exist");
            return res;
        }

        rootRepository.save(mObject);

        res.put("result","000");
        res.put("message","success");
        res.put("detail",mObject.detail);
        return res;
    }

    @RequestMapping(path = "/Unsubscribe" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Unsubscribe(@RequestBody String _req) {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        if (mObject.sessionIds.contains(req.get("sessionId").getAsString()))
        {
            mObject.sessionIds.remove(req.get("sessionId").getAsString());
        }
        else
        {
            res.put("result","001");
            res.put("message","not subscribe");
            return res;
        }
        rootRepository.save(mObject);


        res.put("result","000");
        res.put("message","success");
        return res;
    }
}
