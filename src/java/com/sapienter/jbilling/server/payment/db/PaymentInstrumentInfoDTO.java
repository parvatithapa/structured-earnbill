package com.sapienter.jbilling.server.payment.db;

import java.io.Closeable;
import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import lombok.ToString;

/**
 * 
 * @author khobab
 *
 */
@Entity
@TableGenerator(
        name="payment_instrument_info_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_instrument_info",
        allocationSize = 10
        )
@Table(name = "payment_instrument_info")
@ToString
public class PaymentInstrumentInfoDTO implements AutoCloseable, Serializable{
	
	private Integer id;
	
	private PaymentDTO payment;
	private PaymentResultDTO result;
	private PaymentMethodDTO paymentMethod;
	private PaymentInformationDTO paymentInformation;
	
	public PaymentInstrumentInfoDTO() {
	}
	
	public PaymentInstrumentInfoDTO(PaymentDTO payment, PaymentResultDTO result, PaymentMethodDTO paymentMethod, PaymentInformationDTO paymentInformation) {
    	this.payment = payment;
    	this.result = result;
    	this.paymentMethod = paymentMethod;
    	this.paymentInformation = paymentInformation;
	}
	
	@Id @GeneratedValue(generator="payment_instrument_info_GEN", strategy=GenerationType.TABLE)
	@Column(name="id", unique=true, nullable=false)
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id")
	public PaymentDTO getPayment() {
		return payment;
	}

	public void setPayment(PaymentDTO payment) {
		this.payment = payment;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "result_id")
	public PaymentResultDTO getResult() {
		return result;
	}

	public void setResult(PaymentResultDTO result) {
		this.result = result;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "method_id")
	public PaymentMethodDTO getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethodDTO paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "instrument_id")
	public PaymentInformationDTO getPaymentInformation() {
		return paymentInformation;
	}

	public void setPaymentInformation(PaymentInformationDTO paymentInformation) {
		this.paymentInformation = paymentInformation;
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
		// Close PaymentInformationObject object
		if (null != paymentInformation){
			paymentInformation.close();
		}
	}
}
