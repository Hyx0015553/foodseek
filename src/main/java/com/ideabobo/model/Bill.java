package com.ideabobo.model;

import java.io.Serializable;

public class Bill implements Serializable {
    private Integer id;

    private String gids;

    /** 下单用户名快照（展示用，主数据见 uid） */
    private String user;

    private Integer uid;

    /** 店铺名称快照（展示用，主数据见 sid） */
    private String shop;

    private String ndate;

    private Double total;

    private String gnames;

    private Integer sid;

    private String tel;

    private String note;

    /** 订单状态码，字典 fs_dict.bill_state */
    private Integer state;

    private Double pf;

    private String way;

    private Integer yhqid;

    /** 1=商家于消息通知页移除该单的「新订单提醒」 */
    private Integer merchantMsgHide;

    /** 0/null=在「我的订单」展示；1=用户从订单列表移除（非删单） */
    private Integer user_delete;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGids() {
        return gids;
    }

    public void setGids(String gids) {
        this.gids = gids == null ? null : gids.trim();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user == null ? null : user.trim();
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop == null ? null : shop.trim();
    }

    public String getNdate() {
        return ndate;
    }

    public void setNdate(String ndate) {
        this.ndate = ndate == null ? null : ndate.trim();
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getGnames() {
        return gnames;
    }

    public void setGnames(String gnames) {
        this.gnames = gnames == null ? null : gnames.trim();
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel == null ? null : tel.trim();
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

    public Double getPf() {
        return pf;
    }

    public void setPf(Double pf) {
        this.pf = pf;
    }

    public String getWay() {
        return way;
    }

    public void setWay(String way) {
        this.way = way == null ? null : way.trim();
    }

    public Integer getYhqid() {
        return yhqid;
    }

    public void setYhqid(Integer yhqid) {
        this.yhqid = yhqid;
    }

    public Integer getMerchantMsgHide() {
        return merchantMsgHide;
    }

    public void setMerchantMsgHide(Integer merchantMsgHide) {
        this.merchantMsgHide = merchantMsgHide;
    }

    public Integer getUser_delete() {
        return user_delete;
    }

    public void setUser_delete(Integer user_delete) {
        this.user_delete = user_delete;
    }
}
