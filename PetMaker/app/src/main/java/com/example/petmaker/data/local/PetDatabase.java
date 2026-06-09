package com.example.petmaker.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {PetEntity.class}, version = 1, exportSchema = false)
@TypeConverters({TraitsConverter.class})
public abstract class PetDatabase extends RoomDatabase {
    public abstract PetDao petDao();

    private static volatile PetDatabase INSTANCE = null;

    public static PetDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PetDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        PetDatabase.class,
                        "pet_maker_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
