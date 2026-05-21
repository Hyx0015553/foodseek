package com.ideabobo.service;

import com.ideabobo.model.Btype;

public interface BtypeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Btype record);

    int insertSelective(Btype record);

    Btype selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Btype record);

    int updateByPrimaryKey(Btype record);
}