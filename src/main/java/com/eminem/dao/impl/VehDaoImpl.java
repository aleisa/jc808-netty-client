package com.eminem.dao.impl;

import com.eminem.entity.VehicleInfo;
import org.springframework.stereotype.Repository;


import java.util.List;

/**
 * Created by 王欣宇 on 2017/11/21.
 */
@Repository
public class VehDaoImpl extends BaseMongoDaoImpl<VehicleInfo> {

    public List<VehicleInfo> findVehList(){
        return findAll();
    }

    public void insert(VehicleInfo vehicleInfo){
        insert(vehicleInfo,"VehicleInfo");
    }


}
