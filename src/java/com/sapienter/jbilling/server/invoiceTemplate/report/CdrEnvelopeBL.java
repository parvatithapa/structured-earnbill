package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Klim Sviridov
 */
public class CdrEnvelopeBL {

    private int productId;
    private String productName;
    private String cdrId;
    private Date date;
    private BigDecimal duration;
    private BigDecimal price;
    private String description;
    private long timestamp;
    private String accountcode;
    private String amaflags;
    private Date answer;
    private Integer billsec;
    private String channel;
    private String clid;
    private String disposition;
    private String dst;
    private String dstchannel;
    private Date end;
    private String itemId;
    private String lastapp;
    private String lastdata;
    private String src;
    private Date start;
    private String userfield;
    private String dcontext;
    private String num;
    private String assetId;
    private String assetType;
    private String assetDetail;

    public CdrEnvelopeBL setProductId(int productId) {
        this.productId = productId;
        return this;
    }

    public CdrEnvelopeBL setProductName(String productName) {
        this.productName = productName;
        return this;
    }

    public CdrEnvelopeBL setCdrId(String cdrId) {
        this.cdrId = cdrId;
        return this;
    }

    public CdrEnvelopeBL setDate(Date date) {
        this.date = date;
        return this;
    }

    public CdrEnvelopeBL setDuration(BigDecimal duration) {
        this.duration = duration;
        return this;
    }

    public CdrEnvelopeBL setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public CdrEnvelopeBL setDescription(String description) {
        this.description = description;
        return this;
    }

    public CdrEnvelopeBL setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public CdrEnvelopeBL setAccountcode(String accountcode) {
        this.accountcode = accountcode;
        return this;
    }

    public CdrEnvelopeBL setAmaflags(String amaflags) {
        this.amaflags = amaflags;
        return this;
    }

    public CdrEnvelopeBL setAnswer(Date answer) {
        this.answer = answer;
        return this;
    }

    public CdrEnvelopeBL setBillsec(Integer billsec) {
        this.billsec = billsec;
        return this;
    }

    public CdrEnvelopeBL setChannel(String channel) {
        this.channel = channel;
        return this;
    }

    public CdrEnvelopeBL setClid(String clid) {
        this.clid = clid;
        return this;
    }

    public CdrEnvelopeBL setDisposition(String disposition) {
        this.disposition = disposition;
        return this;
    }

    public CdrEnvelopeBL setDst(String dst) {
        this.dst = dst;
        return this;
    }

    public CdrEnvelopeBL setDstchannel(String dstchannel) {
        this.dstchannel = dstchannel;
        return this;
    }

    public CdrEnvelopeBL setEnd(Date end) {
        this.end = end;
        return this;
    }

    public CdrEnvelopeBL setItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public CdrEnvelopeBL setLastapp(String lastapp) {
        this.lastapp = lastapp;
        return this;
    }

    public CdrEnvelopeBL setLastdata(String lastdata) {
        this.lastdata = lastdata;
        return this;
    }

    public CdrEnvelopeBL setSrc(String src) {
        this.src = src;
        return this;
    }

    public CdrEnvelopeBL setStart(Date start) {
        this.start = start;
        return this;
    }

    public CdrEnvelopeBL setUserfield(String userfield) {
        this.userfield = userfield;
        return this;
    }

    public CdrEnvelopeBL setDcontext(String dcontext) {
        this.dcontext = dcontext;
        return this;
    }

    public CdrEnvelopeBL setNum(int num) {
        this.num = String.valueOf(num);
        return this;
    }

    public CdrEnvelopeBL setAssets(Iterable<AssetEnvelope> assets, String defaultAssetIdLabel) {
        StringBuilder assetIdBuilder = new StringBuilder();

        Map<ItemTypeDTO, Collection<AssetEnvelope>> assetsByType = new HashMap<ItemTypeDTO, Collection<AssetEnvelope>>();

        for (Iterator<AssetEnvelope> i = assets.iterator(); i.hasNext(); ) {
            AssetEnvelope asset = i.next();
            String assetIdentifier = asset.getIdentifier();
            ItemTypeDTO assetItemType = asset.getItemType();
            assetIdBuilder.append(assetIdentifier);
            if (i.hasNext()) {
                assetIdBuilder.append("\n\r");
            }
            if (!assetsByType.containsKey(assetItemType)) {
                assetsByType.put(assetItemType, new TreeSet<AssetEnvelope>(new Comparator<AssetEnvelope>() {
                    @Override
                    public int compare(AssetEnvelope o1, AssetEnvelope o2) {
                        return o1.getIdentifier().compareTo(o2.getIdentifier());
                    }
                }));
            }
            assetsByType.get(assetItemType).add(asset);
        }

        StringBuilder assetTypeBuilder = new StringBuilder();
        StringBuilder assetDetailBuilder = new StringBuilder();


        for (Iterator<Map.Entry<ItemTypeDTO, Collection<AssetEnvelope>>> iterator = assetsByType.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ItemTypeDTO, Collection<AssetEnvelope>> entry = iterator.next();
            String assetType = entry.getKey().getDescription();
            String assetIdLabel = entry.getKey().getAssetIdentifierLabel();
            if (assetIdLabel == null || assetIdLabel.isEmpty()) {
                assetIdLabel = defaultAssetIdLabel;
            }
            Collection<AssetEnvelope> assetEnvelopeCollection = entry.getValue();
            assetDetailBuilder.append(assetType).append(" - ").append(assetIdLabel).append(assetEnvelopeCollection.size() > 1 ? "s" : "").append(": ");
            for (Iterator<AssetEnvelope> i = assetEnvelopeCollection.iterator(); i.hasNext(); ) {
                assetDetailBuilder.append(i.next().getIdentifier());
                if (i.hasNext()) {
                    assetDetailBuilder.append(", ");
                }
            }
            assetTypeBuilder.append(assetType);
            if (iterator.hasNext()) {
                assetTypeBuilder.append("\n\r");
                assetDetailBuilder.append("\n\r");
            }
        }

        assetId = assetIdBuilder.toString();
        assetType = assetTypeBuilder.toString();
        assetDetail = assetDetailBuilder.toString();

        return this;
    }

    public CdrEnvelope createCdrEnvelope() {
        return new CdrEnvelope(productId, productName, cdrId, date, duration, price, description, timestamp, accountcode,
                amaflags, answer, billsec, channel, clid, disposition, dst, dstchannel, end, itemId, lastapp, lastdata,
                src, start, userfield, dcontext, num, assetId, assetType, assetDetail);
    }
}
