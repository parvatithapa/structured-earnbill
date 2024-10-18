<div class="box-cards">
    <div class="box-cards-title">
        <a class="btn-open">
        <g:if test="${user}">
        <g:set var="customer" value="${user.customer}"/>
        <g:set var="accountType" value="${customer.accountType}"/>
            <span>${user?.userName}
            <g:if test="${isBlacklisted}">
                <em style="color: #ff0000"><g:message code="enrollment.blacklisted.message"/></em>
            </g:if>
            </span>
        </g:if>
    <g:else>
        <g:set var="accountType" value="${enrollment?.accountType}"/>
        <span>Enrollment - ${enrollment?.id}</span>
    </g:else>


        </a>
</div>
    <div class="box-card-hold" style="display: none;">
        <div class="content">

            <g:each in="${accountType.informationTypes}" var="ait">
                <%
                    def aitMetaFields =  ait?.metaFields?.sort { it.displayOrder }
                    def leftColumnFields = []
                    def rightColumnFields = []
                    aitMetaFields.eachWithIndex { field, index ->
                        def fieldValue = metaFields.find {
                            mfv -> mfv.field.id == field.id }

                        if(fieldValue){
                            if(index > aitMetaFields.size()/2){
                                rightColumnFields << fieldValue
                            } else {
                                leftColumnFields << fieldValue
                            }
                        }
                    }
                %>

                <div class="column">
                    <g:if test="${leftColumnFields.size() > 0}">
                        <g:each in="${leftColumnFields}" var="varMetaField" >
                            <g:render template="/metaFields/displayMetaField"
                                      model="[metaField : varMetaField, companyId:companyId]"/>
                        </g:each>
                    </g:if>
                </div>
                <div class="column">
                    <g:if test="${rightColumnFields.size() > 0}">
                        <g:each in="${rightColumnFields}" var="varMetaField" >
                            <g:render template="/metaFields/displayMetaField"
                                      model="[metaField : varMetaField, companyId:companyId]"/>
                        </g:each>
                    </g:if>
                </div>

            </g:each>
            <div class="btn-box">
                <g:if test="${!isBlacklisted}">
                    <div class="row">
                        <g:link controller="customerEnrollment" action="updateEnrollmentForm" params="[accountTypeId:accountTypeId, userId:user?.id, enrollmentId:enrollment?.id, entityId:entityId]" class="submit order"><g:message code="enrollment.edit.customer.select"/></g:link>
                    </div>
                </g:if>
            </div>
        </div>
    </div>
</div>