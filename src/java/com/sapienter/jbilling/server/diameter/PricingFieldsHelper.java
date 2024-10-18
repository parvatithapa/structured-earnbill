package com.sapienter.jbilling.server.diameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sapienter.jbilling.server.item.PricingField;
import org.apache.commons.lang.StringUtils;


public class PricingFieldsHelper {
	private HashMap<String, PricingField> fields = new HashMap<String,PricingField>();
	
	public PricingFieldsHelper(List<PricingField> fields) {
		setFields(fields);
	}

    public PricingFieldsHelper(PricingField[] fields) {
        for (PricingField f : fields) {
            this.fields.put(f.getName(), f);
        }
    }

    public PricingFieldsHelper(String data) {
        PricingField[] fields = PricingField.getPricingFieldsValue(data);
        for (PricingField f : fields) {
            this.fields.put(f.getName(), f);
        }
    }
	
	public PricingField getField(String key) {
		return this.fields.get(key);
	}
	
	public Object getValue(String key) {
		if (!this.fields.containsKey(key)) {
			return null;
		}
		return this.fields.get(key).getValue();
	}
	
	public void add(PricingField field) {
		if (field != null
				&& StringUtils.isNotBlank(field.getName())
				&& !fields.containsKey(field.getName())) {
			fields.put(field.getName(), field);
		}
	}
	
	public void add(String name, String value) {
		PricingField field = new PricingField(name, value);
		add(field);
	}
	
	public void addIfNotBlank(String name, String value) {
		if (StringUtils.isNotBlank(value)) {
			add(name, value);
		}
	}

	public List<PricingField> getFields() {
		List<PricingField> result = new ArrayList<PricingField>();

		for (String key : fields.keySet()) {
			result.add(fields.get(key));
		}
		return result;
	}

    public PricingField[] getFieldsAsArray(){
        return getFields().toArray(new PricingField[getFields().size()]);
    }

    public void setFields(List<PricingField> pricingFields){
        for (PricingField f : pricingFields) {
            this.fields.put(f.getName(), f);
        }
    }
}