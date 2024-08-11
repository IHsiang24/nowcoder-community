package com.xiangkai.community.model.renum;

public enum EventType {
    COMMENT("COMMENT"),
    LIKE("LIKE"),
    FOLLOW("FOLLOW");

    private String type;

    EventType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}