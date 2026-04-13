package com.nba.model;

import com.nba.exception.InvalidStaffDataException;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Player extends Staff {
    private int rating;
    private Set<Position> positions;

    public Player(String name, double baseSalary, int rating, Position... positionArray) {
        super(name, baseSalary);
        validatePosition(positionArray);
        this.positions = EnumSet.of(positionArray[0], positionArray);
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

    public int getRating() {
        return rating;
    }

    public Set<Position> getPositions() {
        return positions;
    }

    public void setPositions(Position... positionArray) {
        validatePosition(positionArray);
        this.positions = EnumSet.of(positionArray[0], positionArray);
    }

    public void setRating(int rating) {
        validateRating(rating);
        this.rating = rating;
    }

    //If the rating is > 90, the bonus for player is 20% of the salary.
    @Override
    public double calculateBonus() {
        if (rating > 90) {
            return getBaseSalary() * 0.2;
        }
        return 0;
    }

    @Override
    public String toString() {
        // Красиво склеиваем позиции через запятую
        String posString = positions.stream()
                .map(Enum::name)
                .collect(Collectors.joining("/"));

        return super.toString() + String.format(" | Pos: %s | Rating: %d", posString, rating);
    }

}
