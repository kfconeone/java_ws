package com.kfc.kfconeone.chatRoom;

import com.google.gson.Gson;
import com.kfc.kfconeone.RTDB.Root;
import com.kfc.kfconeone.RTDB.RootRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController     //必加Annotation，告知Spring這個Class是Controller
public class ChatRoomController {

    private RootRepository rootRepository;
    @Autowired // Spring推薦做法
    public ChatRoomController(RootRepository _userRepository)
    {
        rootRepository = _userRepository;
    }


}
