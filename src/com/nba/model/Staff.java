package com.nba.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nba.exception.InvalidStaffDataException;
import jakarta.persistence.*;

@Entity
@Table(name = "staff")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Player.class, name = "player"),
        @JsonSubTypes.Type(value = Coach.class, name = "coach")
})
public abstract class Staff implements Taxable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Use Integer for JPA

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double baseSalary;

    protected Staff() {}

    public Staff(@JsonProperty("name") String name, @JsonProperty("baseSalary") double baseSalary) {
        validateName(name);
        this.name = name;
        validateBaseSalary(baseSalary);
        this.baseSalary = baseSalary;
    }

    private void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new InvalidStaffDataException("Name can't be null or empty");
        }
    }

    private void validateBaseSalary(double baseSalary) {
        if (baseSalary <= 0) {
            throw new InvalidStaffDataException("Base salary must be greater than 0");
        }
    }

    public abstract double calculateBonus();

    public Integer getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { validateName(name); this.name = name; }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { validateBaseSalary(baseSalary); this.baseSalary = baseSalary; }

    @Override
    public double calculateTax() { return calculateTotalSalary() * TAX_RATE; }

    public double calculateTotalSalary() { return baseSalary + calculateBonus(); }
}