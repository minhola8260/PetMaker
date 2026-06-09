package com.example.petmaker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPet(PetEntity pet);

    @Query("SELECT * FROM pets ORDER BY timestamp DESC")
    LiveData<List<PetEntity>> getAllPets();

    @Query("SELECT * FROM pets WHERE id = :id")
    PetEntity getPetById(int id);

    @Delete
    void deletePet(PetEntity pet);
}
