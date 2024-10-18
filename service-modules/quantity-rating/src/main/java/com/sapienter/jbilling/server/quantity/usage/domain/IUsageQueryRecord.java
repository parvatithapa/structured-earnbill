package com.sapienter.jbilling.server.quantity.usage.domain;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

public interface IUsageQueryRecord {

    DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyyMMdd");

    Integer getUserId();

    Integer getEntityId();

    Integer getItemId();

    String getResourceId();

    String getMediationProcessId();

    Date getStartDate();

    Date getEndDate();

    default String getKey() {

        StringBuilder sb1 = new StringBuilder("[");
        StringBuilder sb2 = new StringBuilder("[");

        sb1.append("User Item Entity");
        sb2.append(getUserId()).append(' ')
            .append(getItemId()).append(' ')
            .append(getEntityId());

        if (StringUtils.isNotEmpty(getResourceId())) {
            sb1.append(" Resource");
            sb2.append(' ').append(getResourceId());
        }

        if (getStartDate() != null && getEndDate() != null) {
            sb1.append(" Start End");
            sb2.append(' ').append(dateFormatter.print(getStartDate().getTime()));
            sb2.append(' ').append(dateFormatter.print(getEndDate().getTime()));
        }

        return sb1.append("]:").append(sb2).append(']').toString();
    }
}
