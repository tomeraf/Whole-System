package com.halilovindustries.backend.Domain.Discount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OpenDiscount{
    private int id;
    private int percentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Integer> itemIds;

    public OpenDiscount(int id, int percentage, String startDate, String endDate) {
        validDetails(id, percentage, startDate, endDate);
        this.id = id;
        this.percentage = percentage;
        this.startDate = LocalDate.parse(startDate);
        this.endDate = LocalDate.parse(endDate);
        this.itemIds = new ArrayList<>();
    }
    private void validDetails(int id, int percentage, String startDate, String endDate) {
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
        if (percentage < 0 || percentage > 100) throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        if(!(validDates(startDate,endDate))) {  
            throw new IllegalArgumentException("Invalid date range.");
        }   
    }

    public boolean validDates(String sDate,String eDate) {
        try {
            LocalDate s=LocalDate.parse(sDate);
            LocalDate e=LocalDate.parse(eDate);
            if(s.isAfter(e)) return false; // Start date is after end date
            if(e.isBefore(LocalDate.now())) return false; // End date is before current date
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
        
    }
    public void addItemId(int itemId) {
        this.itemIds.add(itemId);
    }
    public void removeItemId(int itemId) {
        this.itemIds.remove(Integer.valueOf(itemId));
    }
    public boolean inTime(LocalDate currentDate) {
        return (currentDate.isAfter(startDate) || currentDate.equals(startDate)) && (currentDate.isBefore(endDate) || currentDate.equals(endDate));
    }
    public boolean isApplicable(int itemId) {
        return itemIds.contains(itemId);
    }
    public void extendDiscount(String newEndDate) {
        this.endDate = LocalDate.parse(newEndDate);
    }
    public int applyDiscount(int itemId,int price) {
        if(inTime(LocalDate.now()) && isApplicable(itemId))  // Assuming itemIds has at least one item for simplicity
            return price - (price * percentage / 100);
         else 
            return price; // No discount applied
    }

    public int getId() {
        return id;
    }
    public int getPercentage() {
        return percentage;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public List<Integer> getItemIds() {
        return itemIds;
    }
    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

}
