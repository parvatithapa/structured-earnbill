/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.user.db;


import com.sapienter.jbilling.server.util.csv.Exportable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
// No cache, mutable and critical
@Table(name = "reset_password_code")
@TableGenerator(
        name = "reset_pwd_code_field_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "reset_pwd_code_field_GEN",
        allocationSize = 1
)
public class ResetPasswordCodeDTO implements Serializable, Exportable {
    private Integer id;
    private UserDTO user;
    private Date dateCreated;
    private String token;
    private String newPassword;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "reset_pwd_code_field_GEN")
    @Column(name = "id", unique = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_user_id", unique = true)
    public UserDTO getUser () {
        return user;
    }

    public void setUser (UserDTO user) {
        this.user = user;
    }

    @Column(name = "date_created")
    public Date getDateCreated () {
        return dateCreated;
    }

    public void setDateCreated (Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Column(name = "token", length = 32, nullable = false, unique = true)
    public String getToken () {
        return token;
    }

    public void setToken (String token) {
        this.token = token;
    }

    @Column(name = "new_password", length = 40)
    public String getNewPassword () {
        return newPassword;
    }

    public void setNewPassword (String newPassword) {
        this.newPassword = newPassword;
    }

    @Transient
    public String[] getFieldNames () {
        return new String[]{
                "id",
                "user",
                "dateCreated",
                "token",
                "newPassword",
        };
    }

    @Transient
    public Object[][] getFieldValues () {
        return new Object[][]{
                {
                        id,
                        (user != null ? user.getUserName() : null),
                        dateCreated,
                        token,
                        newPassword
                }
        };
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getUser().getCompany().getId())
                .append("-usr-")
                .append(getUser().getId())
                .append("-")
                .append(id);

        return key.toString();
    }
}
