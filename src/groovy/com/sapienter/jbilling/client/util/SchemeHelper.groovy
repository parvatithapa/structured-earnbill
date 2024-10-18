package com.sapienter.jbilling.client.util

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.DynamicAttributeLineWS;


class SchemeHelper {

    static void bindRatingScheme(UsageRatingSchemeWS ws,GrailsParameterMap params) {
        def lineIndex = 0
        def lineID, sequence
        DynamicAttributeLineWS lineWS

        params.fixedAttributes.each { k, v ->
            ws.fixedAttributes.put(k, v)
        }

        if (params.dynamicAttributes != null) {
            while (params.dynamicAttributes.containsKey(Integer.toString(lineIndex))) {
                lineWS = new DynamicAttributeLineWS()
                lineWS.attributes = new HashMap<>()
                lineID = params.dynamicAttributes.get(Integer.toString(lineIndex)).get("id")
                sequence = params.dynamicAttributes.get(Integer.toString(lineIndex)).get("sequence")

                if (!StringUtils.isEmpty(lineID))
                    lineWS.id = lineID.toInteger()

                lineWS.sequence = Integer.valueOf(sequence)

                params.dynamicAttributes.get(Integer.toString(lineIndex)).get("attributes").each { key, value ->
                    if (!StringUtils.isEmpty(key))
                        lineWS.attributes.put(key, value);
                }
                for (Map.Entry<String,String> entry : lineWS.attributes) {
                    if (!StringUtils.isEmpty(entry.value)) {
                        ws.dynamicAttributes.add(lineWS)
                        break
                    }
                }
                lineIndex++
            }
        }
    }
}

