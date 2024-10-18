package com.sapienter.jbilling.server.pluggableTask.admin;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * 
 * @author Khurram Cheema
 *
 */
@ApiModel(value = "Plugin type category data", description = "PluggableTaskTypeCategoryWS model")
public class PluggableTaskTypeCategoryWS implements Serializable{
	
	private Integer id;
	private String interfaceName;
	
	public PluggableTaskTypeCategoryWS(){
		
	}

	@ApiModelProperty(value = "Unique identifier of the plugin type category", required = true)
	public Integer getId(){
		return this.id;
	}
	
	public void setId(Integer id){
		this.id = id;
	}

	@ApiModelProperty(value = "Fully qualified class name of the interface for the plugin type category", required = true)
	public String getInterfaceName(){
		return this.interfaceName;
	}
	
	public void setInterfaceName(String interfaceName){
		this.interfaceName = interfaceName;
	}

	@Override
	public String toString(){
		return "PluggableTaskTypeCategoryWS = [id: "+this.id
				+", interfaceName: "+this.interfaceName+" ]";
	}

	@Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof PluggableTaskTypeCategoryWS)) {
            return false;
        }
        PluggableTaskTypeCategoryWS pluginTypeCategory = (PluggableTaskTypeCategoryWS) object;
        return nullSafeEquals(this.id, pluginTypeCategory.id) &&
                nullSafeEquals(this.interfaceName, pluginTypeCategory.interfaceName);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(interfaceName);
        return result;
    }
}
