package com.kfc.kfconeone.RTDB;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;

public class Root {
    @Id
    public String id;

    public Object detail;
    public Object privateDetail;
    public ArrayList<Object> pushArray;
    public String tableId;
    ArrayList<String> sessionIds = new ArrayList<>();
    public boolean isQueueTable = false;
    public int pushArrayBound = 200;
    public int pushTableDetailLength = 10;
    public int subscribedSessionBound = 100;
}
