package com.sapienter.jbilling.server.usageRatingScheme.util;


import java.util.Optional;

public class AttributeDefinition implements IAttributeDefinition {

    private String name;
    private Type type;
    private boolean required;
    private InputType inputType;
    private AttributeIterable<?> iterable;

    public static AttributeDefinitionBuilder builder() {
        return new AttributeDefinitionBuilder();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public InputType getInputType() {
        return this.inputType;
    }

    @Override
    public AttributeIterable getIterable() {
        return iterable;
    }

    @Override
    public String toString() {
        return "Attribute{" + type.name() + ": " + name + (required ? "(required)" : "") + "}";
    }


    public static class AttributeDefinitionBuilder {

        private AttributeDefinition managedInstance = new AttributeDefinition();

        public AttributeDefinitionBuilder name(String name) {
            managedInstance.name = name;
            return this;
        }

        public AttributeDefinitionBuilder type(Type type) {
            managedInstance.type = type;
            return this;
        }

        public AttributeDefinitionBuilder inputType(InputType inputType) {
            managedInstance.inputType = inputType;
            return this;
        }

        public AttributeDefinitionBuilder required(boolean required) {
            managedInstance.required = required;
            return this;
        }

        public AttributeDefinitionBuilder iterable(AttributeIterable iterable) {
            managedInstance.iterable = iterable;
            return this;
        }

        public AttributeDefinition build() {
            validate();
            return managedInstance;
        }

        private void validate() {
            managedInstance.type = Optional.ofNullable(managedInstance.type)
                    .orElse(Type.STRING);

            managedInstance.inputType = Optional.ofNullable(managedInstance.inputType)
                    .orElse(InputType.TEXT);

            // throw error if name, required not populated
        }
    }
}
