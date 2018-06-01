package com.kfc.kfconeone.RTDB;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;

public class Root {
    @Id
    public String id;

    public Object detail;
    public Object privateDetail;
    public ArrayList<Object> pushArray;
    //加入groupId的原因主要是因為分類
    //例如：
    //A與B進行溝通，此溝通屬於聊天室溝通
    //groupId取名為chatRoom,tableId則取名為AToB
    //A與B進行單人BlackJack遊戲
    //groupId取名為Game_BlackJack，tableId則依然可以取名為AToB(但名稱最好還是要更明確)
    public String groupId;
    public String tableId;
    public ArrayList<String> sessionIds = new ArrayList<>();
    public boolean isQueueTable = false;
    public int pushArrayBound = 200;
    public int pushTableDetailLength = 10;
    public int subscribedSessionBound = 100;
    public long lastUpdateTime;
}
