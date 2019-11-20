package com.javaawesome.tag;

import androidx.room.Database;
import androidx.room.RoomDatabase;


@Database(entities = {PlayerLocal.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PlayerDao playerDao();
}
