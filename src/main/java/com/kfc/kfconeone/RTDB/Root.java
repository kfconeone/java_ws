package com.kfc.kfconeone.RTDB;

import com.google.gson.JsonObject;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;

public class Root {
    @Id
    public String id;

    public Object detail;
    String tableId;
    ArrayList sessionIds = new ArrayList();
}
