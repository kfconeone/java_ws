package com.kfc.kfconeone.RTDB;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kfc.kfconeone.SocketHandler;
import com.kfc.kfconeone.chatRoom.DiamondSlotsParameters;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class mainController {

    // ================說明===============
// 這裡主要是處理WebSocket直接通知使用者訊息的一些接口，
// 此處說明Server-Client端的互動模式：
// 1.使用者進行WS連線。
// 2.連線成功後會訂閱自己的SystemMsg的桌子，此時取得桌子上所有的訊息，
// 如果桌子不存在就直接創造一個，並且用自己的帳號作為key。
// 3.每個訊息都有自己的欄位，並且必定包含一個long型態的dateTime。
// 4.Server有特定訊息要通知則直接寫入欄位，並且更新dateTime。
//

    @Resource
    private RootRepository rootRepository;

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
//    @Deprecated
//    @RequestMapping(path = "/Create" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
//    public @ResponseBody
//    Map Create(@RequestBody String _req) {
//
//        Map<String,Object> res = new HashMap<>();
//        Gson gson = new Gson();
//        JsonObject req = gson.fromJson(_req,JsonObject.class);
//        String tableId = req.get("tableId").getAsString();
//        if(rootRepository.findByTableId(tableId) != null)
//        {
//            res.put("result","001");
//            res.put("message","table exists");
//            return res;
//        }
//
//        Root newRoot = new Root();
//
//        //注意，不用括號的字串轉成object，mongo有bug存不進去
//        if(req.has("detail")) newRoot.detail = gson.fromJson(req.get("detail").toString(),Object.class);
//        else newRoot.detail = gson.fromJson("{}",Object.class);
//
//        newRoot.pushArray = new ArrayList<>();
//        newRoot.tableId = req.get("tableId").getAsString();
//        //建立一個private的detail，面向伺服器端，使用者端不可見
//        if(req.has("privateDetail")) newRoot.detail = gson.fromJson(req.get("privateDetail").toString(),Object.class);
//        else newRoot.detail = gson.fromJson("{}",Object.class);
//
//        //檢查有沒有決定Table是否Queue型態
//        if(req.has("isQueueTable")) newRoot.isQueueTable = req.get("isQueueTable").getAsBoolean();
//        if(req.has("subscribedSessionBound")) newRoot.subscribedSessionBound = req.get("subscribedSessionBound").getAsInt();
//
//        rootRepository.save(newRoot);
//
//
//        res.put("result","000");
//        res.put("message","success");
//        return res;
//    }


    //非Queue Table才可以Update，不然要用Push
    @RequestMapping(path = "/Update{optionalParameter}" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Update(@RequestBody String _req,@PathVariable(name = "optionalParameter") String _optionalParameter) throws InterruptedException, IOException {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            mObject = new com.kfc.kfconeone.template.RootTableCreator().CreateNewTable(groupId,tableId,false,100,100,200);
        }

        if(mObject.isQueueTable)
        {
            res.put("result","002");
            res.put("message","can't update a queue table");
            return res;
        }

        Object detail = gson.fromJson(req.get("detail").toString(),Object.class);
        mObject.detail = gson.toJson(detail);
        if(_optionalParameter.equals("Superior"))
        {
            if(req.has("privateDetail"))
            {
                mObject.privateDetail = gson.toJson(gson.fromJson(req.get("privateDetail").toString(),Object.class));
            }
        }

        mObject.lastUpdateTime = ZonedDateTime.now(ZoneOffset.UTC).plusHours(8).toInstant().toEpochMilli();

        ArrayList<String> removeList = new ArrayList<>();
        for (String sessionId :mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionId))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionId);
                if(session.isOpen())
                {
                    Map<String,Object> msg = new HashMap<>();
                    msg.put("messageType","UPDATE_MESSAGE");
                    msg.put("groupId",groupId);
                    msg.put("tableId",req.get("tableId"));
                    msg.put("lastUpdateTime",mObject.lastUpdateTime);
                    msg.put("detail",detail);
                    session.sendMessage(new TextMessage(new Gson().toJson(msg)));
                }
                else
                {
                    removeList.add(sessionId);
                }
            }
            else
            {
                removeList.add(sessionId);
            }
        }
        mObject.sessionIds.removeAll(removeList);

        rootRepository.save(mObject);


        res.put("result","000");
        res.put("lastUpdateTime",mObject.lastUpdateTime);
        res.put("detail",detail);
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
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        Map<String,Object> resToPlayer = new HashMap<>();
        resToPlayer.put("result","100");
        resToPlayer.put("messageType","UPDATE_MESSAGE");
        resToPlayer.put("tableId",tableId);
        resToPlayer.put("tableId",groupId);
        resToPlayer.put("message","table is remove");
        for (Object sessionId :mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionId.toString()))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionId.toString());
                if(session.isOpen())
                {
                    session.sendMessage(new TextMessage(new Gson().toJson(resToPlayer)));
                }
            }
        }
        rootRepository.delete(mObject);


        res.put("result","000");
        res.put("message","success");
        return res;
    }

    //Queue Table才可以Push，不然要用Update
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
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            mObject = new com.kfc.kfconeone.template.RootTableCreator().CreateNewTable(groupId,tableId,true,100,100,200);
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

        tempPushArray = gson.fromJson(mObject.pushArray,ArrayList.class);

        Object pushObject = gson.fromJson(req.get("pushObject").toString(),Object.class);
        tempPushArray.add(pushObject);


        if(tempPushArray.size() < (mObject.pushTableDetailLength + 1))
        {
            mObject.detail = gson.toJson(tempPushArray);
        }
        else
        {
            mObject.detail = gson.toJson(tempPushArray.subList(tempPushArray.size() - mObject.pushTableDetailLength,tempPushArray.size()));
        }

        //step 4 : 超過一定筆數，就紀錄聊天訊息
        if(tempPushArray.size() > mObject.pushArrayBound)
        {
            String fileName = DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss").format(ZonedDateTime.now(ZoneOffset.UTC).plusHours(8));

            if(new File(fileName).exists())
            {
                fileName += UUID.randomUUID();
            }

            try (FileWriter writer = new FileWriter(String.format("%s_%s_%s",fileName,tableId,"Backup.txt")))
            {
                writer.write(new Gson().toJson(tempPushArray));
                mObject.pushArray = gson.toJson(new ArrayList<>(tempPushArray.subList(tempPushArray.size() - mObject.pushTableDetailLength,tempPushArray.size())));
            }
        }
        else
        {
            mObject.pushArray = gson.toJson(tempPushArray);
        }

        //紀錄最後更新時間
        mObject.lastUpdateTime = ZonedDateTime.now(ZoneOffset.UTC).plusHours(8).toInstant().toEpochMilli();

        //step 5 : 將訊息回傳給所有註冊桌子的使用者，如果session不存在，
        //則加到removeList中，迴圈後刪除
        ArrayList<String> removeList = new ArrayList<>();
        for (String sessionId : mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionId))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionId);
                if(session.isOpen())
                {
                    Map<String,Object> msg = new HashMap<>();
                    msg.put("messageType","PUSH_MESSAGE");
                    msg.put("groupId",groupId);
                    msg.put("tableId",tableId);
                    msg.put("lastUpdateTime",mObject.lastUpdateTime);
                    msg.put("pushObject",pushObject);
                    session.sendMessage(new TextMessage(new Gson().toJson(msg)));
                }
                else
                {
                    removeList.add(sessionId);
                }
            }
            else
            {
                removeList.add(sessionId);
            }
        }

        mObject.sessionIds.removeAll(removeList);

        rootRepository.save(mObject);


        res.put("result","000");
        res.put("lastUpdateTime",mObject.lastUpdateTime);
        res.put("pushObject",pushObject);
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

        String fileName = DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss").format(ZonedDateTime.now(ZoneOffset.UTC).plusHours(8));

        if(new File(fileName).exists())
        {
            fileName += UUID.randomUUID();
        }

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
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
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
    //=================部分特定功能==================
    //檢查桌子是否存在
    @RequestMapping(path = "/CheckTableExist" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map CheckTableExist(@RequestBody String _req) {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)

        res.put("result","000");
        res.put("isExist",(mObject != null));
        return res;
    }

