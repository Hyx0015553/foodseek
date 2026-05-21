package com.ideabobo.model;

import java.io.Serializable;

/**
 * 探店计划
 */
public class Blogplan implements Serializable {
    private Integer id;

    private Integer uid;

    private String username;

    /** 店铺 id */
    private Integer sid;

    /** 店铺名称快照 */
    private String stitle;

    /** 计划到店时间 */
    private String plantime;

    private String note;

    /** 计划状态码，见 fs_dict.blogplan_state */
    private Integer state;

    private String ndate;

    /** 1=已向用户发送「计划到店前第3天」系统通知（fs_system_notify） */
    private Integer remind_3d_sent;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public String getStitle() {
        return stitle;
    }

    public void setStitle(String stitle) {
        this.stitle = stitle == null ? null : stitle.trim();
    }

    public String getPlantime() {
        return plantime;
    }

    public void setPlantime(String plantime) {
        this.plantime = plantime == null ? null : plantime.trim();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getNdate() {
        return ndate;
    }

    public void setNdate(String ndate) {
        this.ndate = ndate == null ? null : ndate.trim();
    }

    public Integer getRemind_3d_sent() {
        return remind_3d_sent;
    }

    public void setRemind_3d_sent(Integer remind_3d_sent) {
        this.remind_3d_sent = remind_3d_sent;
    }
}
