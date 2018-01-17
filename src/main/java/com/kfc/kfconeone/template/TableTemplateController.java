package com.kfc.kfconeone.template;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.Root;
import com.kfc.kfconeone.RTDB.RootRepository;
import com.kfc.kfconeone.SocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
        ApiExamples.add("http://localhost:8081/CreateTemplateTable?tableId=[TABLE_ID]&isQueueTable=true&pushArrayBound=100&pushTableDetailLength=15&subscribedSessionBound=120");
        ApiExamples.add("http://localhost:8081/DeleteTable?tableId=[TABLE_ID]");
        ApiExamples.add("http://localhost:8081/ReadTable?tableId=[TABLE_ID]");
        ApiExamples.add("http://localhost:8081/ReadTableSuperior?tableId=[TABLE_ID]");
        ApiExamples.add("http://localhost:8081/GetSessionAliveStatus?tableId=[TABLE_ID]");

        res.put("result","000");
        res.put("message","success");
        res.put("tableList",tableList);
        res.put("ApiExamples",ApiExamples);
        return res;
    }

    //http://localhost:8081/CreateTemplateTable?tableId=testing&isQueueTable=true&pushArrayBound=100&pushTableDetailLength=15&subscribedSessionBound=120
    @RequestMapping(path = "/CreateTemplateTable" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map CreateTemplateTable(@RequestParam(value="tableId") String tableId,
                            @RequestParam(value="isQueueTable") boolean isQueueTable,
                            @RequestParam(value="pushArrayBound") int pushArrayBound,
                            @RequestParam(value="pushTableDetailLength") int pushTableDetailLength,
                            @RequestParam(value="subscribedSessionBound") int subscribedSessionBound
                            ) throws IOException
    {
        Map<String,Object> res = new HashMap<>();
        Gson gson = new Gson();


        Root newRoot = new Root();

        //直接new Object()會出錯
        newRoot.detail = gson.fromJson("{}",Object.class);
        newRoot.privateDetail = gson.fromJson("{}",Object.class);
        newRoot.pushArray = new ArrayList<>();
        newRoot.tableId = tableId;
        newRoot.isQueueTable = isQueueTable;
        newRoot.subscribedSessionBound = subscribedSessionBound;
        if(isQueueTable)
        {
            newRoot.pushArrayBound = pushArrayBound;
            newRoot.pushTableDetailLength = pushTableDetailLength;
        }

        rootRepository.save(newRoot);


        res.put("result","000");
        res.put("message","success");
        return res;
    }

    @RequestMapping(path = "/DeleteTable" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map Delete(@RequestParam(value="tableId") String tableId) throws InterruptedException, IOException {

        Map<String,Object> res = new HashMap<>();

        Root mObject = rootRepository.findByTableId(tableId);
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
    Map Read(@PathVariable(name = "optionalParameter") String _optionalParameter,@RequestParam(value="tableId") String _tableId) {

        Map<String,Object> res = new HashMap<>();

        Root mObject = rootRepository.findByTableId(_tableId);
        if(mObject == null)
        {
            res.put("result","001");
            res.put("message","table not exists");
            return res;
        }

        res.put("result","000");
        res.put("detail",mObject.detail);
        if(_optionalParameter.equals("Superior"))
        {
            res.put("privateDetail",mObject.privateDetail);
            res.put("isQueueTable",mObject.isQueueTable);
            res.put("pushTableDetailLength",mObject.pushTableDetailLength);
            res.put("pushArrayBound",mObject.pushArrayBound);
        }

        return res;
    }


    //檢查session是否還存活，如果已經死亡則刪除
    @RequestMapping(path = "/GetSessionAliveStatus" , method = RequestMethod.GET)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map GetConnectionStatus(@RequestParam(value="tableId") String _tableId) {

        Map<String,Object> res = new HashMap<>();

        Root mObject = rootRepository.findByTableId(_tableId);
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
}
