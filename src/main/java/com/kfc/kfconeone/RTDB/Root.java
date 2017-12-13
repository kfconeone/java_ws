package com.kfc.kfconeone.RTDB;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;

public class Root {
    @Id
    public String id;

    public Object detail;
    public ArrayList<Object> pushArray;
    String tableId;
    public ArrayList<String> sessionIds = new ArrayList<>();
}
