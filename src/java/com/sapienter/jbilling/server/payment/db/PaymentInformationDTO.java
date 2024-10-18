package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.GroupCustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.CharMetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author khobab
 *
 */
@Entity
@TableGenerator(
        name="payment_information_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_information",
        allocationSize = 10
        )
@Table(name = "payment_information")
public class PaymentInformationDTO extends GroupCustomizedEntity implements AutoCloseable,Serializable{
	private Integer id;
	private Integer processingOrder;
	private Integer deleted = 0;
	
	private UserDTO user;
	private PaymentMethodTypeDTO paymentMethodType;
	private List<MetaFieldValue> metaFields = new ArrayList<MetaFieldValue>(0);
	private Integer paymentMethodId;
	private Date createDateTime;
	private Date updateDateTime;

	private int versionNum;
	
	private PaymentMethodDTO paymentMethod;
	
	// transient fields
	boolean blacklisted = false;
	// for worldpay payment
	private char[] cvv;

	public PaymentInformationDTO() {
		// default constructor
	}

    public MetaField fieldNameRetrievalFunction(MetaContent customizedEntity, String name) {
        PaymentMethodTypeDTO type = ((PaymentInformationDTO) customizedEntity).getPaymentMethodType();
        return MetaFieldExternalHelper.findPaymentMethodMetaField(name, type.getId());
    }
	
	public PaymentInformationDTO(Integer processingOrder, UserDTO user, PaymentMethodTypeDTO paymentMethodTye, Integer paymentMethodId) {
		this.processingOrder = processingOrder;
		this.user = user;
		this.paymentMethodType = paymentMethodTye;
		this.paymentMethodId = paymentMethodId;
	}
	
	public PaymentInformationDTO(PaymentInformationWS ws, Integer entityId) {
		if(ws.getId() != null) {
			setId(ws.getId());
		}
		
		setProcessingOrder(ws.getProcessingOrder());
		setPaymentMethodType(new PaymentMethodTypeDAS().find(ws.getPaymentMethodTypeId()));
		setPaymentMethodId(ws.getPaymentMethodId());
		setPaymentMethod(new PaymentMethodDAS().find(ws.getPaymentMethodId()));
		setCreateDateTime(ws.getCreateDateTime());
		setUpdateDateTime(ws.getUpdateDateTime());

		if(ws.getPaymentMethodId() != null) {
			setPaymentMethod(new PaymentMethodDTO(ws.getPaymentMethodId()));
		}
		
		MetaFieldBL.fillMetaFieldsFromWS(entityId, this, ws.getMetaFields());
		setCvv(ws.getCvv());
	}

	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_information_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

