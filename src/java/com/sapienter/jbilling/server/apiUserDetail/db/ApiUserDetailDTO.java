package com.sapienter.jbilling.server.apiUserDetail.db;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;

@Entity
@IdClass(ApiUserDetailDTO.UserCompanyID.class)
@TableGenerator(
        name = "api_user_details_GEN",
        table = "jbilling_seqs",
        pkColumnName = "user_name",
        valueColumnName = "next_id",
        pkColumnValue = "api_user_details",
        allocationSize = 1
)
@Table(name = "api_user_details", uniqueConstraints = @UniqueConstraint(columnNames={"user_name", "company_id"}))
@Data
public class ApiUserDetailDTO implements Serializable{

    @Column(name = "access_code", unique = true, nullable = false)
    String accessCode;

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Id
    @Column(name = "company_id")
    Integer companyId;

    @Id
    @Column(name = "user_name")
    String userName;

    public static class UserCompanyID implements Serializable{
        Integer companyId;
        String userName;

        public UserCompanyID(){}
        public UserCompanyID(Integer companyId, String userName){
            this.companyId = companyId;
            this.userName = userName;
        }
    }
}
