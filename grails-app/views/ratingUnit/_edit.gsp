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

<%@ page contentType="text/html;charset=UTF-8" %>


<div class="column-hold">
    
    <g:set var="isNew" value="${!ratingUnit || !ratingUnit?.id || ratingUnit?.id == 0}"/>
    
    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="ratingUnit.add.title"/>
            </g:if>
            <g:else>
                <g:message code="ratingUnit.edit.title"/>
            </g:else>
        </strong>
    </div>

    <g:form id="save-unit-form" name="unit-form" url="[action: 'save']" useToken="true">
    <input type="hidden" name="isNew" value="${isNew}">
    <div class="box">
        <div class="sub-box">
          <fieldset>
            <div class="form-columns">
                <g:hiddenField name="id" value="${ratingUnit?.id}"/>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="ratingUnit.name"/></content>
                    <content tag="label.for">name</content>
                    <g:textField class="field" name="name" value="${ratingUnit?.name}"/>
                </g:applyLayout>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="ratingUnit.priceUnitName"/></content>
                    <content tag="label.for">priceUnitName</content>
                    <g:textField class="field" name="priceUnitName" value="${ratingUnit?.priceUnitName}"/>
                </g:applyLayout>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="ratingUnit.incrementUnitName"/></content>
                    <content tag="label.for">incrementUnitName</content>
                    <g:textField class="field" name="incrementUnitName" value="${ratingUnit?.incrementUnitName}"/>
                </g:applyLayout>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="ratingUnit.incrementUnitQuantity"/></content>
                    <content tag="label.for">incrementUnitQuantity</content>
                    <g:textField class="field" name="incrementUnitQuantity" value="${ratingUnit?.incrementUnitQuantity}"/>
                </g:applyLayout>
                
            </div>
        </fieldset>
      </div>

        <div class="btn-box buttons">
            <ul>
                <li><a class="submit save button-primary" onclick="$('#save-unit-form').submit();"><span><g:message code="button.save"/></span></a></li>
                <li><a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a></li>
            </ul>
        </div>
    </div>

    </g:form>
</div>
