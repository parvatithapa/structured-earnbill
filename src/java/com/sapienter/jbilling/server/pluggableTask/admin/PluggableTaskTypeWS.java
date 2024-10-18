package com.sapienter.jbilling.server.pluggableTask.admin;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;
/**
 * 
 * @author Khurram Cheema
 *
 */
@ApiModel(value = "Plugin type data", description = "PluggableTaskTypeWS model")
public class PluggableTaskTypeWS implements java.io.Serializable{

	private Integer id;
	private String className;
	private Integer minParameters;
	private Integer categoryId;
	
	public PluggableTaskTypeWS(){
		
	}
	
	@Override
	public String toString(){
		return "PluggableTaskTypeWS [id: "+this.id+",className: "+this.className
				+", minParameters: "+this.minParameters
				+", categoryId: "+this.categoryId+" ]";
	}

	@ApiModelProperty(value = "Unique identifier of the plugin type", required = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "Fully qualified class name of the class for the plugin type", required = true)
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	@ApiModelProperty(value = "The minimum number of parameters for the plugin type", required = true)
	public Integer getMinParameters() {
		return minParameters;
	}

	public void setMinParameters(Integer minParameters) {
		this.minParameters = minParameters;
	}

	@ApiModelProperty(value = "Unique identifier of the plugin type category for the plugin type", required = true)
	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public boolean equals(Object object) {

		if (this == object) {
			return true;
		}
		if (!(object instanceof PluggableTaskTypeWS)) {
			return false;
		}
		PluggableTaskTypeWS pluginType = (PluggableTaskTypeWS) object;
		return nullSafeEquals(this.id, pluginType.id) &&
				nullSafeEquals(this.className, pluginType.className) &&
				nullSafeEquals(this.minParameters, pluginType.minParameters) &&
				nullSafeEquals(this.categoryId, pluginType.categoryId);
	}

	@Override
	public int hashCode() {

		int result = nullSafeHashCode(id);
		result = 31 * result + nullSafeHashCode(className);
		result = 31 * result + nullSafeHashCode(minParameters);
		result = 31 * result + nullSafeHashCode(categoryId);
		return result;
	}
}
