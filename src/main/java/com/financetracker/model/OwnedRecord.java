package com.financetracker.model;

public interface OwnedRecord {
    Long getId();

    String getOwnerId();

    void setOwnerId(String ownerId);
}
