package com.ideabobo.service;

import com.ideabobo.model.Snotice;

public interface SnoticeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Snotice record);

    int insertSelective(Snotice record);

    Snotice selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Snotice record);

    int updateByPrimaryKey(Snotice record);
}