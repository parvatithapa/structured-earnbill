package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.diameter.PricingFieldsHelper;
import com.sapienter.jbilling.server.invoiceTemplate.domain.DocElement;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Font;
import com.sapienter.jbilling.server.invoiceTemplate.domain.FontFace;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignTextElement;

import org.joda.time.format.DateTimeFormat;

import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * @author elmot
 */
public class ConvertUtils {

    private ConvertUtils() {
    }

    static Color convertColor(String bgColor, Color defaultColor) {
        try {
            if (bgColor != null) {
                return Color.decode(bgColor);
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultColor;
    }

    static Color convertColor(String bgColor) {
        return convertColor(bgColor, Color.PINK);
    }

    static void setupFont(Font font, JRDesignTextElement designText) {
        if (font != null) {
            designText.setForecolor(convertColor(font.getColor()));
            String fontName = font.getFace() == null ? FontFace.DEFAULT.INTERNAL_NAME : font.getFace().INTERNAL_NAME;
            designText.setFontName(fontName);
            designText.setFontSize(font.getSize());
            designText.setBold(font.isBold());
            designText.setItalic(font.isItalic());
        }
    }

    static void setupRectangle(DocElement element, JRDesignElement designElement, int maxWidth) {
        designElement.setX(Math.min(element.getLeft(), maxWidth - 1));
        designElement.setY(element.getTop());
        designElement.setWidth(Math.min(element.getWidth(), maxWidth - designElement.getX()));
        designElement.setHeight(element.getHeight());
    }

    public static String formatMoney(BigDecimal money, Locale locale) {
        if (money == null) return null;
        if (locale != null) {
            ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            ((DecimalFormat) format).applyPattern(bundle
                    .getString("format.float.invoice"));

            return format.format(money.doubleValue());
        }

        return new DecimalFormat("#0.00").format(money);
    }

    public Collection<InvoiceLineEnvelope> filterByTotal(Collection<InvoiceLineEnvelope> lines, BigDecimal minTotal) {
        List<InvoiceLineEnvelope> result = new ArrayList<InvoiceLineEnvelope>(lines);
        for (InvoiceLineEnvelope line : lines) {
            if (line.isSameOrBigger(minTotal)) {
                result.add(line);
            }
        }
        return result;
    }

    public static List<CdrEnvelope> collectCdrs(Map<JbillingMediationRecord, CallDataRecord> mediationRecordLines, Locale locale, String defaultAssetIdLabel) {
        List<CdrEnvelope> result = new ArrayList<>(mediationRecordLines.size());
        int cdrCounter = 0;
        for (Map.Entry<JbillingMediationRecord, CallDataRecord> entry : mediationRecordLines.entrySet()) {

            JbillingMediationRecord mediationRecordLine = entry.getKey();
            CallDataRecord record = entry.getValue();
            PricingFieldsHelper pfh = new PricingFieldsHelper(record.getFields());

            Set<AssetEnvelope> cdrAssets = new TreeSet<>((o1, o2) -> {
                return o1.getIdentifier().compareTo(o2.getIdentifier());
            });

            Integer orderLineId = mediationRecordLine.getOrderLineId();
            if(orderLineId!=null) {
                OrderLineDTO orderLineDTO = new OrderLineDAS().find(orderLineId);
                for (AssetDTO asset : orderLineDTO.getAssets()) {
                    ItemTypeDTO itemType = asset.getItem().findItemTypeWithAssetManagement();
                    cdrAssets.add(new AssetEnvelope(AssetBL.getWS(asset), itemType));
                }
            }

            ItemDTO item = new ItemDAS().find(mediationRecordLine.getItemId());

            CdrEnvelope cdr = new CdrEnvelopeBL()
                    .setProductId(item.getId())
                    .setProductName(item.getDescription())
                    .setCdrId(mediationRecordLine.getRecordKey())
                    .setDate(new Date(mediationRecordLine.getEventDate().toInstant().toEpochMilli()))
                    .setDuration(mediationRecordLine.getQuantity())
                    .setPrice(mediationRecordLine.getRatedPrice())
                    .setDescription(mediationRecordLine.getDescription())
                    .setTimestamp(mediationRecordLine.getEventDate().toInstant().toEpochMilli())
                    .setAccountcode(String.valueOf(pfh.getValue("accountcode")))
                    .setAmaflags(String.valueOf(pfh.getValue("amaflags")))
                    .setAnswer((Date) pfh.getValue("answer"))
                    .setBillsec((Integer) pfh.getValue("billsec"))
                    .setChannel(String.valueOf(pfh.getValue("channel")))
                    .setClid(String.valueOf(pfh.getValue("clid")))
                    .setDcontext(String.valueOf(pfh.getValue("dcontext")))
                    .setDisposition(String.valueOf(pfh.getValue("disposition")))
                    .setDst("+" + String.valueOf(pfh.getValue("dst")))
                    .setDstchannel(String.valueOf(pfh.getValue("dstchannel")))
                    .setEnd((Date) pfh.getValue("end"))
                    .setItemId(String.valueOf(pfh.getValue("itemId")))
                    .setLastapp(String.valueOf(pfh.getValue("lastapp")))
                    .setLastdata(String.valueOf(pfh.getValue("lastdata")))
                    .setSrc("+" + String.valueOf(pfh.getValue("src")))
                    .setStart((Date) pfh.getValue("start"))
                    .setUserfield(String.valueOf(pfh.getValue("userfield")))
                    .setNum(++cdrCounter)
                    .setAssets(cdrAssets, defaultAssetIdLabel)
                    .createCdrEnvelope();
            result.add(cdr);
        }
        return result;
    }

    private static String formatDate(Date date, String format) {
        return DateTimeFormat.forPattern(format).print(date.getTime());
    }
}
