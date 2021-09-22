package com.example.lab2_recyclerview;

public class ContactDataModel {
    private final String name;
    private final String emailAddress;
    private final String phoneNumber;
    private final String imageUrl;

    public ContactDataModel(String name, String emailAddress, String phoneNumber, String imageUrl){
        this.name = name;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
