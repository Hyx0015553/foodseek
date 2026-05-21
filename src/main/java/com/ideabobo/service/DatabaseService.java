package com.ideabobo.service;

import com.ideabobo.pojo.Params;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 通用 SQL 访问层，由 MyBatis 根据 mapper/DatabaseMapper.xml 映射为 Spring Bean。
 * 标注 MyBatis 的 Mapper 注解便于 IDE 识别可注入的 Bean（与 GeneratorApplication 的 MapperScan 一致）。
 */
@Mapper
public interface DatabaseService {
	/**
	 * 执行增删改sql语句
	 * 
	 * @author hyx
	 * @param sql sql语句
	 * @return
	 */
	public long executeAction(String sql);
	
	/**
	 * 根据sql语句查询单调数据
	 * 
	 * @author hyx
	 * @param sql sql语句
	 * @return
	 */
	public Map<String, Object> findFirst(String sql);
	
	/**
	 * 根据sql语句查询多调数据
	 * 
	 * @author hyx
	 * @param sql sql语句
	 * @return
	 */
	public List<Map<String, Object>> find(String sql);
	
	/**
	 * 添加实体数据
	 * 
	 * @author hyx
	 * @param params 添加参数类
	 * @return
	 */
	public int addEntity(Params params);
	
	/**
	 * 根据sql语句查询条数
	 * 
	 * @author hyx
	 * @param sql sql语句
	 * @return
	 */
	public long findCount(String sql);
	
	/**
	 * 批量添加
	 * 
	 * @author hyx
	 * @param params 添加参数
	 * @return
	 */
	public int batchAdd(Params params);
	
	/**
	 * 批量删除
	 * 
	 * @author hyx
	 * @param params 参数类
	 * @return
	 */
	public int batchDelete(Params params);
	
	/**
	 * 根据sql语句查询单个值
	 * 
	 * @author hyx
	 * @param sql sql语句
	 * @return
	 */
	public Object findOneValue(String sql);
}
