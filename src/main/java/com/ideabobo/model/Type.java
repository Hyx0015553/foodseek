package com.ideabobo.model;

import java.io.Serializable;

public class Type implements Serializable {
    private Integer id;

    private String title;

    /** 所属店铺 ID（可选） */
    private Integer ownid;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public Integer getOwnid() {
        return ownid;
    }

    public void setOwnid(Integer ownid) {
        this.ownid = ownid;
    }
}