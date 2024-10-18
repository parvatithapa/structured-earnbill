package com.sapienter.jbilling.server.user;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CancellationRequestDAS;
import com.sapienter.jbilling.server.user.db.CancellationRequestDTO;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;


public class CancellationRequestBL{

	private EventLogger eLogger;
	private CancellationRequestDAS cancellationRequestDAS;
	public CancellationRequestBL() {
		init();
	}

	private void init() {
		eLogger = EventLogger.getInstance();
		cancellationRequestDAS = new CancellationRequestDAS();
	}

	private static final FormatLogger LOG = new FormatLogger(CancellationRequestBL.class);

	public CancellationRequestWS[] getAllCancellationRequests(Integer entityId, Date startDate, Date endDate) {
		List<CancellationRequestWS> cancellationRequestWSs = new ArrayList<CancellationRequestWS>();
		List<CancellationRequestDTO>  cancellationRequestDTOs = cancellationRequestDAS.findCancelRequestsByEntityAndDateRange(entityId, startDate, endDate);
		for (CancellationRequestDTO cancellationRequestDTO : cancellationRequestDTOs) {
			cancellationRequestWSs.add(getWS(cancellationRequestDTO));
		}
		 return cancellationRequestWSs.toArray(new CancellationRequestWS[cancellationRequestWSs.size()]);
	}

	public CancellationRequestWS getCancellationRequestById(Integer cancellationRequestId) {
		CancellationRequestDTO cancellationRequestDTO = cancellationRequestDAS.find(cancellationRequestId);
		return getWS(cancellationRequestDTO);
	}

	public CancellationRequestWS[] getCancellationRequestsByUserId(Integer userId) {
		List<CancellationRequestWS> cancellationRequestWSs = new ArrayList<CancellationRequestWS>();
		List<CancellationRequestDTO> cancellationRequestDTOs = cancellationRequestDAS.getCancellationRequestsByUserId(userId);
		for (CancellationRequestDTO cancellationRequestDTO : cancellationRequestDTOs) {
			cancellationRequestWSs.add(getWS(cancellationRequestDTO));
		}
		return cancellationRequestWSs.toArray(new CancellationRequestWS[cancellationRequestWSs.size()]);
	}

	/**
	 * This method creates a cancellation request after validating it.
	 *
	 * @param cancellationRequest
	 * @param executorUserId
	 * @return
	 */
	public Integer createCancellationRequest(CancellationRequestWS cancellationRequest,Integer executorUserId) {
		try {
			Integer requestId = null;
			CancellationRequestDTO cancellationRequestDTO = getDTO(cancellationRequest);
			Integer userId = cancellationRequestDTO.getCustomer().getBaseUser().getId();
			if (isCancellationRequestExist(userId)) {
				LOG.error("There is already one pending cancellation request for user %s ",userId);
				throw new SessionInternalError("Cancellation Request is already in Pending State");
			}
			if(!validateCancellationRequest(cancellationRequest.getCancellationDate(), userId)){
				LOG.error("One or more Validation failed while creating cancellation request for user %s ",userId);
				throw new SessionInternalError("Validation Failed while creation of Cancellation Request");
			}
			updateActiveUntil(userId,executorUserId,cancellationRequest.getCancellationDate());
			cancellationRequestDTO.setStatus(CancellationRequestStatus.APPLIED);
			requestId = cancellationRequestDAS.save(cancellationRequestDTO).getId();
			Integer userEntityId = new UserDAS().getEntityByUserId(userId);
			eLogger.audit(executorUserId!=null ? executorUserId : userEntityId ,userId, Constants.TABLE_CANCELLATION_REQUEST,
					requestId, EventLogger.MODULE_USER_MAINTENANCE,
					EventLogger.ROW_CREATED, null, null, null);
			return requestId;
		} catch(Exception ex) {
			LOG.error("Exception occurred while creating cancellation request");
			throw new SessionInternalError("Exception occurred while creating cancellation request", ex);
		}
	}

