package com.eminem.service;

import com.eminem.common.TPMSConsts;
import com.eminem.dao.impl.VehDaoImpl;
import com.eminem.entity.VehicleInfo;
import com.eminem.vo.PackageData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.eminem.vo.PackageData.MsgHeader;
import java.util.List;

/**
 * Created by 王欣宇 on 2017/11/21.
 */
@Service
public class VehicleService {

    @Autowired
    VehDaoImpl vehDao;

    @Autowired
    TCPServer server;

    public List<VehicleInfo> findVehList(){
       return vehDao.findAll("VehicleInfo");
    }

    public void save(VehicleInfo vehicleInfo){
        vehDao.insert(vehicleInfo);

    }

    void registerVeh(){
        PackageData pd = new PackageData();
        MsgHeader header = new MsgHeader();
        header.setMsgId(TPMSConsts.msg_id_terminal_register);
    }

}
