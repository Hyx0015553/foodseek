package com.ideabobo.service;

import com.ideabobo.model.Mgc;

public interface MgcMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Mgc record);

    int insertSelective(Mgc record);

    Mgc selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Mgc record);

    int updateByPrimaryKey(Mgc record);
}