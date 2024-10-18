package com.sapienter.jbilling.server.item;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * Version of ItemDependencyDTO safe for WS.
 *
 * @author Gerhard
 * @since 12/09/13
 */
@ApiModel(value = "Item Dependency Model", description = "ItemDependencyDTOEx Model")
public class ItemDependencyDTOEx implements Serializable {

    private Integer id;
    @NotNull(message = "validation.error.notnull")
    private ItemDependencyType type;

    private Integer itemId;
    @NotNull(message = "validation.error.notnull")
    @Min(value = 0, message = "validation.error.min,0")
    private Integer minimum;
    private Integer maximum;
    @NotNull(message = "validation.error.notnull")
    private Integer dependentId;  //maps to ItemDTO or ItemTypeDTO id

    private String dependentDescription;

    @ApiModelProperty(value = "The id of the item dependency entity")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The type of item dependency", required = true)
    public ItemDependencyType getType() {
        return type;
    }

    public void setType(ItemDependencyType type) {
        this.type = type;
    }

    @ApiModelProperty(value = "The id of the item for which this dependency is related to")
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @ApiModelProperty(value = "Minimum quantity purchased of the dependent item or category")
    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    @ApiModelProperty(value = "Maximum quantity purchased of the dependent item or category")
    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    @ApiModelProperty(value = "The id of the dependent entity")
    public Integer getDependentId() {
        return dependentId;
    }

    public void setDependentId(Integer dependentId) {
        this.dependentId = dependentId;
    }

    @ApiModelProperty(value = "The description of the dependent entity")
    public String getDependentDescription() {
        return dependentDescription;
    }

    public void setDependentDescription(String dependentDescription) {
        this.dependentDescription = dependentDescription;
    }

    @Override
    public String toString() {
        return "ItemDependencyDTOEx{" +
                "id=" + id +
                ", type=" + type +
                ", itemId=" + itemId +
                ", minimum=" + minimum +
                ", maximum=" + maximum +
                ", dependentId=" + dependentId +
                ", dependentDescription='" + dependentDescription + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemDependencyDTOEx)) return false;

        ItemDependencyDTOEx that = (ItemDependencyDTOEx) o;

        return nullSafeEquals(id, that.id) &&
                type == that.type &&
                nullSafeEquals(itemId, that.itemId) &&
                nullSafeEquals(minimum, that.minimum) &&
                nullSafeEquals(maximum, that.maximum) &&
                nullSafeEquals(dependentId, that.dependentId) &&
                nullSafeEquals(dependentDescription, that.dependentDescription);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(type);
        result = 31 * result + nullSafeHashCode(itemId);
        result = 31 * result + nullSafeHashCode(maximum);
        result = 31 * result + nullSafeHashCode(minimum);
        result = 31 * result + nullSafeHashCode(dependentId);
        result = 31 * result + nullSafeHashCode(dependentDescription);
        return result;
    }
}
