package com.javaawesome.tag;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PlayerDao {

    @Query("Select * FROM playerlocal WHERE username=:username")
    PlayerLocal getPlayerByUsername(String username);

    @Insert
    void addPlayer(PlayerLocal player);

    @Update
    void updatePlayer(PlayerLocal player);

    @Delete
    void deletePlayer(PlayerLocal player);
}
