package com.sapienter.jbilling.rest;

/**
 * @author Vojislav Stanojevikj
 * @since 24-Oct-2016.
 */
final class RestQueryParameter<T> {

    private String parameterName;
    private T parameterValue;

    public RestQueryParameter(String parameterName, T parameterValue) {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public T getParameterValue() {
        return parameterValue;
    }

    @Override
    public String toString() {
        return "RestQueryParameter{" +
                "parameterName='" + parameterName + '\'' +
                ", parameterValue=" + parameterValue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestQueryParameter)) return false;

        RestQueryParameter<?> that = (RestQueryParameter<?>) o;

        if (parameterName != null ? !parameterName.equals(that.parameterName) : that.parameterName != null)
            return false;
        return !(parameterValue != null ? !parameterValue.equals(that.parameterValue) : that.parameterValue != null);

    }

    @Override
    public int hashCode() {
        int result = parameterName != null ? parameterName.hashCode() : 0;
        result = 31 * result + (parameterValue != null ? parameterValue.hashCode() : 0);
        return result;
    }
}
