package com.sapienter.jbilling.server.payment.db;

import org.junit.Test;
import org.mockito.Mockito;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class GetDescriptionTest{

    @Test (expected = NullPointerException.class)
    public void testPaymentNull() {
        PaymentMethodDTO paymentMethodDTO = new PaymentMethodDTO();
        paymentMethodDTO.getDescription(null, 1);
        fail("NPE is expected");
    }

    @Test
    public void testMethodName() {
        PaymentMethodDTO paymentMethodDTO = Mockito.mock(PaymentMethodDTO.class);
        PaymentDTO paymentDTO = getPaymentDTO();
        Mockito.when(paymentMethodDTO.getDescription(paymentDTO,1)).thenReturn("Card");
        assertEquals(paymentMethodDTO.getDescription(paymentDTO,1),"Card");
    }

    @Test
    public void testCustomMethodName(){
        PaymentInformationDAS mockPaymentInformationDAS = Mockito.mock(PaymentInformationDAS.class);
        Integer paymentMethodId = getPaymentDTO().getPaymentInstrumentsInfo().get(0).getId();
        Mockito.when(mockPaymentInformationDAS.findCustomPaymentMethodType(paymentMethodId)).thenReturn("Cash");
        assertEquals(mockPaymentInformationDAS.findCustomPaymentMethodType(paymentMethodId),"Cash");
    }

    private PaymentMethodDTO getPaymentMethodDTO(PaymentDTO paymentDTO){
        PaymentMethodDTO paymentMethodDTO = new PaymentMethodDTO();
        Set<PaymentDTO> payments = new HashSet<>();
        payments.add(paymentDTO);
        paymentMethodDTO.setId(3);
        paymentMethodDTO.setDescription("Custom");
        paymentMethodDTO.setPayments(payments);
        return paymentMethodDTO;
    }

    private PaymentMethodTemplateDTO getPaymentMethodTemplateDTO(){
        PaymentMethodTemplateDTO paymentMethodTemplateDTO = new PaymentMethodTemplateDTO();
        paymentMethodTemplateDTO.setId(7);
        paymentMethodTemplateDTO.setTemplateName("Custom");
        return paymentMethodTemplateDTO;
    }

    private PaymentMethodTypeDTO getPaymentMethodTypeDTO(){
        PaymentMethodTypeDTO paymentMethodTypeDTO = new PaymentMethodTypeDTO();
        paymentMethodTypeDTO.setId(8);
        paymentMethodTypeDTO.setMethodName("Cash");
        paymentMethodTypeDTO.setPaymentMethodTemplate(getPaymentMethodTemplateDTO());
        return paymentMethodTypeDTO;
    }

    private PaymentInformationDTO getPaymentInformationDTO(){
        PaymentInformationDTO paymentInformationDTO = new PaymentInformationDTO();
        paymentInformationDTO.setId(9);
        paymentInformationDTO.setProcessingOrder(123);
        paymentInformationDTO.setPaymentMethodType(getPaymentMethodTypeDTO());
        return paymentInformationDTO;
    }

    private List<PaymentInstrumentInfoDTO> getPaymentInstrumentInfoDTO(PaymentDTO paymentDTO){
        PaymentInstrumentInfoDTO paymentInstrumentInfoDTO = new PaymentInstrumentInfoDTO();
        List<PaymentInstrumentInfoDTO> paymentInstrumentInfoDTOS = new ArrayList<>();
        paymentInstrumentInfoDTO.setId(16);
        paymentInstrumentInfoDTO.setPaymentMethod(getPaymentMethodDTO(paymentDTO));
        paymentInstrumentInfoDTO.setPaymentInformation(getPaymentInformationDTO());
        paymentInstrumentInfoDTOS.add(paymentInstrumentInfoDTO);
        return paymentInstrumentInfoDTOS;
    }

    private PaymentDTO getPaymentDTO(){
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(18);
        paymentDTO.setPaymentInstrumentsInfo(getPaymentInstrumentInfoDTO(paymentDTO));
        paymentDTO.setPaymentMethod(getPaymentMethodDTO(paymentDTO));
        return paymentDTO;
    }
}