package com.eminem.dao;

import java.util.List;
import java.util.Map;

import com.eminem.util.Page;
import com.mongodb.WriteResult;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Created by 王欣宇 on 2017/11/21.
 */
public interface MongoBase<T> {
    //添加
    public void insert(T object,String collectionName);
    //根据条件查找
    public T findOne(Query query);
    //查找所有
    public List<T> findAll();

    public List<T> findAll(String collectionName);
    //修改
    public WriteResult update(T entity);

    /**
     * 根据条件 更新
     */
    public WriteResult update(Query query, Update update);

    /**
     * 根据条件查询
     */
    public List<T> find(Query query);
    //创建集合
    public void createCollection(String collectionName);
    //根据条件删除
    public void remove(Query query);
    //根据ID查找
    public T findById(String id,String collectionName);
    //分页
    public Page<T> findPage(Page<T> page, Query query);
    //查总数
    public long count(Query query);

}
