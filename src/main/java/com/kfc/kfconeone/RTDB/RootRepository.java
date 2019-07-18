package com.kfc.kfconeone.RTDB;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RootRepository extends MongoRepository<Root,String> {
    //Root findByTableId(String tableId);
    Root findByTableIdAndGroupId(String tableId,String groupId);
}