	public void setId(Integer id) {
		this.id = id;
	}
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return this.user;
    }

    public void setUser(UserDTO user) {
    	this.user = user;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    public PaymentMethodTypeDTO getPaymentMethodType() {
        return this.paymentMethodType;
    }

    public void setPaymentMethodType(PaymentMethodTypeDTO paymentMethodType){
    	this.paymentMethodType = paymentMethodType;
    }
    
    @Transient
    public PaymentMethodDTO getPaymentMethod() {
    	return this.paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethodDTO paymentMethod) {
    	this.paymentMethod = paymentMethod;
    }
    
    @Column(name = "processing_order")
    public Integer getProcessingOrder() {
    	return processingOrder;
    }
    
    public void setProcessingOrder(Integer processingOrder) {
    	this.processingOrder = processingOrder;
    }
    
    @Column(name = "deleted")
    public Integer getDeleted() {
    	return this.deleted;
    }
    
    public void setDeleted(Integer deleted) {
    	this.deleted = deleted;
    }
    
    @Column(name = "payment_method")
    public Integer getPaymentMethodId() {
		return paymentMethodId;
	}

	public void setPaymentMethodId(Integer paymentMethodId) {
		this.paymentMethodId = paymentMethodId;
	}

	@Column(name="create_datetime",length = 29)
	@CreationTimestamp
	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	@Column(name = "update_datetime", length = 29)
	public Date getUpdateDateTime() {
		return updateDateTime;
	}

	public void setUpdateDateTime(Date updateDateTime) {
		this.updateDateTime = updateDateTime;
	}
	@Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }
    
    public void setVersionNum(int versionNum){
    	this.versionNum = versionNum;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "payment_information_meta_fields_map",
            joinColumns = @JoinColumn(name = "payment_information_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

	@Transient
	public EntityType[] getCustomizedEntityType() {
		return new EntityType[] { EntityType.PAYMENT_METHOD_TYPE };
	}
	
	/**
     * Useful method for updating payment method meta fields with validation before entity saving
     *
     * @param dto dto with new data
     */
    @Transient
    public void updatePaymentMethodMetaFieldsWithValidation(Integer entityId, MetaContent dto) {
        MetaFieldExternalHelper.updatePaymentMethodMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(),
                entityId, getPaymentMethodType().getId(), this, dto);
    }
	
    @Transient
    public boolean isBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
	}

    @Transient
    public String getCvv() {
        return null != this.cvv ? new String(cvv) : StringUtils.EMPTY;
    }

    public void setCvv(char[] cvv) {
        this.cvv = cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv.toCharArray();
    }
	@Transient
    public PaymentInformationDTO getDTO() {
    	PaymentInformationDTO paymentInformation = new PaymentInformationDTO();
		paymentInformation.setId(this.id);
		paymentInformation.setPaymentMethod(this.paymentMethod);
		paymentInformation.setPaymentMethodType(this.paymentMethodType);
		paymentInformation.setProcessingOrder(this.processingOrder);
		paymentInformation.setPaymentMethodId(this.paymentMethodId);
		paymentInformation.setUser(this.user);
		paymentInformation.setCvv(this.getCvv());
		paymentInformation.setCreateDateTime(this.createDateTime);
		paymentInformation.setUpdateDateTime(this.updateDateTime);

		for(MetaFieldValue metaField : getMetaFields()) {
			MetaFieldValue value = metaField.getField().createValue();
			value.setId(metaField.getId());
			value.setValue(metaField.getValue());
			
			paymentInformation.getMetaFields().add(value);
		}
		
		return paymentInformation;
    }
    
	@Transient
	public boolean isNumberObsucred(char[] ccNumber) {
		return ccNumber != null && ccNumber[0] =='*';
	}
	
    @Transient
    public PaymentInformationDTO getSaveableDTO() {
    	PaymentInformationDTO paymentInformation = new PaymentInformationDTO();
		paymentInformation.setPaymentMethod(this.paymentMethod);
		paymentInformation.setPaymentMethodType(this.paymentMethodType);
		paymentInformation.setProcessingOrder(this.processingOrder);
		paymentInformation.setPaymentMethodId(this.paymentMethodId);
		paymentInformation.setCreateDateTime(this.createDateTime);
		paymentInformation.setUpdateDateTime(this.updateDateTime);
		//paymentInformation.setPayments(this.payments);
		//paymentInformation.setUser(this.user);
		
		for(MetaFieldValue metaField : this.getMetaFields()) {
			MetaFieldValue value = metaField.getField().createValue();
			//value.setId(null);
			value.setValue(metaField.getValue());
			
			paymentInformation.getMetaFields().add(value);
		}
		
		return paymentInformation;
    }

    @Transient
    public boolean isMetaFieldEmpty(){
        int count=0;
        for(MetaFieldValue metaField : this.getMetaFields()) {
            if(metaField.isEmpty()){ count++; }
        }
        if(count == this.getMetaFields().size())
            return true;
        else
            return false;
    }

	/**
	 * Closes this resource, relinquishing any underlying resources.
	 * This method is invoked automatically on objects managed by the
	 * {@code try}-with-resources statement.
	 * <p>
	 * <p>While this interface method is declared to throw {@code
	 * Exception}, implementers are <em>strongly</em> encouraged to
	 * declare concrete implementations of the {@code close} method to
	 * throw more specific exceptions, or to throw no exception at all
	 * if the close operation cannot fail.
	 * <p>
	 * <p> Cases where the close operation may fail require careful
	 * attention by implementers. It is strongly advised to relinquish
	 * the underlying resources and to internally <em>mark</em> the
	 * resource as closed, prior to throwing the exception. The {@code
	 * close} method is unlikely to be invoked more than once and so
	 * this ensures that the resources are released in a timely manner.
	 * Furthermore it reduces problems that could arise when the resource
	 * wraps, or is wrapped, by another resource.
	 * <p>
	 * <p><em>Implementers of this interface are also strongly advised
	 * to not have the {@code close} method throw {@link
	 * InterruptedException}.</em>
	 * <p>
	 * This exception interacts with a thread's interrupted status,
	 * and runtime misbehavior is likely to occur if an {@code
	 * InterruptedException} is {@linkplain Throwable#addSuppressed
	 * suppressed}.
	 * <p>
	 * More generally, if it would cause problems for an
	 * exception to be suppressed, the {@code AutoCloseable.close}
	 * method should not throw it.
	 * <p>
	 * <p>Note that unlike the {@link Closeable#close close}
	 * method of {@link Closeable}, this {@code close} method
	 * is <em>not</em> required to be idempotent.  In other words,
	 * calling this {@code close} method more than once may have some
	 * visible side effect, unlike {@code Closeable.close} which is
	 * required to have no effect if called more than once.
	 * <p>
	 * However, implementers of this interface are strongly encouraged
	 * to make their {@code close} methods idempotent.
	 *
	 * @throws Exception if this resource cannot be closed
	 */
	@Override
	public void close() throws Exception {
		for(MetaFieldValue metaFieldValue : metaFields) {
			if(metaFieldValue instanceof CharMetaFieldValue) {
				((CharMetaFieldValue) metaFieldValue).clearValue();
			}
		}
	}
}
