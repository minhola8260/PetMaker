package com.example.petmaker.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.List;

@Entity(tableName = "pets")
public class PetEntity {
    @PrimaryKey(autoGenerate = true)
    private int id = 0;
    
    private String name;
    private String description;
    private String personality;
    private List<String> traits;
    private String weather;
    private double temperature;
    private String timezone;
    private String location;
    private long timestamp;
    private String imagePath;

    // Default constructor for Room
    public PetEntity() {
    }

    public PetEntity(String name, String description, String personality, List<String> traits,
                     String weather, double temperature, String timezone, String location,
                     long timestamp, String imagePath) {
        this.name = name;
        this.description = description;
        this.personality = personality;
        this.traits = traits;
        this.weather = weather;
        this.temperature = temperature;
        this.timezone = timezone;
        this.location = location;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }

    public List<String> getTraits() { return traits; }
    public void setTraits(List<String> traits) { this.traits = traits; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
