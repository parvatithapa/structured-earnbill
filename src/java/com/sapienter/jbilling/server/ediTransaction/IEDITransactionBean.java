package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.fileProcessing.fileParser.FlatFileParser;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Date;

import java.util.List;
import java.util.Map;

/**
 * Created by aman on 16/10/15.
 */
public interface IEDITransactionBean {

    public EDIFileWS getEDIFileWS(Integer ediFileId);

    public EDIFileWS parseEDIFile(FlatFileParser fileParser);

    public boolean isUniqueKeyExistForFile(Integer entityId, Integer ediTypeId, Integer ediFileId, String key, String value, TransactionType transactionType) throws SessionInternalError;

    public CompanyWS getCompanyWS(Integer entityId) throws SessionInternalError;

    public OrderDTO getOrder(Integer orderId) throws SessionInternalError;

    /*Enrollment Parser task methods*/
    public EDIFileWS getOutboundFileForCustomerEnrollment(String enrollmentId, Integer entityId, Integer ediTypeId);

    public Integer createCustomerAndOrder(CustomerEnrollmentWS customerEnrollmentWS, Map<String, String> ediFileData) throws SessionInternalError;

    public void initiateTermination(Integer userId, String reasonCode, Date terminationDate) throws SessionInternalError;

    /*EDI file generation task*/
    public String getCommodityCode(String internalNumber, Integer entityId);

    public DateTime getRateChangeDate(DateTime dateTime, Integer cycleNumber, Integer entityId);
    public String getRateCode(Integer companyId, Double price);
    public EDIFileDTO generateEDIFile(FileFormat fileFormat, Integer entityId, String name,List<Map<String, String>> recordMapList);
    public void updateUser(UserDTO userDTO);
    public Integer generateInvoice(String meterReadId, Integer companyId, String INVOICE_NR) ;

    public BigDecimal getPlanItemPrice(UserDTO userDTO, Integer itemId, Integer companyId );
    public BigDecimal getRateByRateCode(Integer companyId, String rateCode );

    public boolean hasPlanSendRateChangeDaily(PlanDTO planDTO);

    public boolean hasPlanSendRateChangeDaily(UserDTO userDTO);

    public String calculateRate(CustomerEnrollmentDTO enrollmentDTO, Boolean rateCode);

    public Boolean isCustomerExistForAccountNumber(CustomerEnrollmentDTO enrollmentDTO);
    public UserDTO findUserByAccountNumber(Integer companyId, String metaFieldName, String customerAccountNumber, String commodityValue);
    public UserDTO findUserByAccountNumber(Integer companyId, String metaFieldName, String customerAccountNumber);
    public UserDTO findUserByAccountNumber(Integer companyId, String metaFieldName, String customerAccountNumber, Boolean isFinal, String commodityValue);
    public String getEDICommunicationPath(Integer entityId, TransactionType transactionType);
    public String getBillingModelType(CustomerEnrollmentDTO enrollmentDTO) ;
    public void changeEdiFileStatus(String statusName, EDIFileWS ediFileWS, Integer ediTypeId);
    public EDIFileStatusWS getEDIFileStatus(Integer ediTypeId, String statusName);
    public Integer getEDITypeId(Integer entityId, String companyLevelMetaFieldName);
}
