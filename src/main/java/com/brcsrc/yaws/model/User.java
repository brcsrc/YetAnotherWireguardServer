package com.brcsrc.yaws.model;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.SQLInsert;

/**
 * this entity is not conventional, the users table is designed to only have a
 * single row for the 'admin' user. the user should be able to determine what the
 * username and password should be, but we only want 0-1 rows consistently
 */

@Entity
@Table(name = "users")
@Check(constraints = "id = 1")
public class User {

    @Id
    private Long id = 1L;
    private String userName;
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
