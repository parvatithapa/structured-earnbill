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

<%@ page import="com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.pricing.db.RatingUnitDTO" contentType="text/html;charset=UTF-8" %>

<%--
  Shows edit form for a route based rate card type.

  @author Rahul Asthana

--%>

<div class="column-hold">
    <g:set var="isNew" value="${!routeRateCard || !routeRateCard?.id || routeRateCard?.id == 0}"/>

    <div class="heading">
        <g:if test="${isNew}">
            <strong><g:message code="route.rate.card.add.title"/></strong>
        </g:if>
        <g:else>
            <strong><g:message code="route.rate.card.edit.title"/></strong>
        </g:else>
    </div>

    <g:uploadForm id="route-based-rate-card-form" name="route-based-rate-card-form" url="[action: 'save']">

    <div class="box">
        <div class="sub-box">
          <fieldset>
            <div class="form-columns">
                <g:hiddenField name="id" value="${routeRateCard?.id}"/>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="route.based.rate.card.name"/></content>
                    <content tag="label.for">name</content>
                    <g:textField class="field" name="name" value="${routeRateCard?.name}"/>
                </g:applyLayout>

                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="route.based.rate.card.unit"/></content>
                    <content tag="label.for">ratingUnitId</content>
                    <content tag="include.script">true</content>
                    <g:select from="${RatingUnitDTO.findAllByCompany(new CompanyDTO(session['company_id']))}"
                              optionKey="id"
                              optionValue="${{it.name}}"
                              name="ratingUnitId"
                              value="${routeRateCard?.ratingUnitId}"/>
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="route.rate.card.csv.file"/></content>
                    <g:applyLayout name="form/fileupload">
                        <content tag="input.name">routeRates</content>
                    </g:applyLayout>
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label">&nbsp;</content>
                    <a href="${resource(dir:'examples', file:'example_route_rate_card.csv')}">example_route_rate_card.csv</a>
                </g:applyLayout>
            </div>
        </fieldset>
      </div>
        <div class="btn-box buttons">
            <ul>
                <li><a class="submit save button-primary" onclick="$('#route-based-rate-card-form').submit();"><span><g:message code="button.save"/></span></a></li>
                <li><a class="submit cancel" onclick="$('#column2 div').empty();"><span><g:message code="button.cancel"/></span></a></li>
            </ul>
        </div>

    </div>

    </g:uploadForm>


</div>
