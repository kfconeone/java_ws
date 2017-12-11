package com.kfc.kfconeone.RTDB;

import org.springframework.data.annotation.Id;

import java.time.ZonedDateTime;
import java.util.ArrayList;

public class Root {
    @Id
    public String id;

    public Object detail;
    String tableId;
    ArrayList<String> sessionIds = new ArrayList<>();
}
