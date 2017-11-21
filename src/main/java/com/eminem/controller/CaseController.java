package com.eminem.controller;

import com.eminem.entity.VehicleInfo;
import com.eminem.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by 王欣宇 on 2017/11/21.
 */
@RestController
public class CaseController {

    @Autowired
    VehicleService vehicleService;

    @RequestMapping("findVehInfo")
    public List<VehicleInfo> findVehInfo(){
        return vehicleService.findVehList();
    }

    @RequestMapping("saveVehicle")
    public String saveVehicle(@RequestBody VehicleInfo vehicleInfo){
        vehicleService.save(vehicleInfo);
        return "success";
    }

}
