package com.sapienter.jbilling.server.provisioning;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.provisioning.db.*;

import java.util.*;

public class ProvisioningCommandBL {

    private ProvisioningCommandDTO provisioningCommandDTO;
    private ProvisioningCommandDAS provisioningCommandDAS;

    private OrderLineDAS orderLineDAS;
    private OrderDAS orderDAS;
    private AssetDAS assetDAS;
    private PaymentDAS paymentDAS;

    public ProvisioningCommandBL() {
        init();
    }

    public ProvisioningCommandBL(Integer provisioningCommandId) {
        init();
        set(provisioningCommandId);
    }

    public ProvisioningCommandBL(ProvisioningCommandDTO provisioningCommandDTO) {
        init();
        this.provisioningCommandDTO = provisioningCommandDTO;
    }

    public ProvisioningCommandDTO getProvisioningCommandDTO() {
        return provisioningCommandDTO;
    }

    private void init() {
        provisioningCommandDAS = new ProvisioningCommandDAS();
        orderDAS = new OrderDAS();
        orderLineDAS = new OrderLineDAS();
        assetDAS = new AssetDAS();
        paymentDAS = new PaymentDAS();
    }

    public void set(Integer id) {
        provisioningCommandDTO = provisioningCommandDAS.find(id);
    }

    public void set(ProvisioningCommandDTO commandDTO) {
        provisioningCommandDTO = commandDTO;
    }

    public IProvisionable getObjectDTO(ProvisioningCommandType type, Integer typeId) {

        switch (type) {

            case ORDER_LINE:
                IProvisionable orderLineDTO = orderLineDAS.findNow(typeId);
                return orderLineDTO;

            case ORDER:
                IProvisionable orderDTO = orderDAS.findNow(typeId);
                return orderDTO;

            case ASSET:
                IProvisionable assetDTO = assetDAS.findNow(typeId);
                return assetDTO;

            case PAYMENT:
                IProvisionable paymentDTO = paymentDAS.findNow(typeId);
                return paymentDTO;

            default:
                throw new SessionInternalError("Object from type" + type + " and Id: " + typeId + " not found");
        }
    }

    public List <ProvisioningCommandWS> getCommandWSList(ProvisioningCommandType type, Integer typeId){
        if (type != null){
                ProvisioningCommandWS[] listCommandsWS = getCommandsList(getObjectDTO(type, typeId));
                return Arrays.asList(listCommandsWS);
        } return null;
    }

    public List <ProvisioningRequestWS> getRequestList(Integer provisioningCommandId){
        if (provisioningCommandId != null){
            ProvisioningRequestDAS cmdDAS = new ProvisioningRequestDAS();
            List <ProvisioningRequestDTO> requestsDTOList = cmdDAS.findRequestsByCommandId(provisioningCommandId);
            List <ProvisioningRequestWS> requestsWSList = new ArrayList<ProvisioningRequestWS>();
            ProvisioningRequestBL requestBL = new ProvisioningRequestBL();
            for(ProvisioningRequestDTO requestDTO: requestsDTOList) {
                ProvisioningRequestWS ws = requestBL.getProvisioningRequestWS(requestDTO);
                requestsWSList.add(ws);
            }
            return requestsWSList;
        } return null;
    }

    public ProvisioningCommandWS[] getCommandsList(IProvisionable dto){

        if (dto == null) {
            return null;
        }

        List<ProvisioningCommandDTO> commandsDTOList = dto.getProvisioningCommands();
        List<ProvisioningCommandWS> commandsWSList = new ArrayList<ProvisioningCommandWS>();
        for(ProvisioningCommandDTO commandDTO: commandsDTOList) {
            ProvisioningCommandWS ws = getCommandWS(commandDTO);
            commandsWSList.add(ws);
        }
        return commandsWSList.toArray((new ProvisioningCommandWS[commandsWSList.size()]));
    }

    public static ProvisioningCommandWS getCommandWS(ProvisioningCommandDTO commandDTO) {
       if(commandDTO !=null){
            ProvisioningRequestBL requestBL = new ProvisioningRequestBL();
            ProvisioningCommandWS ws= new ProvisioningCommandWS();
            ws.setId(commandDTO.getId());
            ws.setName(commandDTO.getName());
            ws.setEntityId(commandDTO.getEntity().getId());
            ws.setExecutionOrder(commandDTO.getExecutionOrder());
            ws.setCreateDate(commandDTO.getCreateDate());
            ws.setLastUpdateDate(commandDTO.getLastUpdateDate());
            ws.setCommandType(commandDTO.getCommandType());
            ws.setCommandStatus(commandDTO.getCommandStatus());
            ws.setVersionNum(commandDTO.getVersionNum());
            ws.setParameterMap(new HashMap<String, String>(commandDTO.getCommandParameters()));
            if (commandDTO instanceof AssetProvisioningCommandDTO) {
                ws.setOwningEntityId(((AssetProvisioningCommandDTO) commandDTO).getAsset() != null ?
                        ((AssetProvisioningCommandDTO) commandDTO).getAsset().getId() : null);
            } else if (commandDTO instanceof OrderProvisioningCommandDTO) {
                ws.setOwningEntityId(((OrderProvisioningCommandDTO) commandDTO).getOrder() != null ?
                        ((OrderProvisioningCommandDTO) commandDTO).getOrder().getId() : null);
            } else if (commandDTO instanceof OrderLineProvisioningCommandDTO) {
                ws.setOwningEntityId(((OrderLineProvisioningCommandDTO) commandDTO).getOrderLine() != null ?
                        ((OrderLineProvisioningCommandDTO) commandDTO).getOrderLine().getId() : null);
            } else if (commandDTO instanceof PaymentProvisioningCommandDTO) {
                ws.setOwningEntityId(((PaymentProvisioningCommandDTO) commandDTO).getPayment() != null ?
                        ((PaymentProvisioningCommandDTO) commandDTO).getPayment().getId() : null);
            }
            ws.setProvisioningRequests(requestBL.getProvisioningRequestWSList(commandDTO));
            return ws;
       } return null;
    }
}
