package com.sapienter.jbilling.server.customerEnrollment.db;

import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;

@Entity
@TableGenerator(
        name="customer_enrollment_comments_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="customer_notes",
        allocationSize = 100
)
@Table(name = "customer_enrollment_comments")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CustomerEnrollmentCommentDTO implements java.io.Serializable
{
    private int id;
    private String comment;
    private Date creationTime;
    private CustomerEnrollmentDTO customerEnrollment;
    private UserDTO user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_enrollment_id")
    public CustomerEnrollmentDTO getCustomerEnrollment() {
        return customerEnrollment;
    }

    public void setCustomerEnrollment(CustomerEnrollmentDTO customerEnrollment) {
        this.customerEnrollment = customerEnrollment;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_enrollment_comments_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "comment", length = 1000)
    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Column(name = "creation_time", nullable = false, length = 29)
    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    @PrePersist
    protected void onCreate() {
        creationTime = TimezoneHelper.serverCurrentDate();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "CustomerEnrollmentCommentDTO{" +
                "id=" + id +
                ", comment='" + comment + '\'' +
                ", creationTime=" + creationTime +
                ", user=" + user +
                '}';
    }
}
