<div id="ait-inner-${cit.id}" class="form-columns">
	<%
  	def citMetaFields =  cit?.metaFields?.sort { it.displayOrder }
   	def leftColumnFields = []
  	def rightColumnFields = []
  	citMetaFields.eachWithIndex { field, index ->
    if(index > citMetaFields.size()/2){
   		rightColumnFields << field
  	} else {
      	leftColumnFields << field
    }
    }
   	%>
   	<div class="column">
    	<g:if test="${leftColumnFields.size() > 0}">
           	<g:render template="/metaFields/editMetaFields" model="[
                                                availableFields: leftColumnFields,
                                                fieldValues: values,
                                                groupId: cit.id
                                        ]"/>
     	</g:if>
	</div>
 	<div class="column">
   		<g:if test="${rightColumnFields.size() > 0}">
       		<g:render template="/metaFields/editMetaFields" model="[
                                                availableFields: rightColumnFields,
                                                fieldValues: values,
                                                groupId: cit.id
                                        ]"/>
    	</g:if>
 	</div>
</div>
