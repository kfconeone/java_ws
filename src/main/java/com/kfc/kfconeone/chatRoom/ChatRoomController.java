package com.kfc.kfconeone.chatRoom;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.Root;
import com.kfc.kfconeone.RTDB.RootRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class ChatRoomController {

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public ChatRoomController(RootRepository _userRepository)
    {
        rootRepository = _userRepository;
    }


    @Deprecated
    @RequestMapping(path = "/CreateLobbyChatRoom" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map CreateLobbyChatRoom() throws IOException
    {
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();

        if(rootRepository.findByTableIdAndGroupId("LobbyChatRoom","ChatRoom") != null)
        {
            res.put("result","001");
            res.put("message","table exists");
            return res;
        }

        Root newRoot = new Root();

        //直接new Object()會出錯
        newRoot.detail = gson.fromJson("{}",Object.class);
        newRoot.privateDetail = gson.fromJson("{}",Object.class);
        newRoot.pushArray = new ArrayList<>();
        newRoot.tableId = "LobbyChatRoom";
        newRoot.isQueueTable = true;
        rootRepository.save(newRoot);


        res.put("result","000");
        res.put("message","success");
        return res;
    }
}
