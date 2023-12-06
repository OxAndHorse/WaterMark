package cn.ac.iie.pkcgroup.dws.data.dao;

import cn.ac.iie.pkcgroup.dws.data.dao.entity.AppRegisterInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppRegisterInfoRepository extends JpaRepository<AppRegisterInfoEntity, Integer> {
    AppRegisterInfoEntity findFirstBySystemId(String systemId);

    List<AppRegisterInfoEntity> findAllBySystemId(String systemId);

    AppRegisterInfoEntity findFirstBySystemIdAndDbName(String systemId, String dbName);

    AppRegisterInfoEntity findFirstBySystemIdAndDbNameAndDbIpAndDbPortAndDbType(String systemId, String dbName, String dbIP, String dbPort, String dbType);
}
