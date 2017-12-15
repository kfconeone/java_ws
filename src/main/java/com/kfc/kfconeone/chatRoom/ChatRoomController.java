package com.kfc.kfconeone.chatRoom;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kfc.kfconeone.RTDB.Root;
import com.kfc.kfconeone.RTDB.RootRepository;
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
import java.util.Map;

@RestController     //必加Annotation，告知Spring這個Class是Controller
public class ChatRoomController {

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public ChatRoomController(RootRepository _userRepository)
    {
        rootRepository = _userRepository;
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(path = "/Push" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Push(@RequestBody String _req) throws InterruptedException, IOException {
        //step 1 : 解析收到的request
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        //step 2 :檢查有沒有這張桌子，沒有就直接回傳訊息
        String tableId = req.get("tableId").getAsString();
        Root mObject = rootRepository.findByTableId(tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        //step 3 : 將資料庫中的table取出後，把request中的訊息給加上去
        //pushArray會留存全部訊息，而detail中止會留存最新十條
        ArrayList<Object> tempPushArray;
        tempPushArray = gson.fromJson(new Gson().toJson(mObject.pushArray),ArrayList.class);

        Object pushObject = gson.fromJson(req.get("pushObject").toString(),Object.class);
        tempPushArray.add(pushObject);


        if(tempPushArray.size() < 11)
        {
            mObject.detail = tempPushArray;
        }
        else
        {
            mObject.detail = tempPushArray.subList(tempPushArray.size() - 10,tempPushArray.size());
        }

        //step 4 : 超過一定筆數，就紀錄聊天訊息
        if(tempPushArray.size() > 200)
        {
            String fileName = DateTimeFormatter.ofPattern("yyyymmdd_hhmmss").format(ZonedDateTime.now(ZoneOffset.UTC));
            try (FileWriter writer = new FileWriter(String.format("%s%s",fileName,"ChatRoomBackup.txt")))
            {
                writer.write(new Gson().toJson(tempPushArray));
                mObject.pushArray = new ArrayList<>(tempPushArray.subList(tempPushArray.size() - 10,tempPushArray.size()));
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
}
