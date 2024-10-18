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

<%--
  Layout for an attribute name / value pair.

  Usage:

    <g:applyLayout name="form/checkbox">
        <content tag="header.class">className</content>
        <content tag="header.name.title">titleName</content>
        <content tag="header.name">name</content>
        <content tag="header.value.title">valueTitle</content>
        <content tag="header.value">value</content>
        <content tag="name">
            <g:textField class="className" title="someTitle" name="someName"/>
        </content>
        <content tag="value">
            <div class="inp-bg">
                <g:textField class="className" title="someTitle" name="someName"/>
            </div>
        </content>
    </g:applyLayout>


  @author Brian Cowdery
  @since  25-11-2010
--%>
<%@page defaultCodec="none" %>
<div class="row dynamicAttrs">

    <table>
        <thead>
            <tr>
                <td>
                    <div style="font-size: small" title="${pageProperty(name:'page.header.name.title')}" class="${pageProperty(name:'page.header.class')}">
                        <g:pageProperty name="page.header.name"/>
                    </div>
                </td>
                <td>
                    <div style="font-size: small" title="${pageProperty( name: 'page.header.value.title')}" class=" ${pageProperty(name: 'page.header.class')}">
                        <g:pageProperty name="page.header.value"/>
                    </div>
                </td>
            </tr>
        </thead>
        <tbody>
            <g:if test="${pageProperty( name: 'page.name') || pageProperty( name: 'page.value')}">
            <tr>
                <td>
                    <div class="inp-bg inp4">
                        <g:pageProperty name="page.name"/>
                    </div>
                </td>
                <td>
                    <g:pageProperty name="page.value"/>
                </td>
                <td>
                    <g:layoutBody/>
                </td>
            </tr>
            </g:if>
        </tbody>
    </table>
</div>