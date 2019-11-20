package com.javaawesome.tag;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PlayerLocal {
    @PrimaryKey(autoGenerate = true)
    private long localId;
    private String id;
    private String username;

    public PlayerLocal(String username, String id) {
        this.username = username;
        this.id = id;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
