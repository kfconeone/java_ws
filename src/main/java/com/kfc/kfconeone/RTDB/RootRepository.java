package com.kfc.kfconeone.RTDB;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface RootRepository extends MongoRepository<Root,String> {
    Root findByTableId(String tableId);
}
