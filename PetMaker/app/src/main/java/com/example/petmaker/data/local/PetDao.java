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

    @Query("SELECT * FROM pets ORDER BY timestamp ASC")
    LiveData<List<PetEntity>> getAllPets();

    @Query("SELECT * FROM pets WHERE id = :id")
    PetEntity getPetById(int id);

    @Query("SELECT name FROM pets")
    List<String> getAllPetNames();

    @Delete
    void deletePet(PetEntity pet);

    // 파트너 펫 조회 (isPartner = true)
    @Query("SELECT * FROM pets WHERE isPartner = 1 LIMIT 1")
    LiveData<PetEntity> getPartnerPetLiveData();

    // 모든 파트너 해제 (isPartner = false 로 초기화)
    @Query("UPDATE pets SET isPartner = 0 WHERE isPartner = 1")
    void clearPartners();
}
