%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.process.db.PeriodUnitDTO" %>

<%@ page contentType="text/html;charset=UTF-8" %>


<div class="table-box">
    <table id="periods" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
            <th class="medium">
                    <g:message code="upload.products.description"/>
            </th>

        </thead>

        <tbody>

            <tr id="period-${1}" class="${selected == 1 ?'active' : ''}">
                <td>
                    <g:remoteLink class="cell double" controller="bulkUpload" action="showUploadProducts" id="1" before="register(this);"
                                  onSuccess="render(data, next);">
                        <strong>${"Default Prices"}</strong>
                    </g:remoteLink>
                </td>
            </tr>

            <tr id="period-${2}" class="${selected == 2 ?'active' : ''}">
                <td>
                <g:remoteLink class="cell double" controller="bulkUpload" action="showUploadProducts" id="2" before="register(this);"
                              onSuccess="render(data, next);">
                    <strong>${"Account Type Prices"}</strong>
                </g:remoteLink></td>
            </tr>

            <tr id="period-${3}" class="${selected == 3 ?'active' : ''}">
                <td>
                    <g:remoteLink class="cell double" controller="bulkUpload" action="showUploadProducts" id="3" before="register(this);"
                                  onSuccess="render(data, next);">
                        <strong>${"Customer Prices"}</strong>
                    </g:remoteLink>
                </td>
            </tr>

            <tr id="period-${4}" class="${selected == 4 ?'active' : ''}">
                <td>
                    <g:remoteLink class="cell double" controller="bulkUpload" action="showUploadProducts" id="4" before="register(this);"
                                  onSuccess="render(data, next);">
                        <strong>${"Plan Prices"}</strong>
                    </g:remoteLink>
                </td>
            </tr>

        </tbody>
    </table>
</div>

