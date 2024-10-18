package com.sapienter.jbilling.server.provisioning;

import com.sapienter.jbilling.server.provisioning.db.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProvisioningRequestBL {

    private ProvisioningRequestDTO provisioningRequestDTO;
    private ProvisioningRequestDAS provisioningRequestDAS;

    public ProvisioningRequestBL() {
        init();
    }

    public ProvisioningRequestBL(Integer provisioningCommandId) {
        init();
        set(provisioningCommandId);
    }

    public ProvisioningRequestBL(ProvisioningRequestDTO provisioningRequestDTO) {
        init();
        this.provisioningRequestDTO = provisioningRequestDTO;
    }

    public ProvisioningRequestDTO getProvisioningRequest() {
        return provisioningRequestDTO;
    }

    private void init() {
        provisioningRequestDAS = new ProvisioningRequestDAS();
    }

    public void set(Integer id) {
        provisioningRequestDTO = provisioningRequestDAS.find(id);
    }

    public void set(ProvisioningRequestDTO provisioningRequestDTO) {
        this.provisioningRequestDTO = provisioningRequestDTO;
    }


    public ProvisioningRequestWS[] getProvisioningRequestWSList(ProvisioningCommandDTO dto){
            if(dto != null) {
                List<ProvisioningRequestDTO> requestsDTOList = dto.getProvisioningRequests();
                List<ProvisioningRequestWS> requestsWSList = new ArrayList<ProvisioningRequestWS>();
                for(ProvisioningRequestDTO requestDTO: requestsDTOList) {
                    ProvisioningRequestWS ws = getProvisioningRequestWS(requestDTO);
                    requestsWSList.add(ws);
                }
                return requestsWSList.toArray(new ProvisioningRequestWS[requestsWSList.size()]);
            }
        return null;
    }

    public ProvisioningRequestWS getProvisioningRequestWS(ProvisioningRequestDTO dto) {
        ProvisioningRequestWS ws = new ProvisioningRequestWS();
        ws.setId(dto.getId());
        ws.setIdentifier(dto.getIdentifier());
        if (dto.getProvisioningCommand() != null) {
            ws.setProvisioningCommandId(dto.getProvisioningCommand().getId());
            ws.setEntityId(dto.getProvisioningCommand().getEntity().getId());
        }
        ws.setProcessor(dto.getProcessor());
        ws.setExecutionOrder(dto.getExecutionOrder());
        ws.setCreateDate(dto.getCreateDate());
        ws.setSubmitDate(dto.getSubmitDate());
        ws.setResultReceivedDate(dto.getResultReceivedDate());
        ws.setVersionNum(dto.getVersionNum());
        ws.setRequestStatus(dto.getRequestStatus());
        ws.setRollbackRequest(dto.getRollbackRequest());
		ws.setResultReceivedDate(dto.getResultReceivedDate());
        ws.setSubmitRequest(dto.getSubmitRequest());
        ws.setResultMap(new HashMap<String, String>(dto.getResultMap()));
        return ws;
    }
}
