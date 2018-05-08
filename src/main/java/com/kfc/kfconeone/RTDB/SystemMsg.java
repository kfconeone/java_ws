package com.kfc.kfconeone.RTDB;

import org.springframework.data.annotation.Id;

public class SystemMsg {
    @Id
    public String id;
    public String account;
    public Object importantMsg;
}
