package com.kfc.kfconeone.chatRoom;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kfc.kfconeone.RTDB.SystemMsg;
import com.kfc.kfconeone.RTDB.SystemMsgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class SystemMsgController {

    private SystemMsgRepository systemMsgRepository;
    @Autowired // Spring推薦做法
    public SystemMsgController(SystemMsgRepository _systemMsgRepository)
    {
        systemMsgRepository = _systemMsgRepository;
    }

    @RequestMapping(path = "/SendImportantSystemMsg" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map SendImportantSystemMsg(@RequestBody String _req) throws IOException
    {
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);
        String account = req.get("account").getAsString();

        SystemMsg systemMsg = systemMsgRepository.findByAccount(account);
        if(systemMsg == null)
        {
            systemMsg = CreateSystemMsgTable(account);
            systemMsgRepository.save(systemMsg);
        }

        res.put("result","000");
        res.put("message",systemMsg);
        return res;
    }

    private SystemMsg CreateSystemMsgTable(String _account)
    {
        Gson gson = new Gson();
        SystemMsg newSystemMsg = new SystemMsg();
        newSystemMsg.account = _account;
        //直接new Object()會出錯
        newSystemMsg.importantMsg = gson.fromJson("{}",Object.class);

        return  newSystemMsg;
    }
}
