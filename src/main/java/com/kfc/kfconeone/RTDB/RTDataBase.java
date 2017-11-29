package com.kfc.kfconeone.RTDB;


import com.google.gson.*;

import java.util.HashMap;
import java.util.StringTokenizer;

public class RTDataBase
{
    private HashMap getDb() {
        return db.get("Root");
    }

    private HashMap<String,HashMap> db;
    public RTDataBase()
    {
        db = new HashMap<>();
        db.put("Root",new HashMap<String,HashMap>());
    }

    public String GetEntireDb()
    {
        return new Gson().toJson(db);
    }


    public String AddElement(String path,String element)
    {
        StringTokenizer tokenizer = new StringTokenizer(path,"/");
        HashMap nodePointer = getDb();

        while(tokenizer.hasMoreTokens()) {
            String tempNode = tokenizer.nextToken();

            if (!nodePointer.containsKey(tempNode)) {
                if (tokenizer.hasMoreTokens()) {
                    nodePointer.put(tempNode, new HashMap<>());

                } else {
                    nodePointer.put(tempNode, element);
                }
            }

            if (tokenizer.hasMoreTokens())
            {
                nodePointer = (HashMap)nodePointer.get(tempNode);
            }
        }
        return new Gson().toJson(nodePointer);
    }


}
