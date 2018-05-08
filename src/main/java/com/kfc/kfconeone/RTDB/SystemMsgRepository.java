package com.kfc.kfconeone.RTDB;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SystemMsgRepository extends MongoRepository<SystemMsg,String> {
    SystemMsg findByAccount (String account);
}