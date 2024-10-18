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

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.csv.Exportable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Entity
// No cache, mutable and critical
@TableGenerator(
        name = "reset_password_code_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "user_password",
        allocationSize = 100)
@Table(name = "user_password_map")
public class UserPasswordDTO implements Serializable, Exportable {
    private Integer id;
    private UserDTO user;
    private Date dateCreated;
    private String password;
    private String encryptedPassword;

    public UserPasswordDTO() {
    }
    
    public UserPasswordDTO(UserDTO user, String encryptedPassword) {
    	this.user = user;
    	this.dateCreated = TimezoneHelper.serverCurrentDate();
    	this.encryptedPassword = encryptedPassword;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "reset_password_code_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_user_id")
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

    @Pattern(regexp=Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES, message="validation.error.password.size,8,40")
    @Transient
    public String getPassword () {
        return password;
    }

    public void setPassword (String password,Integer userMainRoleId) {
        this.password = password;
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(userMainRoleId);
        setEncryptedPassword(JBCrypto.encodePassword(passwordEncoderId, this.password));

    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "new_password", length = 40)
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    private void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @Transient
    public String[] getFieldNames () {
        return new String[]{
                "user",
                "dateCreated",
                "encryptedPassword",
        };
    }

    @Transient
    public Object[][] getFieldValues () {
        return new Object[][]{
                {
                        (user != null ? user.getUserName() : null),
                        dateCreated,
                        encryptedPassword
                }
        };
    }
}
