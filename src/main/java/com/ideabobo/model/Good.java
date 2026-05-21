package com.ideabobo.model;

import java.io.Serializable;

public class Good implements Serializable {
    private Integer id;

    private String gname;

    private Double price;

    private String note;

    /** 货架短分类名（展示快照，可与 typeid 对应 fs_type） */
    private String type;

    private String img;

    private Integer typeid;

    private Integer sid;

    /** 店铺名称快照 */
    private String shop;

    /** 展示用月销量（与 xl 可并存，xl 为累计销量） */
    private String mcount;

    /** 商品种类：1单品 2组合 */
    private Integer btype;

    /** 上架状态码，字典 fs_dict.good_state */
    private Integer state;

    /** 营销标签：热销/招牌等 */
    private String stype;

    private Integer ppid;

    private String pptitle;

    /** 细分类名称快照（可与 ctypeid 对应 fs_type2） */
    private String ctype;

    private Integer ctypeid;

    private Integer xl;

    private Double pf;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGname() {
        return gname;
    }

    public void setGname(String gname) {
        this.gname = gname == null ? null : gname.trim();
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img == null ? null : img.trim();
    }

    public Integer getTypeid() {
        return typeid;
    }

    public void setTypeid(Integer typeid) {
        this.typeid = typeid;
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop == null ? null : shop.trim();
    }

    public String getMcount() {
        return mcount;
    }

    public void setMcount(String mcount) {
        this.mcount = mcount == null ? null : mcount.trim();
    }

    public Integer getBtype() {
        return btype;
    }

    public void setBtype(Integer btype) {
        this.btype = btype;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getStype() {
        return stype;
    }

    public void setStype(String stype) {
        this.stype = stype == null ? null : stype.trim();
    }

    public Integer getPpid() {
        return ppid;
    }

    public void setPpid(Integer ppid) {
        this.ppid = ppid;
    }

    public String getPptitle() {
        return pptitle;
    }

    public void setPptitle(String pptitle) {
        this.pptitle = pptitle == null ? null : pptitle.trim();
    }

    public String getCtype() {
        return ctype;
    }

    public void setCtype(String ctype) {
        this.ctype = ctype == null ? null : ctype.trim();
    }

    public Integer getCtypeid() {
        return ctypeid;
    }

    public void setCtypeid(Integer ctypeid) {
        this.ctypeid = ctypeid;
    }

    public Integer getXl() {
        return xl;
    }

    public void setXl(Integer xl) {
        this.xl = xl;
    }

    public Double getPf() {
        return pf;
    }

    public void setPf(Double pf) {
        this.pf = pf;
    }
}
