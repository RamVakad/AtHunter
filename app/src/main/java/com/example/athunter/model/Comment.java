package com.example.athunter.model;

import com.example.athunter.global.config.AppConfig;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;

import java.util.ArrayList;

public class Comment {

    private String id;
    private String text;
    private Long time;

    public Comment() {

    }

    public Comment(String text, Long time) {
        this.text = text;
        this.time = time;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
