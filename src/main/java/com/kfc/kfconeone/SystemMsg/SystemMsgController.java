package com.kfc.kfconeone.SystemMsg;
import com.kfc.kfconeone.RTDB.RootRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


// ================說明===============
// 這裡主要是處理WebSocket直接通知使用者訊息的一些接口，
// 此處說明Server-Client端的互動模式：
// 1.使用者進行WS連線。
// 2.連線成功後會訂閱自己的SystemMsg的桌子，此時取得桌子上所有的訊息，
// 如果桌子不存在就直接創造一個，並且用自己的帳號作為key。
// 3.每個訊息都有自己的欄位，並且必定包含一個long型態的dateTime。
// 4.Server有特定訊息要通知則直接寫入欄位，並且更新dateTime。
//
@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class SystemMsgController {

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public SystemMsgController(RootRepository _rootRepository)
    {
        rootRepository = _rootRepository;
    }

}
