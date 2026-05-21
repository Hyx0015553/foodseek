package com.ideabobo.service;

import com.ideabobo.model.ShopQa;

public interface ShopQaMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ShopQa record);

    int insertSelective(ShopQa record);

    ShopQa selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ShopQa record);

    int updateByPrimaryKey(ShopQa record);
}

