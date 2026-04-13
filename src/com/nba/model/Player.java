package com.nba.model;

public class Player extends Staff {
    private int rating;
    private String position;
    public Player(String name, double baseSalary ,String position,int rating){
        super(name,baseSalary);
        this.position = position;
        this.rating = rating;
    }

    public int getRating() {
        return rating;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setRating(int rating) {
        if(rating>0){
            this.rating = rating;
        }else {
            System.out.println("Rating must be positive number!");
        }
    }

    //If the rating is > 90, the bonus is 20% of the salary.
    @Override
    public double calculateBonus(){
        if(rating>90){
          return getBaseSalary() *0.2;
        }
        return 0;
    }

    @Override
    public String toString(){
        return super.toString() + String.format(" | Pos: %s | Rating: %d | Bonus: $%,.0f",
                position, rating, calculateBonus());
    }

}
