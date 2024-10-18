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

<%@ page import="com.sapienter.jbilling.server.metafields.DataType" contentType="text/html;charset=UTF-8" %>

<%--
  Plan builder view

  This view doubles as a way to render partial page templates by setting the 'template' parameter. This
  is used as a workaround for rendering AJAX responses from within the web-flow.

  @author Brian Cowdery
  @since 01-Feb-2011
--%>

<g:if test="${params.template}">
    <!-- render the template -->
    <g:render template="${params.template}"/>
</g:if>

<g:else>
    <!-- render the main builder view -->
    <html>
    <head>
        <meta name="layout" content="builder"/>
		<r:require module="showtab"/>
		 <r:require module="errors" />
        <r:script disposition="head">
            $(document).ready(function() {
                $('#builder-tabs').tabs();
                $('#review-tabs').tabs();
                changeMetafieldDataType()
            });


            /*
                Controls for refreshing the main components of the view
            */
            var timeline = {
                refresh: function() {
                    return $.ajax({
                        type: 'GET',
                        url: '${createLink(action: 'edit')}',
                        data:{'_eventId': 'timeline'},
                        success: function(data) { $('#timeline').replaceWith(data); }
                    });
                }
            };

            var details = {
                refresh: function() {
                    if ($('#timeline').is(':visible')) {
                        return $.ajax({
                            type: 'GET',
                            url: '${createLink(action: 'edit')}',
                            data:{'_eventId': 'details'},
                            success: function(data) { $('#details-box').replaceWith(data); }
                         });
                    }
                    return undefined;
                }
            };
			
            var review = {
                refresh: function () {
                    return $.ajax({
                        type: 'GET',
                        url: '${createLink(action: 'edit')}',
                        data: {'_eventId': 'review'},
                        success: function(data) { $('#ui-tabs-review').replaceWith(data); }
                    });
                }
            };
            
           function changeMetafieldDataType(){
	           $('#metaField\\.dataType').on("change", function() {
	            if ($(this).val() == '${DataType.ENUMERATION}' || $(this).val() == '${DataType.LIST}') {
	                $('.field-name').hide().find('input').prop('disabled', 'true');
	                $('.field-enumeration').show().find('select').prop('disabled', '');
	                $('.field-filename').hide().find('input').prop('disabled', 'true')
	            } else if ($(this).val() == '${DataType.SCRIPT}'){
	                $('.field-name').show().find('input').prop('disabled', '');
	                $('.field-enumeration').hide().find('select').prop('disabled', 'true');
	                $('.field-filename').show().find('input').prop('disabled', '')
	            } else {
	                $('.field-name').show().find('input').prop('disabled', '');
	                $('.field-enumeration').hide().find('select').prop('disabled', 'true');
	                $('.field-filename').hide().find('input').prop('disabled', 'true')
	            }
	        	}).change();
			}
            
        </r:script>
    </head>
    <body>

    <g:if test="${plan?.id}">
        <content tag="top">
            <g:render template="timeline"/>
        </content>
    </g:if>
-
    <content tag="builder">
        <div id="builder-tabs">
            <ul>
                <li aria-controls="ui-tabs-details"><a href="${createLink(action: 'edit', event: 'details')}"><g:message code="builder.details.title"/></a></li>
                <li aria-controls="ui-tabs-products"><a href="${createLink(action: 'edit', event: 'products')}"><g:message code="builder.products.title"/></a></li>
            </ul>
        </div>
        
        <div class="btn-box ait-btn-box">
            <g:remoteLink class="submit save" action="edit" params="[_eventId: 'addMetaField']" update="ui-tabs-metafields" method="GET">
                <span><g:message code="button.new.metafield"/></span>
            </g:remoteLink>
        </div>
    </content>

    <content tag="review">
    	<div id="review-tabs">
            <ul>
                <li aria-controls="ui-tabs-review"><a href="${createLink(action: 'edit', event: 'review')}"><g:message code="builder.review.title"/></a></li>
                <li aria-controls="ui-tabs-metafields"><a href="${createLink(action: 'edit', event: 'metaFields')}"><g:message code="builder.metafields.title"/></a></li>
            </ul>
        </div>
    </content>

    </body>
    </html>
</g:else>