	/**
	 * Helper method to Validate Cancellation Request before Creating or Updating it.
	 *
	 * @param cancellationDate
	 * @param userId
	 * @return
	 * @throws SessionInternalError
	 */
	private boolean validateCancellationRequest(Date cancellationDate,
			Integer userId) throws SessionInternalError {
		if (!isCancellationDateBeforeLastInvoiceDate(cancellationDate, userId)) {
			LOG.error("Cancellation Request Date should be higher than Last Invoice Date");
			throw new SessionInternalError("Cancellation Request Date should be higher than Last Invoice Date");
		}
		if (checkActiveSubscriptionOrders(userId) <= 0) {
			LOG.error("No Active Subscription Orders for the Customer");
			throw new SessionInternalError("No Active Subscription Orders for the Customer");
		}
		return true;
	}

	/**
	 * Helper Method to validate if a cancellation request with pending or in processed status is already present for the User.
	 *
	 * @param userId
	 * @return
	 */
	private boolean isCancellationRequestExist(Integer userId) {
		// validate if a cancellation with pending status is already present for the User.
		CancellationRequestWS[] cancellationRequestWSs = getCancellationRequestsByUserId(userId);
		return !ArrayUtils.isEmpty(cancellationRequestWSs);
	}



	public CancellationRequestWS getWS(CancellationRequestDTO cancellationRequestDTO){
		CancellationRequestWS cancellationRequestWS = new CancellationRequestWS();
		if (null != cancellationRequestDTO){
			cancellationRequestWS.setId(cancellationRequestDTO.getId());
			cancellationRequestWS.setCancellationDate(cancellationRequestDTO.getCancellationDate());
			cancellationRequestWS.setCustomerId(cancellationRequestDTO.getCustomer().getId());
			cancellationRequestWS.setReasonText(cancellationRequestDTO.getReasonText());
			cancellationRequestWS.setStatus(cancellationRequestDTO.getStatus());
		}
		return cancellationRequestWS;
	}

	public CancellationRequestDTO getDTO(CancellationRequestWS cancellationRequestWS){
		CancellationRequestDTO cancellationRequestDTO = new CancellationRequestDTO();
		if (null != cancellationRequestWS){
			cancellationRequestDTO.setId(cancellationRequestWS.getId());
			cancellationRequestDTO.setCancellationDate(cancellationRequestWS.getCancellationDate());
			cancellationRequestDTO.setCreateTimestamp(TimezoneHelper.serverCurrentDate());
			cancellationRequestDTO.setCustomer(new CustomerDAS().find(cancellationRequestWS.getCustomerId()));
			cancellationRequestDTO.setReasonText(cancellationRequestWS.getReasonText());
		}
		return cancellationRequestDTO;
	}

	/**
	 * This method Updates the Cancellation Request after validating the cancellation request
	 *
	 * @param cancellationRequest
	 * @param executorUserId
	 */
	public void updateCancellationRequest(CancellationRequestWS cancellationRequest,Integer executorUserId){
		if(null == cancellationRequest){
			LOG.error("Exception occurred while updating cancellation request");
			throw new SessionInternalError("Cancellation Request cannot be null");
		}
		try {
			CancellationRequestDTO cancellationRequestDTO = cancellationRequestDAS.find(cancellationRequest.getId());
			Integer userId = cancellationRequestDTO.getCustomer().getBaseUser().getId();
			if(!validateCancellationRequest(cancellationRequest.getCancellationDate(), userId)){
				LOG.error("One or more Validation failed while updating cancellation request for user %s ",userId);
				throw new SessionInternalError("Validation Failed while updation of Cancellation Request");
			}
			updateActiveUntil(userId, executorUserId, cancellationRequest.getCancellationDate());
			cancellationRequestDTO.setCancellationDate(cancellationRequest.getCancellationDate());
			cancellationRequestDTO.setCreateTimestamp(TimezoneHelper.serverCurrentDate());
			cancellationRequestDTO.setCustomer(new CustomerDAS().find(cancellationRequest.getCustomerId()));
			cancellationRequestDTO.setReasonText(cancellationRequest.getReasonText());
			Integer requestId = cancellationRequestDAS.save(cancellationRequestDTO).getId();
			Integer userEntityId = new UserDAS().getEntityByUserId(userId);

			eLogger.audit(executorUserId!=null ? executorUserId : userEntityId ,userId, Constants.TABLE_CANCELLATION_REQUEST,
					requestId, EventLogger.MODULE_USER_MAINTENANCE,
					EventLogger.ROW_UPDATED, null, null, null);
		} catch(Exception ex) {
			LOG.error("Exception occurred during updating cancellation request");
			throw new SessionInternalError("Exception occurred during updating cancellation request ", ex);
		}
	}

