package com.ideabobo.service;

import com.ideabobo.model.Looked;

public interface LookedMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Looked record);

    int insertSelective(Looked record);

    Looked selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Looked record);

    int updateByPrimaryKey(Looked record);
}