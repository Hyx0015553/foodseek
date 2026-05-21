package com.ideabobo.model;

import java.io.Serializable;

public class Replay implements Serializable {
    private Integer id;

    /** 商品/菜品 ID */
    private Integer pid;

    private String note;

    private Integer uid;

    private String username;

    private String ndate;

    private Integer type;

    private String pf;

    private String img;

    /**
     * 0/null：正常展示；1：商家在「评价管理」列表中移除显示（非物理删除，顾客端等仍可保留展示逻辑由业务决定）。
     */
    private Integer merchant_delete;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getNdate() {
        return ndate;
    }

    public void setNdate(String ndate) {
        this.ndate = ndate == null ? null : ndate.trim();
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getPf() {
        return pf;
    }

    public void setPf(String pf) {
        this.pf = pf == null ? null : pf.trim();
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img == null ? null : img.trim();
    }

    public Integer getMerchant_delete() {
        return merchant_delete;
    }

    public void setMerchant_delete(Integer merchant_delete) {
        this.merchant_delete = merchant_delete;
    }
}