package com.sapienter.jbilling.server.customerInspector.domain;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@XmlRootElement(name = "metaFieldType")
@XmlAccessorType(XmlAccessType.NONE)
public class MetaFieldTypeField extends AbstractField {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(required = true)
    protected String label;

    public String getName() {
        return name;
    }

    @Override
    public Object getValue(Integer userId) {
        if (userId != null) {
            if (MetaFieldType.fromString(name) != null) {
                return getMetaFieldValueByFieldUsage(name, userId);
            }
        }
        return null;
    }

    public static List<Object> getMetaFieldValueByFieldUsage(String metaFieldType, Integer userId) {
        UserDTO userDTO = new UserDAS().find(userId);
        CustomerDTO customerDTO = userDTO.getCustomer();
        List<AccountInformationTypeDTO> accountInformationTypeList = new AccountInformationTypeDAS().getAvailableAccountInformationTypes(userDTO.getEntity().getId());
        Set<Object> customerAccountInforTypeSet = new TreeSet<>();

        Date now = new Date();
        for (AccountInformationTypeDTO accountInformationTypeDTO : accountInformationTypeList) {
            CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = customerDTO.getCustomerAccountInfoTypeMetaFields().stream()
                .filter(caitm -> (caitm.getMetaFieldValue().getField().getFieldUsage() != null && caitm.getMetaFieldValue().getField().getFieldUsage().name().equals(metaFieldType)) &&
                    caitm.getAccountInfoType().getId() == accountInformationTypeDTO.getId() &&
                    now.after(caitm.getEffectiveDate()))
                .max(Comparator.comparing(CustomerAccountInfoTypeMetaField::getEffectiveDate))
                .orElse(null);

            if (customerAccountInfoTypeMetaField != null) {
                customerAccountInforTypeSet.add(customerAccountInfoTypeMetaField.getMetaFieldValue().getValue());
            }
        }

        return new ArrayList<>(customerAccountInforTypeSet);
    }

    public String getLabel() {
        return label;
    }
}