	/**
	 * This method Deletes the Cancellation Request
	 *
	 * @param cancellationId
	 * @param executorUserId
	 */
	public void deleteCancellationRequest(Integer cancellationId, Integer executorUserId){
		if(null == cancellationId){
			LOG.error("Exception occurred while deleting cancellation request");
			throw new SessionInternalError("Cancellation Request ID cannot be null");
		}
		CancellationRequestDTO cancellationRequestDTO = cancellationRequestDAS.find(cancellationId);
		if(null==cancellationRequestDTO){
			LOG.error("Exception occurred while deleting cancellation request");
			throw new SessionInternalError("Cancellation Request Not Found");
		}
		Integer userId = cancellationRequestDTO.getCustomer().getBaseUser().getUserId();
		Integer userEntityId = new UserDAS().getEntityByUserId(userId);
		// update the Active Until Date for Subscription orders to null
		List<OrderDTO> subscriptions = new OrderDAS().findByUserSubscriptions(userId);
		for(OrderDTO orderDTO : subscriptions){
			orderDTO.setActiveUntil(null);
			OrderBL bl = new OrderBL(orderDTO);
			bl.updateActiveUntil(executorUserId, null, orderDTO);
		}
		eLogger.audit(executorUserId!=null ? executorUserId : userEntityId ,userId, Constants.TABLE_CANCELLATION_REQUEST,
				cancellationRequestDTO.getId(), EventLogger.MODULE_USER_MAINTENANCE,
				EventLogger.ROW_DELETED, null, null, null);
		cancellationRequestDAS.delete(cancellationRequestDTO);
	}

	/**
	 * Helper method to update the Active Until date of Active Subscription orders for the provided userId.
	 *
	 * @param userId
	 * @param executorUserId
	 * @param date
	 */
	private void updateActiveUntil(Integer userId, Integer executorUserId, Date date){
		new OrderDAS().findByUserSubscriptions(userId).stream()
		.forEach(order -> {
			if (date.before(order.getActiveSince())) {
				LOG.error("Exception occurred during updating Active Until date of Order for Cancellation Request");
				throw new SessionInternalError("Cancellation Request Date should not be before than active since date");
			}
			order.setActiveUntil(date);
            OrderBL bl = new OrderBL(order);
            bl.updateActiveUntil(executorUserId, date, order);
		} );
	}

	/**
	 * Helper method to check the cancellation date is before the last invoice date
	 *
	 * @param requestedDate - Cancellation Date
	 * @param userId
	 *
	 * @return
	 */
	private boolean isCancellationDateBeforeLastInvoiceDate(Date requestedDate, Integer userId){
		try {
			Integer invoiceId = new InvoiceBL().getLastByUser(userId);
			if(null !=invoiceId ){
				return new InvoiceDAS().find(invoiceId).getCreateDatetime().before(requestedDate);
			}
		} catch (SQLException e) {
			LOG.error("SQL exception occured while checking cancellation request date for user %s ",userId);
			throw new SessionInternalError("SQL exception occured while checking cancellation request date ", e);
		}
		return true;
	}

	/**
	 * Helper method to count the Active subscription orders for the given userId.
	 *
	 * @param userId
	 * @return
	 */
	private int checkActiveSubscriptionOrders(Integer userId) {
		List<OrderDTO> orderDTOs = new OrderDAS().findByUserSubscriptions(userId);
		return (int) orderDTOs.stream()
				.filter(orderDTO -> orderDTO.getOrderStatus().getStatusValue().equalsIgnoreCase("Active")).count();
	}

	/**
	 * Helper method to check the User Status and if there is any Processed Cancellation Request
	 * 
	 * @param user
	 * @return boolean
	 */
	public boolean isUserCancelled(UserDTO user){
		if(user.getStatus().getDescription(user.getLanguage().getId()).equalsIgnoreCase(Constants.CUSTOMER_CANCELLATION_STATUS_DESCRIPTION)){
			return true;
		}
		List<CancellationRequestDTO> cancellationRequestDTOs = cancellationRequestDAS.getCancellationRequestsByUserId(user.getId());
		for(CancellationRequestDTO cancellationRequestDTO :cancellationRequestDTOs){
			if(cancellationRequestDTO.getStatus().equals(CancellationRequestStatus.PROCESSED)){
				return true;
			}
		}
		return false;
	}
}
