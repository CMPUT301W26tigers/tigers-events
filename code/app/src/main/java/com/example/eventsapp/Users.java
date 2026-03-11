package com.example.eventsapp;

public class Users {
    private String name;
    private String email;
    private String password;

    private int phoneNumber;

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

    public Users(String name, String email, String password, int phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public boolean login(String email, String password) {
        return email.equals(this.email) && password.equals(this.password);
    }
}
