package com.libraryzone.vo;

import java.time.LocalDate;

public class BorrowingHistoryVo {
    private int transactionId;
    private int serialNumber;
    private String isbn;
    private int borrowerId;
    private LocalDate borrowingDate;
    private LocalDate returnDate;
    private boolean availabilityStatus;

    @Override
    public String toString() {
        return "BorrowingHistory{" +
                "transactionId=" + transactionId +
                ", serialNumber=" + serialNumber +
                ", isbn='" + isbn + '\'' +
                ", borrowerId=" + borrowerId +
                ", borrowingDate=" + borrowingDate +
                ", returnDate=" + returnDate +
                ", availabilityStatus=" + availabilityStatus +
                '}';
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getBorrowerId() {
        return borrowerId;
    }

    public void setBorrowerId(int borrowerId) {
        this.borrowerId = borrowerId;
    }

    public LocalDate getBorrowingDate() {
        return borrowingDate;
    }

    public void setBorrowingDate(LocalDate borrowingDate) {
        this.borrowingDate = borrowingDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(boolean availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
// Getters and setters
}

