package com.nba.service;

import com.nba.model.Staff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamManager {
    private Map<Integer, Staff> team;

    public TeamManager() {
        this.team = new HashMap<>();
    }

    public void addStaff(Staff staff) {
        team.put(staff.getId(), staff);
    }

    public Staff getStaffById(int id) {
        return team.get(id);
    }

    public boolean removeStaff(int id) {
        if (team.containsKey(id)) {
            team.remove(id);
            return true;
        }
        return false;
    }

    public List<Staff> getAllStaff() {
        return new ArrayList<>(team.values());
    }

    public List<Staff> getHighestPaidStaff() {
        List<Staff> topEarned =new ArrayList<>();
        double highestSalary = -1;
        for (Staff staffCurrent : team.values()) {
            double currentTotal = staffCurrent.calculateTotalSalary();
            if (currentTotal > highestSalary) {

                highestSalary = currentTotal;
                topEarned.clear();
                topEarned.add(staffCurrent);
            }else if(currentTotal == highestSalary){
                topEarned.add(staffCurrent);
            }
        }
        return topEarned;
    }

}
