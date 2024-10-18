%{--This template in used to display metafields in proper format on the review enrollment page --}%
<%
    def leftColumnFields = []
    def rightColumnFields = []
    metaFields.eachWithIndex { field, index ->
        def fieldValue = metaFieldValue.find {
            mfv -> mfv.field.id == field.id }

        if(fieldValue){
            if(index > metaFields.size()/2){
                rightColumnFields << fieldValue
            } else {
                leftColumnFields << fieldValue
            }
        }
    }
%>

<div class="column" >
    <g:if test="${leftColumnFields.size() > 0}">
        <g:each in="${leftColumnFields}" var="varMetaField" >
            <g:render template="/metaFields/displayMetaField"
                      model="[metaField : varMetaField]"/>
        </g:each>
    </g:if>
</div>

<div class="column">
    <g:if test="${rightColumnFields.size() > 0}">
        <g:each in="${rightColumnFields}" var="varMetaField" >
            <g:render template="/metaFields/displayMetaField"
                      model="[metaField : varMetaField]"/>
        </g:each>
    </g:if>
</div>