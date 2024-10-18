package com.sapienter.jbilling.server.customerInspector.domain;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.user.db.*;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;

@XmlRootElement(name = "metaField")
@XmlAccessorType(XmlAccessType.NONE)
public class MetaFieldField extends AbstractField {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private String accountInformationType;

    @XmlAttribute(required = true)
    protected String label;

    public String getName() {
        return name;
    }

    @Override
    public Object getValue(Integer userId) {
        if (userId == null) {
            return null;
        }

        UserDTO userDTO = new UserDAS().find(userId);
        CustomerDTO customer = userDTO.getCustomer();
        if (StringUtils.isNotEmpty(this.accountInformationType)) {
            Date now = new Date();
            CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = customer.getCustomerAccountInfoTypeMetaFields().stream()
                .filter(caitm -> caitm.getMetaFieldValue().getField().getName().equals(name.trim()) &&
                    caitm.getAccountInfoType().getName().equalsIgnoreCase(this.accountInformationType) &&
                    now.after(caitm.getEffectiveDate()))
                .max(Comparator.comparing(CustomerAccountInfoTypeMetaField::getEffectiveDate))
                .orElse(null);

            return (customerAccountInfoTypeMetaField == null) ? null : customerAccountInfoTypeMetaField.getMetaFieldValue().getValue();
        }

        return this.getMetaFieldValue(customer, this.getApi().getMetaFieldsForEntity(EntityType.CUSTOMER.toString()));
    }

    private Object getMetaFieldValue(CustomerDTO dto, MetaFieldWS[] metafields) {
        Set<MetaField> metafieldsSet = MetaFieldBL.convertMetaFieldsToDTO(Arrays.asList(metafields), this.getCompanyId());
        MetaFieldValueWS[] values = MetaFieldBL.convertMetaFieldsToWS(metafieldsSet, dto);
        for (MetaFieldValueWS ws : values) {
            if (ws.getFieldName().trim().equalsIgnoreCase(this.name)) {
                return ws.getValue();
            }
        }

        return null;
    }

    public String getLabel() {
        return label;
    }
}