package com.example.eventsapp;

import com.google.firebase.firestore.Exclude;

public class Users {
    private String id;
    private String name;
    private String email;
    private String password;
    private int phoneNumber;

    // No-argument constructor required for Firestore
    public Users() {}

    public Users(String name, String email, String password, int phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean login(String email, String password) {
        return email != null && email.equals(this.email) && password != null && password.equals(this.password);
    }
}
