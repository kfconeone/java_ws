package com.kfc.kfconeone.RTDB;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kfc.kfconeone.SocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
//    有三個地方需要檢查數量：
//    Push後檢查訊息總數量 - 暫定200，超過後進行備份
//    Socket連接後檢查sessionMap - 暫定300
//    後檢查桌中存在連線 - 暫定100

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
        //檢查有沒有決定Table是否Queue型態
        if(req.has("isQueueTable")) newRoot.isQueueTable = req.get("isQueueTable").getAsBoolean();
        if(req.has("subscribedSessionBound")) newRoot.subscribedSessionBound = req.get("subscribedSessionBound").getAsInt();

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

        if(mObject.isQueueTable)
        {
            res.put("result","002");
            res.put("message","can't update a queue table");
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

    @SuppressWarnings("unchecked")
    @RequestMapping(path = "/Push" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Push(@RequestBody String _req) throws InterruptedException, IOException {
        //step 1 : 解析收到的request
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        //step 2 :檢查有沒有這張桌子並檢查是不是可以push的QueueTable，沒有就直接回傳訊息
        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        if(!mObject.isQueueTable)
        {
            res.put("result","002");
            res.put("message","not a queue table");
            return res;
        }

        //step 3 : 將資料庫中的table取出後，把request中的訊息給加上去
        //pushArray會留存全部訊息，而detail中止會留存最新十條
        ArrayList<Object> tempPushArray;
        tempPushArray = gson.fromJson(new Gson().toJson(mObject.pushArray),ArrayList.class);

        Object pushObject = gson.fromJson(req.get("pushObject").toString(),Object.class);
        tempPushArray.add(pushObject);


        if(tempPushArray.size() < (mObject.pushTableDetailLength + 1))
        {
            mObject.detail = tempPushArray;
        }
        else
        {
            mObject.detail = tempPushArray.subList(tempPushArray.size() - mObject.pushTableDetailLength,tempPushArray.size());
        }

        //step 4 : 超過一定筆數，就紀錄聊天訊息
        if(tempPushArray.size() > mObject.pushArrayBound)
        {
            String fileName = DateTimeFormatter.ofPattern("yyyymmdd_hhmmss").format(ZonedDateTime.now(ZoneOffset.UTC));
            try (FileWriter writer = new FileWriter(String.format("%s_%s_%s",fileName,tableId,"Backup.txt")))
            {
                writer.write(new Gson().toJson(tempPushArray));
                mObject.pushArray = new ArrayList<>(tempPushArray.subList(tempPushArray.size() - mObject.pushTableDetailLength,tempPushArray.size()));
            }
        }
        else
        {
            mObject.pushArray = tempPushArray;
        }

        //step 5 : 將訊息回傳給所有註冊桌子的使用者，如果session不存在，
        //則加到removeList中，迴圈後刪除
        ArrayList<String> removeList = new ArrayList<>();
        for (String sessionName : mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionName))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionName);
                if(session.isOpen())
                {
                    Map<String,Object> msg = new HashMap<>();
                    msg.put("tableId",req.get("tableId"));
                    msg.put("pushObject",pushObject);
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


    @RequestMapping(path = "/BackUpDb" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map BackUpAllTable() throws IOException
    {

        Map<String,Object> res = new HashMap<>();
        List<Root> allTable = rootRepository.findAll();
        Gson gson = new Gson();

        String fileName = DateTimeFormatter.ofPattern("yyyymmdd_hhmmss").format(ZonedDateTime.now(ZoneOffset.UTC));
        try (FileWriter writer = new FileWriter(String.format("%s_%s",fileName,"AllTable_BackUp.txt")))
        {
            writer.write(gson.toJson(allTable));
        }

        res.put("result","000");
        return res;
    }

    //改變桌子的設定，會將所有的detail和pushArray還原。
    @RequestMapping(path = "/ChangeConfig" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map ChangeConfig(@RequestBody String _req) throws InterruptedException, IOException {

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

        if(req.has("isQueueTable")) mObject.isQueueTable = req.get("isQueueTable").getAsBoolean();
        if(req.has("pushArrayBound")) mObject.pushArrayBound = req.get("pushArrayBound").getAsInt();
        if(req.has("pushTableDetailLength")) mObject.pushTableDetailLength = req.get("pushTableDetailLength").getAsInt();
        if(req.has("subscribedSessionBound")) mObject.subscribedSessionBound = req.get("subscribedSessionBound").getAsInt();


        rootRepository.save(mObject);

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

        //當桌面上註冊的sessionId超過一定數量，進行檢查把斷線的給刪除
        if(mObject.sessionIds.size() > mObject.subscribedSessionBound)
        {
            ArrayList<String> removeList = new ArrayList<>();
            for (String sessionId : mObject.sessionIds)
            {
                if(SocketHandler.sessionMap.containsKey(sessionId))
                {
                    if(!SocketHandler.sessionMap.get(sessionId).isOpen())
                    {
                        SocketHandler.sessionMap.remove(sessionId);
                        removeList.add(sessionId);
                    }
                }
                else
                {
                    removeList.add(sessionId);
                }
            }
            mObject.sessionIds.removeAll(removeList);
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
