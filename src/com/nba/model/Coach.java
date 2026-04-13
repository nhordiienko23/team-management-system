package com.nba.model;

public class Coach extends Staff{
    private int experienceYears;
    private int championshipsWon;
    public Coach(String name, double baseSalary, int experienceYears, int championshipsWon) {
        super(name, baseSalary);
        this.experienceYears = experienceYears;
        this.championshipsWon = championshipsWon;
    }

    // The coach receives 5% for each year of experience + a fixed reward for each ring (championship)
    @Override
    public double calculateBonus(){
        double expBonus = getBaseSalary() * (experienceYears * 0.05);
        double ringBonus = championshipsWon * 50000;
        return expBonus + ringBonus;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Role: Coach | Exp: %d yrs | Rings: %d | Bonus: $%,.0f",
                experienceYears, championshipsWon, calculateBonus());
    }

    public int getChampionshipsWon() {
        return championshipsWon;
    }

    public void setChampionshipsWon(int championshipsWon) {
        if(this.championshipsWon<championshipsWon){
            this.championshipsWon = championshipsWon;
        }else {
            System.out.println("To update Championships Won, number must be bigger than previous number");
        }

    }

    public void setExperienceYears(int experienceYears) {
        if(this.experienceYears<experienceYears){
            this.experienceYears = experienceYears;
        }else {
            System.out.println("To update Experience Year, number must be bigger than previous number");
        }
    }

    public int getExperienceYears() {
        return experienceYears;
    }
}
