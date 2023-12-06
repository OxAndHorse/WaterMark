package cn.ac.iie.pkcgroup.dws.data.dao;

import cn.ac.iie.pkcgroup.dws.data.dao.entity.NumericOptAuxInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository(value = "numericOptAuxInfoRepository")
public interface NumericOptAuxInfoRepository extends JpaRepository<NumericOptAuxInfoEntity, Integer> {
    public NumericOptAuxInfoEntity findFirstBySystemIdAndDbNameAndTableNameAndWmField(String systemId, String dbName, String tableName, String wmField);
}
