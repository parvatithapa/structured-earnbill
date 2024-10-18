package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineTierDAS;
import com.sapienter.jbilling.server.order.db.OrderLineTierDTO;

/**
 * Created by faizan on 1/2/18.
 */
public class OrderLineTierBL {
    private OrderLineTierDTO orderLineTierDTO = null;
    private OrderLineTierDAS orderLineTierDAS = null;
    public OrderLineTierBL() {
        init();
    }

    public OrderLineTierBL(Integer orderLineTierId) {
        init();
        set(orderLineTierId);
    }

    public OrderLineTierBL(OrderLineTierDTO orderLineTierDTO) {
        init();
        this.orderLineTierDTO = orderLineTierDTO;
    }

    private void init() {
        orderLineTierDAS = new OrderLineTierDAS();
    }

    public void set(Integer id) {
        orderLineTierDTO = orderLineTierDAS.find(id);
    }

    public OrderLineTierDTO getOrderLineTierDTO(OrderLineTierWS ws) {
        OrderLineTierDTO dto = new OrderLineTierDTO();
        dto.setId(ws.getId());
        dto.setAmount(ws.getAmount());
        dto.setPrice(ws.getPrice());
        dto.setQuantity(ws.getQuantity());
        dto.setTierNumber(ws.getTierNumber());
        dto.setTierFrom(ws.getTierFrom());
        dto.setTierTo(ws.getTierTo());

        OrderLineDAS olDas = new OrderLineDAS();
        dto.setOrderLine(olDas.find(ws.getOrderLineId()));
        return dto;
    }

    public OrderLineTierWS getOrderLineTierWS(OrderLineTierDTO orderLineTierDTO) {
        OrderLineTierWS dtoWs = new OrderLineTierWS();
        dtoWs.setId(orderLineTierDTO.getId());
        dtoWs.setAmount(orderLineTierDTO.getAmount());
        dtoWs.setOrderLineId(orderLineTierDTO.getOrderLine().getId());
        dtoWs.setPrice(orderLineTierDTO.getPrice());
        dtoWs.setQuantity(orderLineTierDTO.getQuantity());
        dtoWs.setTierNumber(orderLineTierDTO.getTierNumber());
        dtoWs.setTierFrom(orderLineTierDTO.getTierFrom());
        dtoWs.setTierTo(orderLineTierDTO.getTierTo());

        return dtoWs;
    }

    public OrderLineTierDTO saveOrderLineTier (OrderLineTierDTO orderLineTierDTO) {
        return orderLineTierDAS.save(orderLineTierDTO);
    }
}
