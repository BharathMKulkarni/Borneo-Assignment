package com.example.BorneoAssignment.Model;

import java.io.Serializable;

public class FileItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private String name;

    private String webContentLink;
    private String content;

    public String getWebContentLink() {
        return webContentLink;
    }

    public void setWebContentLink(String webContentLink) {
        this.webContentLink = webContentLink;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

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
}
