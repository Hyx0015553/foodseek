package com.ideabobo.service;

import com.ideabobo.model.Shitang;

public interface ShitangMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Shitang record);

    int insertSelective(Shitang record);

    Shitang selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Shitang record);

    int updateByPrimaryKey(Shitang record);
}