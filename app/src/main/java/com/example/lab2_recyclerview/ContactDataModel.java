package com.example.lab2_recyclerview;

public class ContactDataModel {
    private String name;
    private String emailAddress;
    private String phoneNumber;
    private String imageUrl;

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
