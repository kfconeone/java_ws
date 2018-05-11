package com.kfc.kfconeone.SystemMsg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kfc.kfconeone.RTDB.Root;
import com.kfc.kfconeone.RTDB.RootRepository;
import com.kfc.kfconeone.SocketHandler;
import com.kfc.kfconeone.chatRoom.DiamondSlotsParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// ================說明===============
// 這裡主要是處理WebSocket直接通知使用者訊息的一些接口，
// 此處說明Server-Client端的互動模式：
// 1.使用者進行WS連線。
// 2.連線成功後會訂閱自己的SystemMsg的桌子，此時取得桌子上所有的訊息，
// 如果桌子不存在就直接創造一個，並且用自己的帳號作為key。
// 3.每個訊息都有自己的欄位，並且必定包含一個long型態的dateTime。
// 4.Server有特定訊息要通知則直接寫入欄位，並且更新dateTime。
//
@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class SystemMsgController {

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public SystemMsgController(RootRepository _rootRepository)
    {
        rootRepository = _rootRepository;
    }

    //傳送重要訊息，基本元素一定要有content和dateTime(long型態)
    @RequestMapping(path = "/SendSystemMsg" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map SendSystemMsg(@RequestBody String _req) throws IOException,NoSuchFieldException
    {
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);
        String account = req.get("account").getAsString();

        Root systemMsgTable = rootRepository.findByTableId("sys_" + account);
        if(systemMsgTable == null)
        {
            systemMsgTable = CreateSystemMsgTable(account);

        }

        Map<String,Object> detail = gson.fromJson(systemMsgTable.detail.toString(),HashMap.class);
        for(String keyName : req.keySet())
        {
            Map<String,Object> objectMap = new HashMap<>();
            objectMap.put("content",gson.fromJson(req.get(keyName).toString(),Object.class));
            objectMap.put("lastUpdateTime",ZonedDateTime.now(ZoneOffset.UTC).plusHours(8).toInstant().toEpochMilli());
            detail.put(keyName,objectMap);
        }

        systemMsgTable.detail = detail;

        rootRepository.save(systemMsgTable);

        res.put("result","000");
        res.put("tableId","sys_" + account);
        res.put("systemMsg",systemMsgTable.detail);

        //============這裡開始處理發送訊息給使用者的部分=============
        if(DiamondSlotsParameters.accountToSessionIdMap.containsKey(account))
        {
            String sessionId = DiamondSlotsParameters.accountToSessionIdMap.get(account);
            if(SocketHandler.sessionMap.containsKey(sessionId))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionId);
                if(session.isOpen())
                {


                }
            }
        }


        return res;
    }

    private Root CreateSystemMsgTable(String _account)
    {
        Root newRoot = new Root();

        //直接new Object()會出錯
        newRoot.detail = new HashMap<String,Object>();
        newRoot.privateDetail = new HashMap<String,Object>();
        newRoot.pushArray = new ArrayList<>();
        newRoot.tableId = "sys_" + _account;
        newRoot.isQueueTable = false;
        newRoot.subscribedSessionBound = 3;
        newRoot.lastUpdateTime = ZonedDateTime.now(ZoneOffset.UTC).plusHours(8).toInstant().toEpochMilli();

        return newRoot;
    }
}
