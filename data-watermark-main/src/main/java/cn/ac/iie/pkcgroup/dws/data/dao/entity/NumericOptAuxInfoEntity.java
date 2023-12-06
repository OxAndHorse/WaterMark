package cn.ac.iie.pkcgroup.dws.data.dao.entity;

import javax.persistence.*;

@Entity
@Table(name = "numeric_opt_aux_info")
public class NumericOptAuxInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "system_id", nullable = false, length = 32)
    private String systemId;

    @Column(name = "watermark")
    private String watermark;

    @Column(name = "threshold")
    private Double threshold;

    @Column(name = "secret_code")
    private Double secretCode;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "db_name")
    private String dbName;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "wm_field")
    private String wmField;

    @Column(name = "partition_count")
    private Integer partitionCount;

    @Lob
    @Column(name = "embedded_msg")
    private String embeddedMsg;

    public String getEmbeddedMsg() {
        return embeddedMsg;
    }

    public void setEmbeddedMsg(String embeddedMsg) {
        this.embeddedMsg = embeddedMsg;
    }

    public Integer getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(Integer partitionCount) {
        this.partitionCount = partitionCount;
    }

    public String getWmField() {
        return wmField;
    }

    public void setWmField(String wmField) {
        this.wmField = wmField;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Double getSecretCode() {
        return secretCode;
    }

    public void setSecretCode(Double secretCode) {
        this.secretCode = secretCode;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
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