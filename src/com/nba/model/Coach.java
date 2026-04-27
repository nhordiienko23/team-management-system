package com.nba.model;

import com.nba.exception.InvalidStaffDataException;
import jakarta.persistence.*;

@Entity
@Table(name = "coaches")
@PrimaryKeyJoinColumn(name = "staff_id")
public class Coach extends Staff {

    private int experienceYears;
    private int championshipsWon;

    protected Coach() {}

    public Coach(String name, double baseSalary, int experienceYears, int championshipsWon) {
        super(name, baseSalary);
        validateExperienceYear(experienceYears);
        this.experienceYears = experienceYears;
        validateChampionshipsWon(championshipsWon);
        this.championshipsWon = championshipsWon;
    }

    private void validateChampionshipsWon(int championshipsWon) {
        if (championshipsWon < 0) {
            throw new InvalidStaffDataException("Championships Won must be a positive number");
        }
    }

    private void validateExperienceYear(int experienceYears) {
        if (experienceYears <= 0) {
            throw new InvalidStaffDataException("Experience years must be a positive number");
        }
    }

    @Override
    public double calculateBonus() {
        return getBaseSalary() * (experienceYears * 0.05) + (championshipsWon * 50000);
    }

    public int getChampionshipsWon() { return championshipsWon; }

    public void setChampionshipsWon(int championshipsWon) {
        validateChampionshipsWon(championshipsWon);
        this.championshipsWon = championshipsWon;
    }

    public void setExperienceYears(int experienceYears) {
        validateExperienceYear(experienceYears);
        this.experienceYears = experienceYears;
    }

    public int getExperienceYears() { return experienceYears; }

    @Override
    public String toString() {
        return "Coach: " + getName() + " | Salary: $" + getBaseSalary() + " | Experience: " + experienceYears + " years | Championships: " + championshipsWon;
    }
}