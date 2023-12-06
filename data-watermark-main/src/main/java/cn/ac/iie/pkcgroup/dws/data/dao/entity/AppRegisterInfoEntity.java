package cn.ac.iie.pkcgroup.dws.data.dao.entity;

import javax.persistence.*;

@Entity
@Table(name = "app_register_info")
public class AppRegisterInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "system_id", nullable = false, length = 64)
    private String systemId;

    @Column(name = "system_auth_key", nullable = false, length = 64)
    private String systemAuthKey;

    @Column(name = "system_name", nullable = false)
    private String systemName;

    @Lob
    @Column(name = "system_nickname")
    private String systemNickname;

    @Column(name = "functions")
    private Integer functions;

    @Column(name = "dw_usage")
    private Integer dwUsage;

    @Column(name = "db_name")
    private String dbName;

    @Column(name = "db_ip", length = 32)
    private String dbIp;

    @Column(name = "db_port", length = 8)
    private String dbPort;

    @Column(name = "db_type", length = 32)
    private String dbType;

    @Column(name = "secret_seed", length = 128)
    private String secretSeed;

    public String getSecretSeed() {
        return secretSeed;
    }

    public void setSecretSeed(String secretSeed) {
        this.secretSeed = secretSeed;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbIp() {
        return dbIp;
    }

    public void setDbIp(String dbIp) {
        this.dbIp = dbIp;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public Integer getDwUsage() {
        return dwUsage;
    }

    public void setDwUsage(Integer dwUsage) {
        this.dwUsage = dwUsage;
    }

    public Integer getFunctions() {
        return functions;
    }

    public void setFunctions(Integer functions) {
        this.functions = functions;
    }

    public String getSystemNickname() {
        return systemNickname;
    }

    public void setSystemNickname(String systemNickname) {
        this.systemNickname = systemNickname;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemAuthKey() {
        return systemAuthKey;
    }

    public void setSystemAuthKey(String systemAuthKey) {
        this.systemAuthKey = systemAuthKey;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}