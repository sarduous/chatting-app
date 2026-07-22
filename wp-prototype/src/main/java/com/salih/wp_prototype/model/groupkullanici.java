
package com.salih.wp_prototype.model;

import jakarta.persistence.*;

@Entity
@Table(name = "grup_kullanicilari")
public class groupkullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupId;
    private String username;

    public groupkullanici() {
    }

    public groupkullanici(Long groupId, String username) {
        this.groupId = groupId;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}