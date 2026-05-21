package com.ideabobo.model;

import java.io.Serializable;

public class Shop implements Serializable {
    private Integer id;

    private String sname;

    private String img;

    private String note;

    private String address;

    private String longitude;

    private String latitude;

    private String tel;

    /** 店主用户 ID（fs_user.id） */
    private Integer ownid;

    private Double pf;

    private Integer typeid;

    /** 类型名称快照（展示用，主数据见 typeid → fs_type） */
    private String typecn;

    private String img2;

    /** 展示用副标题/分店名 */
    private String stitle;

    /** 审核状态码，字典 fs_dict.shop_audit */
    private Integer state;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname == null ? null : sname.trim();
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img == null ? null : img.trim();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address == null ? null : address.trim();
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude == null ? null : longitude.trim();
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude == null ? null : latitude.trim();
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel == null ? null : tel.trim();
    }

    public Integer getOwnid() {
        return ownid;
    }

    public void setOwnid(Integer ownid) {
        this.ownid = ownid;
    }

    public Double getPf() {
        return pf;
    }

    public void setPf(Double pf) {
        this.pf = pf;
    }

    public Integer getTypeid() {
        return typeid;
    }

    public void setTypeid(Integer typeid) {
        this.typeid = typeid;
    }

    public String getTypecn() {
        return typecn;
    }

    public void setTypecn(String typecn) {
        this.typecn = typecn == null ? null : typecn.trim();
    }

    public String getImg2() {
        return img2;
    }

    public void setImg2(String img2) {
        this.img2 = img2 == null ? null : img2.trim();
    }

    public String getStitle() {
        return stitle;
    }

    public void setStitle(String stitle) {
        this.stitle = stitle == null ? null : stitle.trim();
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }
}
