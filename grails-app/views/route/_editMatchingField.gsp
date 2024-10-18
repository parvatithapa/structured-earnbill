%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.db.RouteDTO;" %>
<%@ page import="com.sapienter.jbilling.server.user.Required;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.cache.MatchType;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.RouteBL" %>

<div id="editMatchingFieldForm">

    <div class="heading">
        <strong><g:message code="matching.field.title"/></strong>
    </div>
    <div class="box">
        <div class="sub-box">
            <div class="column-hold">
                <g:form id="save-matching-field-form" name="save-matching-field-form" url="[controller: params.controller, action: 'saveMatchingField']">
                    <fieldset>
                        <div class="form-columns">
                            <div class="column single">
                                <g:hiddenField name="id" value="${matchingField?.id}"/>
                                <g:hiddenField name="routeId" value="${route?.id}"/>
                                <g:hiddenField name="routeRateCardId" value="${routeRateCard?.id}"/>
                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="bean.MatchingFieldWS.description"/> <span>*</span></content>
                                    <content tag="label.for">description</content>
                                    <g:textField name="description" class="field" value="${matchingField?.description}"/>
                                </g:applyLayout>
                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="bean.MatchingFieldWS.orderSequence"/> <span >*</span> </content>
                                    <content tag="label.for">orderSequence</content>
                                    <g:textField name="orderSequence" class="field" value="${matchingField?.orderSequence}"/>
                                </g:applyLayout>
                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="bean.MatchingFieldWS.mediationField"/> <span >*</span></content>
                                    <content tag="label.for">mediationField</content>
                                    <g:textField name="mediationField" class="field" value="${matchingField?.mediationField}"/>
                                </g:applyLayout>
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="bean.MatchingFieldWS.required"/></content>
                                    <content tag="label.for">required</content>
                                    <content tag="include.script">true</content>
                                    <g:select        name = "required"
                                                     from = "${Required.values()}"
                                                optionKey = "id"
                                              optionValue = "name"
                                                    value = "${Required.values().find{val-> val.id == matchingField?.required}?.name}" />
                                </g:applyLayout>
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="bean.MatchingFieldWS.type"/></content>
                                    <content tag="label.for">type</content>
                                    <content tag="include.script">true</content>
                                    <g:select  name = "type"
                                               from = "${MatchingFieldType.values()}"
                                              value = "${matchingField?.type? MatchingFieldType.valueOf(matchingField?.type) : ''}"/>
                                </g:applyLayout>
                                <g:applyLayout name="form/select">
                                    <content tag="label"><g:message code="bean.MatchingFieldWS.matchingField"/></content>
                                    <content tag="label.for">matchingField</content>
                                    <content tag="include.script">true</content>
                                    <g:select  name = "matchingField"
                                               from = "${availMatchingFields}"
                                              value = "${matchingField?.matchingField}"/>
                                </g:applyLayout>
                            </div>
                        </div>
                    </fieldset>
                    <!-- spacer -->
                    <div>
                        <br/>&nbsp;
                    </div>
                </g:form>
            </div>
        </div>

        <div class="btn-box buttons">
            <ul>
                <li>
                    <a class="submit save button-secondary" onclick="$('#save-matching-field-form').submit();">
                        <span><g:message code="button.save"/></span>
                    </a>
                </li>

                <g:if test="${!isNew}">
                    <li>
                        <a class="submit cancel" onclick="$('#editMatchingFieldForm').empty()">
                            <span>
                                <g:message code="button.cancel"/>
                            </span>
                        </a>
                    </li>
                </g:if>

            </ul>
        </div>
    </div>
</div>

 <script>
     function updateDivs(data){
         if(data.showMatchingField != undefined|| data.testRoute != undefined){
             jQuery('#matching-field-holder').html(data.showMatchingField);
             jQuery('#test-route-holder').html(data.testRoute);
         }else{

             jQuery('#matching-field-holder').html(data)
         }
         jQuery('#editMatchingFieldForm').empty();
     }
 </script>
