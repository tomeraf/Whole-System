package Domain.User;

import java.time.LocalDateTime;

public class Suspension {
    private LocalDateTime startDate=null;
    private LocalDateTime endDate=null;
    private boolean isPermanent = false;


    public void setSuspension(LocalDateTime startDate, LocalDateTime endDate) {
        validateDates(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }
    public void setSuspension() {
        this.isPermanent = true;
    }
    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public boolean isSuspended(LocalDateTime currentDate) {
        if (isPermanent) {
            return true;
        }
        if (startDate == null || endDate == null) {
            return false;
        }
        return currentDate.isAfter(startDate) && currentDate.isBefore(endDate);
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        if (endDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("End date cannot be in the past.");
        }
    }
    public void removeSuspension() {
        if(startDate == null && endDate == null && !isPermanent) {
            throw new IllegalArgumentException("No suspension to remove.");
        }
        this.startDate = null;
        this.endDate = null;
        this.isPermanent = false;
    }
    public String toString() {
        if(startDate == null && endDate == null && !isPermanent) 
            return "";
        if (isPermanent) {
            return "Suspension: Permanent\n";
        } else {
            return "Suspension: From " + startDate + " to " + endDate+ "\n";
        }
    }


}
