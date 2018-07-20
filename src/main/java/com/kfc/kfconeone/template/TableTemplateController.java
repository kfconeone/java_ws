package com.kfc.kfconeone.template;

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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class TableTemplateController {

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public TableTemplateController(RootRepository _userRepository)
    {
        rootRepository = _userRepository;
    }

    @RequestMapping(path = "/TableList" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map GetTemplateGuide() throws IOException
    {
        Map<String,Object> res = new HashMap<>();
        Map<String,Object> tableList = new HashMap<>();
        List<Root> rootList = rootRepository.findAll();

        for (Root root : rootList)
        {
            Map<String,Object> tempRoot = new HashMap<>();
            tempRoot.put("isQueueTable",root.isQueueTable);
            tempRoot.put("subscribedSessionBound",root.subscribedSessionBound);
            if(root.isQueueTable)
            {
                tempRoot.put("pushArrayBound",root.pushArrayBound);
                tempRoot.put("pushTableDetailLength",root.pushTableDetailLength);
            }
            tableList.put(root.tableId,tempRoot);
        }

        ArrayList<String> ApiExamples = new ArrayList<>();
        ApiExamples.add("http://localhost:8081/CreateTemplateTable?groupId=[GROUP_ID]&tableId=[TABLE_ID]&isQueueTable=true&pushArrayBound=100&pushTableDetailLength=15&subscribedSessionBound=120");
        ApiExamples.add("http://localhost:8081/DeleteTable?groupId=[GROUP_ID]&tableId=[TABLE_ID]");
        ApiExamples.add("http://localhost:8081/ReadTable?groupId=[GROUP_ID]&tableId=[TABLE_ID]");
        ApiExamples.add("http://localhost:8081/ReadTableSuperior?groupId=[GROUP_ID]&tableId=[TABLE_ID]");
        ApiExamples.add("http://localhost:8081/GetSessionAliveStatus?groupId=[GROUP_ID]&tableId=[TABLE_ID]");

        res.put("result","000");
        res.put("message","success");
        res.put("tableList",tableList);
        res.put("ApiExamples",ApiExamples);
        return res;
    }

    //http://localhost:8081/CreateTemplateTable?tableId=testing&isQueueTable=true&pushArrayBound=100&pushTableDetailLength=15&subscribedSessionBound=120
    @RequestMapping(path = "/CreateTemplateTable" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map CreateTemplateTable(@RequestParam(value="groupId") String groupId,
                            @RequestParam(value="tableId") String tableId,
                            @RequestParam(value="isQueueTable") boolean isQueueTable,
                            @RequestParam(value="pushArrayBound") int pushArrayBound,
                            @RequestParam(value="pushTableDetailLength") int pushTableDetailLength,
                            @RequestParam(value="subscribedSessionBound") int subscribedSessionBound
                            ) throws IOException
    {
        Map<String,Object> res = new HashMap<>();

        if(rootRepository.findByTableIdAndGroupId(tableId,groupId) != null)
        {
            res.put("result","001");
            res.put("message","table exists");
            return res;
        }

        Root newRoot = new RootTableCreator().CreateNewTable(groupId,tableId,isQueueTable,subscribedSessionBound,pushTableDetailLength,pushArrayBound);
        rootRepository.save(newRoot);

        res.put("result","000");
        res.put("message","success");
        return res;
    }

    @RequestMapping(path = "/DeleteTable" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Delete(@RequestParam(value="groupId") String groupId,
                @RequestParam(value="tableId") String tableId) throws InterruptedException, IOException {

        Map<String,Object> res = new HashMap<>();

        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        rootRepository.delete(mObject);

        res.put("result","000");
        res.put("message","table removed successfully");
        return res;
    }

    @RequestMapping(path = "/ReadTable{optionalParameter}" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Read(@PathVariable(name = "optionalParameter") String _optionalParameter,@RequestParam(value="groupId") String _groupId,@RequestParam(value="tableId") String _tableId) {

        Map<String,Object> res = new HashMap<>();

        Root mObject = rootRepository.findByTableIdAndGroupId(_tableId,_groupId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        res.put("result","000");
        res.put("lastUpdateTime",mObject.lastUpdateTime);
        res.put("detail",new Gson().fromJson(mObject.detail,Object.class));
        if(_optionalParameter.equals("Superior"))
        {
            res.put("privateDetail",mObject.privateDetail);
            res.put("isQueueTable",mObject.isQueueTable);
            res.put("pushTableDetailLength",mObject.pushTableDetailLength);
            res.put("pushArrayBound",mObject.pushArrayBound);
        }

        return res;
    }


    //更改單一欄位，使用名稱Patch
    @SuppressWarnings("unchecked")
    @RequestMapping(path = "/Patch{optionalParameter}" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Patch(@PathVariable(name = "optionalParameter") String _optionalParameter,@RequestBody String _req) throws InterruptedException, IOException {

        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();
        JsonObject req = gson.fromJson(_req,JsonObject.class);

        String tableId = req.get("tableId").getAsString();
        String groupId = req.get("groupId").getAsString();
        Root mObject = rootRepository.findByTableIdAndGroupId(tableId,groupId);
        if(mObject == null)
        {
            mObject = new RootTableCreator().CreateNewTable(groupId,tableId,false,100,100,200);
        }

        if(mObject.isQueueTable)
        {
            res.put("result","002");
            res.put("message","can't patch a queue table");
            return res;
        }

        HashMap<String,Object> detail;

        //如果沒填值自動判斷為Public
        if(_optionalParameter.trim().isEmpty())
        {
            _optionalParameter = "Public";
        }

        switch (_optionalParameter)
        {
            case "Public":
                detail = gson.fromJson(mObject.detail,HashMap.class);
                for(String keyName : req.keySet())
                {
                    if(keyName.equals("tableId") || keyName.equals("groupId")) continue;
                    detail.put(keyName,gson.fromJson(req.get(keyName).toString(),Object.class));
                }
                mObject.detail = gson.toJson(detail);
                break;
            case "Private":
                detail = gson.fromJson(mObject.privateDetail,HashMap.class);
                for(String keyName : req.keySet())
                {
                    if(keyName.equals("tableId") || keyName.equals("groupId")) continue;
                    detail.put(keyName,gson.fromJson(req.get(keyName).toString(),Object.class));
                }
                mObject.privateDetail = gson.toJson(detail);
                break;
            default:
                res.put("result","003");
                res.put("message","path parameter is wrong.");
                return res;
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
                    msg.put("messageType","PATCH_MESSAGE");
                    msg.put("groupId",groupId);
                    msg.put("tableId",tableId);
                    msg.put("detail",detail);
                    msg.put("lastUpdateTime",mObject.lastUpdateTime);
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
        res.put("detail",detail);
        res.put("lastUpdateTime",mObject.lastUpdateTime);
        res.put("message","success");
        return res;
    }


    //===============以上為基礎的CRUD功能，以下則為可斟酌使用的API==========
    //檢查session是否還存活，如果已經死亡則刪除
    @RequestMapping(path = "/GetSessionAliveStatus" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map GetConnectionStatus(@RequestParam(value="groupId") String _groupId,
                            @RequestParam(value="tableId") String _tableId) {

        Map<String,Object> res = new HashMap<>();

        Root mObject = rootRepository.findByTableIdAndGroupId(_tableId,_groupId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        ArrayList<String> removeList = new ArrayList<>();
        Map<String,Boolean> status = new HashMap<>();
        for (String sessionId : mObject.sessionIds)
        {
            if(SocketHandler.sessionMap.containsKey(sessionId))
            {
                if(SocketHandler.sessionMap.get(sessionId).isOpen())
                {
                    status.put(sessionId,true);
                }
                else
                {
                    status.put(sessionId,false);
                    SocketHandler.sessionMap.remove(sessionId);
                    removeList.add(sessionId);
                }
            }
            else
            {
                status.put(sessionId,false);
                removeList.add(sessionId);
            }
        }

        mObject.sessionIds.removeAll(removeList);
        rootRepository.save(mObject);
        res.put("status",status);
        return res;
    }


//    //實驗性質，看能不能做到路徑直接讀取
//    @RequestMapping(path = "/Read/{tableId}_{optionalParameter}" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
//    public @ResponseBody
//    Object DirectRead(@PathVariable(name = "tableId") String _tableId,@PathVariable(name = "optionalParameter") String _optionalParameter) {
//
//        Map<String,Object> res = new HashMap<>();
//
//        Root mObject = rootRepository.findByTableId(_tableId);
//        if(mObject == null)
//        {
//            res.put("result","001");
//            res.put("message","table not exists");
//            return res;
//        }
//
//        if(_optionalParameter.trim().isEmpty())
//        {
//            return mObject.detail;
//        }
//
//        String[] elements = _optionalParameter.split("\\.");
//        return ReadRecursive(new Gson(),mObject.detail,elements,0);
//    }
//
//    @SuppressWarnings("unchecked")
//    private Object ReadRecursive(Gson _gson,Object _detail,String[] _elements,int _depth) {
//        int nextDepth = _depth + 1;
//        Object tempDetail;
//        HashMap<String, Object> detail = _gson.fromJson(_detail.toString(), HashMap.class);
//        if (detail.containsKey(_elements[_depth]))
//        {
//            tempDetail = detail.get(_elements[_depth]);
//        }
//        else
//        {
//            return null;
//        }
//
//        if (nextDepth == _elements.length)
//        {
//            return tempDetail;
//        }
//        else
//        {
//            return ReadRecursive(_gson, tempDetail, _elements, nextDepth);
//        }
//    }

}
