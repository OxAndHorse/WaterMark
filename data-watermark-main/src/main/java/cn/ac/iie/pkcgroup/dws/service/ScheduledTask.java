//package cn.ac.iie.pkcgroup.dws.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.DependsOn;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@DependsOn({"tableMap", "reloadConfigService"})
//@Slf4j
//public class ScheduledTask {
//    @Autowired
//    private ReloadConfigService reloadConfigService;
//
//    @Scheduled(cron = "${reloadDuration}")
//    public void reloadConfigTask() {
//        reloadConfigService.reloadConfig();
//    }
//}
