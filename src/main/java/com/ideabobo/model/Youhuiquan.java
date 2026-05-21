package com.ideabobo.model;

import java.io.Serializable;

public class Youhuiquan implements Serializable {
    private Integer id;

    /** 优惠金额(元) */
    private Integer total;

    /** 过期时间 Unix 秒 */
    private Integer extime;

    private Integer sid;

    private Integer uid;

    /** 1=券模板 2=用户领取券 */
    private Integer typeid;

    /** 满减门槛(元) */
    private Integer fulluse;

    private String ndate;

    private String username;

    /** 用户券状态码，字典 coupon_state */
    private Integer state;

    /** 模板券父 ID */
    private Integer pid;

    /** 模板库存 */
    private Integer kucun;

    private String note;

    /** 0=上架 1=已删除(软删) */
    private Integer deleted;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getExtime() {
        return extime;
    }

    public void setExtime(Integer extime) {
        this.extime = extime;
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getTypeid() {
        return typeid;
    }

    public void setTypeid(Integer typeid) {
        this.typeid = typeid;
    }

    public Integer getFulluse() {
        return fulluse;
    }

    public void setFulluse(Integer fulluse) {
        this.fulluse = fulluse;
    }

    public String getNdate() {
        return ndate;
    }

    public void setNdate(String ndate) {
        this.ndate = ndate == null ? null : ndate.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Integer getKucun() {
        return kucun;
    }

    public void setKucun(Integer kucun) {
        this.kucun = kucun;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