//==================以下是Client端要使用的接口================
    @RequestMapping(path = "/Subscribe" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Subscribe(@RequestBody String _req) {
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            Boolean isQueueTable = req.get("isQueueTable").getAsBoolean();
            mObject = new com.kfc.kfconeone.template.RootTableCreator().CreateNewTable(groupId,tableId,isQueueTable,100,100,200);

        }


        if (!mObject.sessionIds.contains(req.get("sessionId").getAsString()))
        {
            mObject.sessionIds.add(req.get("sessionId").getAsString());
            res.put("sessionId",req.get("sessionId").getAsString());
        }
        else
        {
            res.put("result","001");
            res.put("groupId",groupId);
            res.put("tableId",tableId);
            res.put("message","already subscribed");
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

        //========這裡開始將Account和sessionId做連結=======

        rootRepository.save(mObject);

        res.put("result","000");
        res.put("message","success");
        res.put("groupId",groupId);
        res.put("tableId",tableId);
        res.put("detail",gson.fromJson(mObject.detail,Object.class));
        res.put("lastUpdateTime",mObject.lastUpdateTime);
        return res;
    }

    @RequestMapping(path = "/Unsubscribe" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Unsubscribe(@RequestBody String _req) {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("groupId",groupId);
            res.put("tableId",tableId);
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
            res.put("groupId",groupId);
            res.put("tableId",tableId);
            res.put("message","not subscribe");
            return res;
        }
        rootRepository.save(mObject);


        res.put("result","000");
        res.put("groupId",groupId);
        res.put("tableId",tableId);
        res.put("message","success");
        return res;
    }


    //檢查session是否還存活，如果已經死亡則刪除
    @RequestMapping(path = "/CheckAliveSessions" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map CleanSessionMap() {

        Map<String,Object> res = new HashMap<>();
        ArrayList<String> removeList = new ArrayList<>();

        for (String sessionId : SocketHandler.sessionMap.keySet())
        {
            if(!SocketHandler.sessionMap.get(sessionId).isOpen())
            {
                removeList.add(sessionId);
            }
        }

        for (String sessionId : removeList)
        {
            SocketHandler.sessionMap.remove(sessionId);
        }

        res.put("result","000");
        res.put("aliveSessionsCount",SocketHandler.sessionMap.size());
        res.put("message","success");
        return res;
    }


    //=======將Account和SessionId做連結
    @RequestMapping(path = "/SetAccountToSessionId" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map SetAccountToSessionId(@RequestBody String _req) {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String account = req.get("account").getAsString();
        String sessionId = req.get("sessionId").getAsString();

        DiamondSlotsParameters.accountToSessionIdMap.put(account,sessionId);

        res.put("result","000");
        res.put("message","success");
        return res;
    }
}
