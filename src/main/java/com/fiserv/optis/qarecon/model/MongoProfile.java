package com.fiserv.optis.qarecon.model;

public final class MongoProfile {

    public String name;
    public String mongoUriLeft;
    public String dbLeft;
    public String mongoUriRight;
    public String dbRight;
    public Integer socketTimeoutMs;
    public String readPreference;
    public String readConcern;

    // Default constructor
    public MongoProfile() {}

    // Full constructor
    public MongoProfile(String name, String mongoUriLeft, String dbLeft,
                        String mongoUriRight, String dbRight,
                        Integer socketTimeoutMs, String readPreference, String readConcern) {
        this.name = name;
        this.mongoUriLeft = mongoUriLeft;
        this.dbLeft = dbLeft;
        this.mongoUriRight = mongoUriRight;
        this.dbRight = dbRight;
        this.socketTimeoutMs = socketTimeoutMs;
        this.readPreference = readPreference;
        this.readConcern = readConcern;
    }
}