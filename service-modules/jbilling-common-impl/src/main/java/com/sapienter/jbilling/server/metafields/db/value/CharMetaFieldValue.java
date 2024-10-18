/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields.db.value;

import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.security.CipherUtil;
import org.apache.commons.lang.ArrayUtils;
import com.sapienter.jbilling.common.FormatLogger;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.Arrays;

@Entity
@DiscriminatorValue("char")
public class CharMetaFieldValue extends MetaFieldValue<char[]> {
    private static final FormatLogger LOG = new FormatLogger(CharMetaFieldValue.class);
    private char[] value;

    public CharMetaFieldValue() {
    }

    public CharMetaFieldValue(MetaField name) {
        super(name);
    }

    @Column(name = "string_value", nullable = true)
    @Size(max = 1000, message = "validation.error.size,1000")
    public char[] getRawValue() {
        return value;
    }

    public void setRawValue(char[] value) {
        this.value = value;
    }

    @Transient
    public char[] getValue() {
        return getDecryptedValue(getRawValue());
    }

    @Transient
    public void setValue(char[] value) {
        if (null == value || 0 == value.length) {
            setRawValue(null);
        } else {
            char[] crip = getEncryptedValue(value);
            setRawValue(crip);
        }
    }

    private char[] getEncryptedValue(char[] value){
        if(null == value){
            return null;
        }
        else if (MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED.equals(this.getField().getFieldUsage())) {
            try {
                value = CipherUtil.encrypt(value);
            } catch (Exception e){
                LOG.debug("Exception in encrypting CharMetaFieldValue");
            }
        }
        return value;
    }

    private char[] getDecryptedValue(char[] value){
        if(null == value){
            return null;
        }
        else if (MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED.equals(this.getField().getFieldUsage())) {
            try {
                value = CipherUtil.decrypt(value);
            } catch (Exception e){
                LOG.debug("Exception in decrypting CharMetaFieldValue");
            }
        }
        return value;
    }

    public void clearValue() {  
    	if(value!=null && value.length!=0) {
    		Arrays.fill(this.value, ' ');
    	}
    }

    @Override
    @Transient
    public boolean isEmpty() {
        return ArrayUtils.isEmpty(value);
    }
}
