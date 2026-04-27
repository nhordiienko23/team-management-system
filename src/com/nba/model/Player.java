package com.nba.model;

import com.nba.exception.InvalidStaffDataException;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "players")
@PrimaryKeyJoinColumn(name = "staff_id")
public class Player extends Staff {

    private int rating;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_positions", joinColumns = @JoinColumn(name = "staff_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private Set<Position> positions;

    protected Player() {}

    /**
     * Constructs a new Player instance.
     *
     * @param name          Player's full name
     * @param baseSalary    Player's base salary
     * @param rating        Player's rating (0-100)
     * @param positionArray Varargs of {@link Position} assigned to the player
     */
    public Player(String name, double baseSalary, int rating, Position... positionArray) {
        super(name, baseSalary);
        validatePosition(positionArray);
        this.positions = Set.of(positionArray);
        validateRating(rating);
        this.rating = rating;
    }

    private void validatePosition(Position... positionArray) {
        if (positionArray == null || positionArray.length == 0) {
            throw new InvalidStaffDataException("Player must have at least one position");
        }
    }

    private void validateRating(int rating) {
        if (rating < 0 || rating > 100) {
            throw new InvalidStaffDataException("Rating must be between 0 and 100. Provided: " + rating);
        }
    }

    /**
     * Calculates bonus based on player performance.
     * Players with a rating above 90 receive a 20% salary bonus.
     *
     * @return Calculated bonus amount
     */
    @Override
    public double calculateBonus() {
        return (rating > 90) ? getBaseSalary() * 0.2 : 0;
    }

    public int getRating() { return rating; }

    public void setRating(int rating) {
        validateRating(rating);
        this.rating = rating;
    }

    public Set<Position> getPositions() { return positions; }

    public void setPositions(Set<Position> positions) { this.positions = positions; }

    @Override
    public String toString() {
        return "Player: " + getName() + " | Salary: $" + getBaseSalary() + " | Rating: " + rating + " | Positions: " + positions;
    }
}