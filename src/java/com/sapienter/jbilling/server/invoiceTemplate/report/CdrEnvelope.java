package com.sapienter.jbilling.server.invoiceTemplate.report;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author elmot
 */
public class CdrEnvelope {

    // Data from mediation record

    @Field(description = "Product ID")
    private final Integer productId;

    @Field(description = "Product Name")
    private final String productName;

    @Field(description = "CDR ID")
    private final String cdrId;

    @Field(description = "Date")
    private final Date date;

    @Field(description = "Timestamp")
    private final Long timestamp;

    @Field(description = "Duration")
    private final BigDecimal duration;

    @Field(description = "Duration Time")
    private final Date durationTime;

    @Field(description = "Price")
    private final BigDecimal price;

    @Field(description = "Description")
    private final String description;

    // Data from CDR

    @Field(description = "Account Code")
    private final String accountcode;

    @Field(description = "AMA Flags")
    private final String amaflags;

    @Field(description = "Answer")
    private final Date answer;

    @Field(description = "Bill Sec")
    private final Integer billsec;

    @Field(description = "Channel")
    private final String channel;

    @Field(description = "CLI ID")
    private final String clid;

    @Field(description = "Disposition")
    private final String disposition;

    @Field(description = "DST")
    private final String dst;

    @Field(description = "DST Channel")
    private final String dstchannel;

    @Field(description = "End")
    private final Date end;

    @Field(description = "Item ID")
    private final String itemId;

    @Field(description = "Last App")
    private final String lastapp;

    @Field(description = "Last Data")
    private final String lastdata;

    @Field(description = "SRC")
    private final String src;

    @Field(description = "Start")
    private final Date start;

    @Field(description = "User Field")
    private final String userfield;

    @Field(description = "Dcon Text")
    private final String dcontext;

    @Field(description = "Num")
    private final String num;

    // Assets

    @Field(description = "Asset Type")
    private final String assetType;

    @Field(description = "Asset IDs")
    private final String assetId;

    @Field(description = "Asset Detail")
    private final String assetDetail;

    CdrEnvelope(int productId,
                String productName,
                String cdrId,
                Date date,
                BigDecimal duration,
                BigDecimal price,
                String description,
                long timestamp,
                String accountcode,
                String amaflags,
                Date answer,
                Integer billsec,
                String channel,
                String clid,
                String disposition,
                String dst,
                String dstchannel,
                Date end,
                String itemId,
                String lastapp,
                String lastdata,
                String src,
                Date start,
                String userfield,
                String dcontext,
                String num,
                String assetId,
                String assetType,
                String assetDetail) {

        this.productId = productId;
        this.productName = productName;
        this.cdrId = cdrId;
        this.date = date;
        this.duration = duration;
        this.durationTime = new Date(TimeUnit.SECONDS.toMillis(duration.longValue()));
        this.price = price;
        this.description = description;
        this.timestamp = timestamp;
        this.accountcode = accountcode;
        this.amaflags = amaflags;
        this.answer = answer;
        this.billsec = billsec;
        this.channel = channel;
        this.clid = clid;
        this.disposition = disposition;
        this.dst = dst;
        this.dstchannel = dstchannel;
        this.end = end;
        this.itemId = itemId;
        this.lastapp = lastapp;
        this.lastdata = lastdata;
        this.src = src;
        this.start = start;
        this.userfield = userfield;
        this.dcontext = dcontext;
        this.num = num;
        this.assetId = assetId;
        this.assetType = assetType;
        this.assetDetail = assetDetail;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Date getDate() {
        return date;
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public Date getDurationTime() {
        return durationTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getCdrId() {
        return cdrId;
    }

    public String getAccountcode() {
        return accountcode;
    }

    public String getAmaflags() {
        return amaflags;
    }

    public Date getAnswer() {
        return answer;
    }

    public Integer getBillsec() {
        return billsec;
    }

    public String getChannel() {
        return channel;
    }

    public String getClid() {
        return clid;
    }

    public String getDisposition() {
        return disposition;
    }

    public String getDst() {
        return dst;
    }

    public String getDstchannel() {
        return dstchannel;
    }

    public Date getEnd() {
        return end;
    }

    public String getItemId() {
        return itemId;
    }

    public String getLastapp() {
        return lastapp;
    }

    public String getLastdata() {
        return lastdata;
    }

    public String getSrc() {
        return src;
    }

    public Date getStart() {
        return start;
    }

    public String getUserfield() {
        return userfield;
    }

    public String getDcontext() {
        return dcontext;
    }

    public String getNum() {
        return num;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAssetDetail() {
        return assetDetail;
    }
}
