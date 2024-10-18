package com.sapienter.jbilling.server.process.signup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class SignupPlaceHolder {

    public enum OrderType {
        ONE_TIME, MONTHLY;
    }

    private Integer entityId;
    private Map<String, String> parameters;
    private SignupRequestWS signUpRequest;
    private SignupResponseWS signUpResponse;
    private Map<OrderType, List<Integer>> ordersByType;
    private Map<String, Integer> productOrderMap;

    public static SignupPlaceHolder of(SignupRequestWS signUpRequest, Integer entityId) {
        return new SignupPlaceHolder(signUpRequest, entityId);
    }

    private SignupPlaceHolder(SignupRequestWS signUpRequest, Integer entityId) {
        this.signUpRequest = signUpRequest;
        this.signUpResponse = new SignupResponseWS();
        this.entityId = entityId;
        this.parameters = new HashMap<>();
        this.ordersByType = new HashMap<>();
        this.productOrderMap = new HashMap<>();
    }

    public SignupRequestWS getSignUpRequest() {
        return signUpRequest;
    }

    public SignupResponseWS getSignUpResponse() {
        return signUpResponse;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    public void addParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    public Integer getPluginIntParamterByName(String paramName) {
        String value = parameters.get(paramName);
        if(StringUtils.isEmpty(value)) {
            return null;
        }
        if(!StringUtils.isNumeric(value)) {
            throw new IllegalArgumentException("Paramter value type is not integer");
        }
        return Integer.parseInt(value);
    }

    public String getPluginParamterByName(String paramName) {
        return parameters.get(paramName);
    }

    public void addProductCodeAndOrderId(String code, Integer orderId) {
        productOrderMap.put(code, orderId);
    }

    public Integer getOrderByProductCode(String code) {
        return productOrderMap.get(code);
    }

    public void addOrderId(OrderType type, Integer orderId) {
        List<Integer> orders = ordersByType.getOrDefault(type, new ArrayList<>());
        orders.add(orderId);
        if(!ordersByType.containsKey(type)) {
            ordersByType.put(type, orders);
        }
    }

    public List<Integer> getOrdersByType(OrderType type) {
        return ordersByType.get(type);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SignupPlaceHolder [entityId=");
        builder.append(entityId);
        builder.append(", parameters=");
        builder.append(parameters);
        builder.append(", signUpRequest=");
        builder.append(signUpRequest);
        builder.append(", signUpResponse=");
        builder.append(signUpResponse);
        builder.append("]");
        return builder.toString();
    }
}
