package com.kfc.kfconeone.template;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.Root;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class RootTableCreator {
    public Root CreateNewTable(String _groupId,String _tableId,boolean _isQueue,int _subscribedSessionBound,int _pushTableDetailLength,int _pushArrayBound)
    {
        Root newRoot = new Root();

        //直接new Object()會出錯
        Gson gson = new Gson();
        newRoot.detail = gson.toJson(new HashMap<String,Object>());
        newRoot.privateDetail = gson.toJson(new HashMap<String,Object>());
        newRoot.pushArray = gson.toJson(new ArrayList<>());
        newRoot.groupId = _groupId;
        newRoot.tableId = _tableId;
        newRoot.isQueueTable = _isQueue;
        newRoot.subscribedSessionBound = _subscribedSessionBound;
        newRoot.pushTableDetailLength = _pushTableDetailLength;
        newRoot.pushArrayBound = _pushArrayBound;


        newRoot.lastUpdateTime = ZonedDateTime.now(ZoneOffset.UTC).plusHours(8).toInstant().toEpochMilli();

        return newRoot;
    }
}
