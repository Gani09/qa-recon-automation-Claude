package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.engine.ReconciliationEngine;
import com.fiserv.optis.qarecon.model.MongoProfile;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

public class MongoService {

    @Value("${spring.profiles.active}")
    public String name;

    @Value("${spring.data.mongodb.database}")
    public String dbName;

    private static ReconciliationEngine.Clients mongoClients;
    private static MongoClient targetClient;

    public MongoDatabase getDatabase() {
        MongoService service = new MongoService();
        //  String activeProfile = service.activeProfile;
        //System.out.print(activeProfile+" profile is active");
        // String dbName = service.dbName;
        MongoProfile profile = ReconciliationEngine.Profiles.load(name);
        mongoClients = ReconciliationEngine.Profiles.connect(profile);
        targetClient = mongoClients.target;
        MongoDatabase db = targetClient.getDatabase(dbName);
        return db;
    }

    public List<Document> readCollection(MongoClient mongoClient, String collectionName) {
        String[] parts = collectionName.split("\\.", 2);
        String dbName = parts[0];
        String collName = parts[1];
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> collection = db.getCollection(collName);
        return collection.find().into(new ArrayList<>());
    }

    public void writeCollection(String collectionName, Document document) {
        MongoDatabase db = getDatabase();
        MongoCollection<Document> collection = db.getCollection(collectionName);
        InsertOneResult insertOneResult = collection.insertOne(document);
    }
}