package com.kfc.kfconeone.gcpStorage;

import org.springframework.web.bind.annotation.RestController;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController     //必加Annotation，告知Spring這個Class是Controller
public class storageController {

    @RequestMapping(path = "/UploadPicture" , method = RequestMethod.POST)   //建立URI，也可以放在class前面
    public @ResponseBody
    Map UploadPicture(@RequestBody byte[] _req) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Map<String,Object> res = new HashMap<>();
        BlobInfo blobInfo =
                storage.create(
                        BlobInfo.newBuilder("zeso-corp-gcp02-bucket", "NewYear_Slot_Wild.png")
                                // Modify access list to allow all users with link to read file
                                .setAcl(new ArrayList<>(Arrays.asList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
                                .build(),_req, Storage.BlobTargetOption.doesNotExist());


        res.put("result","000");
        res.put("message","success");
        res.put("url",blobInfo.getMediaLink());
        return res;
    }
}
