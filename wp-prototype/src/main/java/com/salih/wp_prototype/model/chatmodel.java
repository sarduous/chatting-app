package com.salih.wp_prototype.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Messages")
public class chatmodel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chatRoomId;
    private String user_sender;
    private String text;
    private String timestamp;

    public chatmodel() {
    }

    public Long getId() {
        return id;
    }

    public String getUser_sender() {
        return user_sender;
    }

    public String getText() {
        return text;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser_sender(String user_sender) {
        this.user_sender = user_sender;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}