package com.nba.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nba.exception.InvalidStaffDataException;

import java.io.Serializable;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Player.class, name = "player"),
        @JsonSubTypes.Type(value = Coach.class, name = "coach")
})
public abstract class Staff implements Serializable, Taxable {
    private static final long serialVersionUID = 1L;
    private static int idCounter = 1;
    private final int id;
    private String name;
    private double baseSalary;

    public Staff(String name, double baseSalary) {
        this.id = idCounter++;
        validateName(name);
        this.name = name;
        validateBaseSalary(baseSalary);
        this.baseSalary = baseSalary;
    }
    private void validateName(String name){
        if(name ==null || name.isEmpty()){
            throw new InvalidStaffDataException("Name can't be null or empty");
        }
    }
    private void validateBaseSalary(double baseSalary){
        if(baseSalary <=0){
            throw new InvalidStaffDataException("Base salary must be bigger than 0");
        }
    }
    public abstract double calculateBonus();

    public int getId() {
        return id;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateName(name);
        this.name = name;
    }

    public void setBaseSalary(double baseSalary) {
        validateBaseSalary(baseSalary);
        this.baseSalary = baseSalary;
    }

    @Override
    public double calculateTax() {
        return calculateTotalSalary() * TAX_RATE;
    }

    public double calculateTotalSalary(){
        return baseSalary+calculateBonus();
    }
    @Override
    public String toString() {
        double total = baseSalary + calculateBonus();
        return String.format("ID: %03d | Name: %-15s | Total Pay: $%,.0f | Tax: $%,.0f",
                id, name, calculateTotalSalary(), calculateTax());
    }
}
