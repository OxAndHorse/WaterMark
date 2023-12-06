package cn.ac.iie.pkcgroup.dws.data.dao.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "trace_info", schema = "dw", catalog = "")
public class TraceInfoEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "system_id")
    private String systemId;
    @Basic
    @Column(name = "db_name")
    private String dbName;
    @Basic
    @Column(name = "table_name")
    private String tableName;
    @Basic
    @Column(name = "wm_fields")
    private String wmFields;
    @Basic
    @Column(name = "watermark")
    private String watermark;
    @Basic
    @Column(name = "embedded_msg")
    private String embeddedMsg;
    @Basic
    @Column(name = "record_time")
    private Timestamp recordTime;

    @Lob
    @Column(name = "primary_keys")
    private String primaryKeys;
    @Basic
    @Column(name = "allow_row_expansion")
    private Byte allowRowExpansion;
    @Basic
    @Column(name = "row_expansion_algorithm")
    private String rowExpansionAlgorithm;
    @Basic
    @Column(name = "allow_column_expansion")
    private Byte allowColumnExpansion;
    @Basic
    @Column(name = "column_expansion_algorithm")
    private String columnExpansionAlgorithm;

    public String getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(String primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getWmFields() {
        return wmFields;
    }

    public void setWmFields(String wmFields) {
        this.wmFields = wmFields;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public String getEmbeddedMsg() {
        return embeddedMsg;
    }

    public void setEmbeddedMsg(String embeddedMsg) {
        this.embeddedMsg = embeddedMsg;
    }

    public Timestamp getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Timestamp recordTime) {
        this.recordTime = recordTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfoEntity that = (TraceInfoEntity) o;
        return id == that.id && Objects.equals(systemId, that.systemId) && Objects.equals(dbName, that.dbName) && Objects.equals(tableName, that.tableName) && Objects.equals(wmFields, that.wmFields) && Objects.equals(watermark, that.watermark) && Objects.equals(embeddedMsg, that.embeddedMsg) && Objects.equals(recordTime, that.recordTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, systemId, dbName, tableName, wmFields, watermark, embeddedMsg, recordTime);
    }

    public Byte getAllowRowExpansion() {
        return allowRowExpansion;
    }

    public void setAllowRowExpansion(Byte allowRowExpansion) {
        this.allowRowExpansion = allowRowExpansion;
    }

    public String getRowExpansionAlgorithm() {
        return rowExpansionAlgorithm;
    }

    public void setRowExpansionAlgorithm(String rowExpansionAlgorithm) {
        this.rowExpansionAlgorithm = rowExpansionAlgorithm;
    }

    public Byte getAllowColumnExpansion() {
        return allowColumnExpansion;
    }

    public void setAllowColumnExpansion(Byte allowColumnExpansion) {
        this.allowColumnExpansion = allowColumnExpansion;
    }

    public String getColumnExpansionAlgorithm() {
        return columnExpansionAlgorithm;
    }

    public void setColumnExpansionAlgorithm(String columnExpansionAlgorithm) {
        this.columnExpansionAlgorithm = columnExpansionAlgorithm;
    }
}
