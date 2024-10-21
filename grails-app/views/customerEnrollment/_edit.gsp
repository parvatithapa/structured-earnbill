<g:each in="${accountInformationTypes}" var="ait" status="i">
    <div id="tabs-${i}" style="display: none">
        <g:if test="${i == 1 && accountInformationTypes.size() != 2}">
            <g:applyLayout name="form/checkbox">
                <content tag="label"><g:message code="label.same.as.service.information"/></content>
                <content tag="label.for"><g:message code="label.same.as.service.information"/></content>
                <g:checkBox class="cb checkbox" id="copyContent" name="copyCheckBox"/>
            </g:applyLayout>
            <g:hiddenField id="ciAitId" name="ciAitId" value="${ait.id}"/>
        </g:if>
        <g:if test="${i == 0 && accountInformationTypes.size() != 2}">
            <g:hiddenField id="biAitId" name="biAitId" value="${ait.id}"/>
        </g:if>

        <g:render template="/customer/aITMetaFields"
                  model="[ait: ait, aitVal: ait.id, values: customerEnrollment?.metaFields]"/>

        <div class="btn-box order-btn-box form-columns" style="width: 20%">
        <div id="custom-div" style="width: 45%; display: inline-block">
            <g:if test="${i > 0}">
                <a href="javascript:void(0)" class="submit previous prev-btn">
                    <span><g:message code="wizard.previous"/></span>
                </a>
            </g:if>

        </div>

        <div id="custom-div2" style="width: 45%;display: inline">
            <a href="javascript:void(0)" class="submit next next-btn">
                    <span><g:message code="wizard.next"/></span>
                </a>
        </div>
    </div>
    </div>
</g:each>

<div id="tabs-${accountInformationTypes.size() }" style="display: none">
    %{--this section used to display the enrollment specific metafield--}%

    <div  class="form-columns">
        <div class="column">
            <g:render template="/metaFields/editMetaFields" model="[ availableFields: enrollmentMetaFields, fieldValues: customerEnrollment?.metaFields ]"/>
        </div>
    </div>

    <div class="btn-box order-btn-box form-columns" style="width: 20%">
        <div id="custom-div" style="width: 45%; display: inline-block">
            <a href="javascript:void(0)" class="submit previous prev-btn">
                <span><g:message code="wizard.previous"/></span>
            </a>
        </div>

        <div id="custom-div2" style="width: 45%;display: inline">
            <a href="javascript:void(0)" class="submit next next-btn">
                <span><g:message code="wizard.next"/></span>
            </a>
        </div>
    </div>
</div>

<div id="tabs-${accountInformationTypes.size() + 1}" style="display: none">
    <div class="content">
        <g:render template="/customerEnrollment/notesAgentsForm" model="[customerEnrollment: customerEnrollment]"/>
    </div>

    <div class="btn-box order-btn-box form-columns" style="width: 20%">
        <div id="custom-div" style="width: 45%; display: inline-block">
            <a href="javascript:void(0)" class="submit previous prev-btn">
                <span><g:message code="wizard.previous"/></span>
            </a>
        </div>

        <div id="custom-div2" style="width: 45%;display: inline">
            <a href="javascript:void(0)" class="submit next next-btn">
                <span><g:message code="wizard.next"/></span>
            </a>
        </div>
    </div>

</div>

<div id="tabs-${accountInformationTypes.size() + 2}"  style="display: none">
%{--this section used to display review form--}%
    <g:if test="${enrollment}">
        <div class="content">
            <g:if test="${flash.info}">
                <div id="messages">
                    <br/>
                    <g:each in="${flash.info.split(";")}" var="message">
                        <div class="msg-box info">
                            <img src="${resource(dir:'images', file: "icon34.gif")}" alt="Information">
                            <strong>${message}</strong>
                        </div>
                    </g:each>
                </div>
            </g:if>
            <g:render template="/customerEnrollment/reviewForm" model="[accountInformationTypes:accountInformationTypes, metaFields:enrollment.getMetaFields(), enrollmentMetaFields:enrollmentMetaFields]"/>
        </div>

    </g:if>

    <div class="btn-box order-btn-box"
         style="width: 20%; margin-left: auto; margin-right: auto;">
        <a href="javascript:void(0)" class="submit edit prev-btn">
            <span><g:message code="customer.enrollment.edit.button"/></span>
        </a>
    </div>

</div>

