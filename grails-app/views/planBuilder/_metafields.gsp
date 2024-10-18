<%@ page import="com.sapienter.jbilling.server.metafields.EntityType" %>
<div id="review-box">

    <div id="messages">
        <g:if test="${errorMessages}">
            <div class="msg-box error">
                <ul>
                    <g:each var="message" in="${errorMessages}">
                        <li>${message}</li>
                    </g:each>
                </ul>
            </div>

            <g:set var="errorMessages" value=""/>
            <ul></ul>
        </g:if>
    </div>

	<div>
          <ul id="metafield-ait">
              <g:each var="metaField" status="index" in="${product?.orderLineMetaFields}">
                <g:set var="editable" value="${index == params.int('newLineIndex')}"/>
                <g:formRemote name="mf-${index}-update-form" url="[action: 'edit']"
                              update="ui-tabs-metafields" method="GET">

                    <fieldset>
                        <g:hiddenField name="_eventId" value="updateMetaField"/>
                        <g:hiddenField name="execution" value="${flowExecutionKey}"/>
						
                        <li id="mf-${index}" class="mf ${editable ? 'active' : ''}">
                            <span class="description">${metaField.name? metaField.name : "-"}</span>
                            <span class="data-type">${metaField.dataType? metaField.dataType : "-"}</span>
                            <span class="mandatory">${metaField.mandatory?'Mandatory':'Not Mandatory'}</span>
                        </li>

                        <li id="mf-${index}-editor" class="editor ${editable ? 'open' : ''}" style="display: block;">
							
							 <div class="box">
                                 <% params.entityType = com.sapienter.jbilling.server.metafields.EntityType.ORDER_LINE.name(); %>
                                 <g:render template="/metaFields/editMetafield"
                                           model="[metaField: metaField,
                                                   entityType: params.entityType,
                                                   metaFieldType:metaField.dataType,
                                                   parentId: 'mf-'+index+'-update-form',
                                                   metaFieldIdx:index,
                                                   displayMetaFieldType: false
                                           ]" />

                                 <g:hiddenField name="index" value="${index}"/>
                             </div>
                              
                            <div class="btn-box">
                                <a class="submit save" onclick="$('#mf-${index}-update-form').submit();"><span><g:message
                                        code="button.update"/></span></a>
                                <g:remoteLink class="submit cancel" action="edit" params="[_eventId: 'removeMetaField', index: index]"
                                              update="ui-tabs-metafields" method="GET">
                                    <span><g:message code="button.remove"/></span>
                                </g:remoteLink>
                            </div>

                        </li>

                    </fieldset>

                </g:formRemote>
                <!-- This inline tag is necessary to close list with style block -->
	            <g:if test="${!editable}">
	                <script>
	                	$('#mf-' + ${index} + '-editor').toggle('blind');
	                </script>
                </g:if>
            </g:each>

            <g:if test="${!product?.orderLineMetaFields}">
                <li><em><g:message code="plan.no.order.metafields"/></em></li>
            </g:if>
        </ul>
    </div>

    <script type="text/javascript">
        $('#metafield-ait li.mf').click(function() {
            var id = $(this).attr('id');
            $('#' + id).toggleClass('active');
            $('#' + id + '-editor').toggle('blind');
        });
    </script>
</div>
<script type="text/javascript">
    showTabWithoutClickIfNeeded('ui-tabs-metafields');
</script>