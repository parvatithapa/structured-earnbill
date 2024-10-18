package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.order.OrderWS;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by pablo123 on 01/03/2017.
 */
public class DistributelOrderWS extends OrderWS {

    private BigDecimal gross;
    private BigDecimal pst;
    private BigDecimal gst;
    private BigDecimal hst;
    private BigDecimal net;
    private Date createDate;

    public DistributelOrderWS(OrderWS orderWS) {
        createDate = orderWS.getCreateDate();
        pst = BigDecimal.ZERO;
        hst = BigDecimal.ZERO;
        gst = BigDecimal.ZERO;
    }

    public BigDecimal getGross() {
        return gross;
    }

    public void setGross(BigDecimal gross) {
        this.gross = gross;
    }

    public BigDecimal getNet() {
        return net;
    }

    public void setNet(BigDecimal net) {
        this.net = net;
    }

    public BigDecimal getPst() {
        return pst;
    }

    public void setPst(BigDecimal pst) {
        this.pst = pst;
    }

    public BigDecimal getGst() {
        return gst;
    }

    public void setGst(BigDecimal gst) {
        this.gst = gst;
    }

    public BigDecimal getHst() {
        return hst;
    }

    public void setHst(BigDecimal hst) {
        this.hst = hst;
    }

    @Override
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
