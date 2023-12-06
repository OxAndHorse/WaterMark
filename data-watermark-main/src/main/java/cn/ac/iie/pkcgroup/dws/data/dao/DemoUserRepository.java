package cn.ac.iie.pkcgroup.dws.data.dao;

import cn.ac.iie.pkcgroup.dws.data.dao.entity.DemoUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoUserRepository extends JpaRepository<DemoUserEntity, String> {
    DemoUserEntity findFirstById(String id);
}
