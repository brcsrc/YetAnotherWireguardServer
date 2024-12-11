package com.brcsrc.yaws.persistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class User {

    @Id
    private String userName;

    private String password;
}
