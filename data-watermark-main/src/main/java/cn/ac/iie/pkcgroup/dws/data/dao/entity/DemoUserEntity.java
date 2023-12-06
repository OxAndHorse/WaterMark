package cn.ac.iie.pkcgroup.dws.data.dao.entity;

import javax.persistence.*;

@Entity
@Table(name = "demo_user")
public class DemoUserEntity {
    @Id
    @Column(name = "username", nullable = false)
    private String id;

    @Column(name = "uid", length = 64)
    private String uid;

    @Lob
    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "password", length = 64)
    private String password;

    @Column(name = "role")
    private Integer role;

    @Column(name = "system_id", length = 64)
    private String systemId;

    @Lob
    @Column(name = "remark")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}