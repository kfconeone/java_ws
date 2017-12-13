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

import java.io.IOException;
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

    @RequestMapping("/hello")   //建立URI，也可以放在class前面
    public @ResponseBody String Hello(@RequestParam(value="name", defaultValue="World") String name) {
        //ResponseBody、RequestParam都算是重要的Annotation，告知Spring要如何處置req和res
        //像這個例子就是把字串"Hello"放置在Response的Body回傳
        return "Hello" + name;
    }



    @RequestMapping(path = "/Push" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Push(@RequestBody String _req) throws InterruptedException, IOException {

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


        @SuppressWarnings("unchecked")
        ArrayList<Object> tempPushArray = gson.fromJson(mObject.pushArray.toString(),ArrayList.class);
        Object pushObject = gson.fromJson(req.get("pushObject").toString(),Object.class);
        tempPushArray.add(pushObject);
        mObject.pushArray = tempPushArray;
        if(tempPushArray.size() < 11)
        {
            mObject.detail = tempPushArray;
        }
        else
        {
            mObject.detail = tempPushArray.subList(tempPushArray.size() - 11,tempPushArray.size() - 1);
        }

        for (Object sessionName :mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionName.toString()))
            {
                WebSocketSession session = SocketHandler.sessionMap.get(sessionName.toString());
                if(session.isOpen())
                {
                    Map<String,Object> msg = new HashMap<>();
                    msg.put("tableId",req.get("tableId"));
                    msg.put("pushObject",pushObject);
                    session.sendMessage(new TextMessage(new Gson().toJson(msg)));
                }

            }
        }
        rootRepository.save(mObject);


        res.put("result","000");
        res.put("message","success");
        return res;
    }
}
