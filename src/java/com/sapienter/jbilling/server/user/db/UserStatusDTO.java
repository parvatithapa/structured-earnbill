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

import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@TableGenerator(name = "user_status_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "user_status",
        allocationSize = 100)
@Table(name = "user_status")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserStatusDTO extends AbstractDescription implements java.io.Serializable {

    protected int id;
    private int canLogin;
    private AgeingEntityStepDTO ageingEntityStep;
    private Set<UserDTO> baseUsers = new HashSet<UserDTO>(0);

    public UserStatusDTO() {
    }

    public UserStatusDTO(int canLogin, AgeingEntityStepDTO ageingEntityStep) {
        this.canLogin = canLogin;
        this.ageingEntityStep = ageingEntityStep;
    }

    public UserStatusDTO(int canLogin, AgeingEntityStepDTO ageingEntityStep, Set<UserDTO> baseUsers) {
        this.canLogin = canLogin;
        this.ageingEntityStep = ageingEntityStep;
        this.baseUsers = baseUsers;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_status_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    @Transient
    protected String getTable() {
        return Constants.TABLE_USER_STATUS;
    }
    
    @Column(name="can_login", nullable=false)
    public int getCanLogin() {
        return this.canLogin;
    }
    
    public void setCanLogin(int canLogin) {
        this.canLogin = canLogin;
    }

    @OneToOne(mappedBy = "userStatus", fetch = FetchType.LAZY)
    public AgeingEntityStepDTO getAgeingEntityStep() {
        return this.ageingEntityStep;
    }

    public void setAgeingEntityStep(AgeingEntityStepDTO ageingEntityStep) {
        this.ageingEntityStep = ageingEntityStep;
    }

    @OneToMany( mappedBy = "userStatus", fetch = FetchType.LAZY)
    public Set<UserDTO> getBaseUsers() {
        return this.baseUsers;
    }

    public void setBaseUsers(Set<UserDTO> baseUsers) {
        this.baseUsers = baseUsers;
    }

    @Transient
    public boolean isSuspended() {
        return ageingEntityStep == null || ageingEntityStep.getSuspend() > 0;
    }
}


