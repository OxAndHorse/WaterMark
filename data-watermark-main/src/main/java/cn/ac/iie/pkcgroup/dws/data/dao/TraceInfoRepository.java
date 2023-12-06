package cn.ac.iie.pkcgroup.dws.data.dao;

import cn.ac.iie.pkcgroup.dws.data.dao.entity.TraceInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceInfoRepository extends JpaRepository<TraceInfoEntity, Integer> {
    List<TraceInfoEntity> findAllBySystemIdAndDbNameAndTableName(String systemId, String dbName, String tableName);
}
