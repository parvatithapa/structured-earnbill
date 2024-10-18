<%@ page import="com.sapienter.jbilling.server.pricing.cache.MatchingFieldType" %>
<div class="form-edit">
    <div class="heading">
        <strong><g:message code="route.test"/></strong>
    </div>
    <div class="form-hold">
        <g:formRemote id="find-route" name="find-route" url="[controller: 'routeBasedRateCard', action: 'findPriceByRouteRateCard']" update="test-route-result">
            <g:hiddenField name="routeRateCardId" value="${routeRateCardId}"/>
            <fieldset>
                <div class="form-columns">
                    <div class="column single">
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="route.rate.card.duration"/></content>
                            <content tag="label.for">business.duration</content>
                            <g:textField name="business.duration" class="field" value=""/>
                        </g:applyLayout>
                            <g:each in="${matchingFields}">
                                <g:if test="${it?.type.equals(MatchingFieldType.ACTIVE_DATE)}">
                                    <g:applyLayout name="form/date">
                                        <content tag="label">${it?.description}</content>
                                        <content tag="label.for">test.${it?.mediationField}</content>
                                        <g:textField name="test.${it?.mediationField}" class="field" value=""/>
                                    </g:applyLayout>
                                </g:if>
                                <g:else>
                                    <g:applyLayout name="form/input">
                                        <content tag="label">${it?.description}</content>
                                        <content tag="label.for">test.${it?.mediationField}</content>
                                        <g:textField name="test.${it?.mediationField}" class="field" value=""/>
                                    </g:applyLayout>
                                </g:else>
                            </g:each>
                    </div>
                </div>
            </fieldset>
            <!-- spacer -->
            <div>
                <br/>&nbsp;
            </div>

            <div id="test-route-result" class="center error-text-message" >

            </div>

            <!-- spacer -->
            <div>
                <br/>&nbsp;
            </div>
        </g:formRemote>
        <div class="btn-box buttons" style="list-style:none outside none">
            <ul>
                <li><a class="submit save" onclick="$('#find-route').submit();"><span><g:message code="button.test"/></span></a></li>
            </ul>
        </div>
    </div>

</div>

<!-- spacer -->
<div>
    <br/>&nbsp;
</div>

