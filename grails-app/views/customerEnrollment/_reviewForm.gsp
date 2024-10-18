<g:if test="${accountInformationTypes && accountInformationTypes.size()>0}">
    <g:each in="${accountInformationTypes}" var="ait">
        <div class="form-columns" id="ait-inner-4">

            <h3>${ait.getName()}</h3>
            <hr/>
            <%
                def aitMetaFields =  ait?.metaFields?.sort { it.displayOrder }
                %>
            <g:render template="displayReviewFormData" model="[metaFields:aitMetaFields, metaFieldValue:metaFields]"/>
        </div>

    </g:each>

    <div class="form-columns" id="ait-inner-4">
        <h3><g:message code="customer.enrollment.edit.enrollment.info"/> </h3>
        <hr/>
        <g:render template="displayReviewFormData" model="[metaFields:enrollmentMetaFields, metaFieldValue:metaFields]"/>
    </div>


    <div class="form-columns" >

        <h3><g:message code="customer.enrollment.edit.notes.agents.title"/></h3>
        <hr/>

        <div class="column" >
        <g:applyLayout name="form/text">
            <content tag="label">
                <g:message code="customer.enrollment.head.agent"/>
            </content>
            <g:message code="customer.enrollment.head.rate"/>
        </g:applyLayout>
        <g:each in="${customerEnrollment.customerEnrollmentAgents}" var="agent" status="idx">
            <g:applyLayout name="form/text">
                <content tag="label">
                    ${agent.partnerName}
                </content>
                ${agent.rate ? formatNumber(number: agent.rate, formatName: 'decimal.format') : ''}
            </g:applyLayout>
        </g:each>
        </div>

        <div class="column" >
            <g:applyLayout name="form/text">
                <content tag="label">
                    <g:message code="customer.enrollment.head.note"/>
                </content>
            </g:applyLayout>
            <div class="row">
                ${comment?:''}
            </div>
        </div>
    </div>
</g:if>



