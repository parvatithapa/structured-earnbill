package com.sapienter.jbilling.server.invoiceTemplate.report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignDataset;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;

import java.util.Map;

/**
 * Created by Klim on 09.02.14.
 */
class ParameterAggregator {

    private final HavingParametersProxy proxy;

    ParameterAggregator(final JasperDesign jasperDesign) {
        this.proxy = jasperDesign == null ? null : new HavingParametersProxy() {

            @Override
            public void addParameter(JRDesignParameter parameter) throws JRException {
                jasperDesign.addParameter(parameter);
            }
        };
    }

    ParameterAggregator(final JRDesignDataset dataSet) {
        this.proxy = dataSet == null ? null : new HavingParametersProxy() {

            @Override
            public void addParameter(JRDesignParameter parameter) throws JRException {
                dataSet.addParameter(parameter);
            }
        };
    }

    ParameterAggregator addParameter(String name, Class<?> clazz) {
        if (proxy != null) {
            try {
                JRDesignParameter parameter = new JRDesignParameter();
                parameter.setName(name);
                parameter.setValueClass(clazz);
                proxy.addParameter(parameter);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    ParameterAggregator addParameters(Map<String, Class<?>> parameters) {
        for (Map.Entry<String, Class<?>> entry : parameters.entrySet()) {
            addParameter(entry.getKey(), entry.getValue());
        }
        return this;
    }

    private interface HavingParametersProxy {

        void addParameter(JRDesignParameter parameter) throws JRException;
    }
}
