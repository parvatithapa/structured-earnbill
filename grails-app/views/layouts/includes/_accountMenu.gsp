%{--
JBILLING CONFIDENTIAL
_____________________

[2003] - [2013] Enterprise jBilling Software Ltd.
All Rights Reserved.

NOTICE:  All information contained herein is, and remains
the property of Enterprise jBilling Software.
The intellectual and technical concepts contained
herein are proprietary to Enterprise jBilling Software
and are protected by trade secret or copyright law.
Dissemination of this information or reproduction of this material
is strictly forbidden.
--}%
<ul class="list">
    <li class="${pageProperty(name: 'page.menu.item') == 'mydetails' ? 'active' : ''}">
        <g:link controller="myAccount" >
            <g:message code="account.menu.mydetails"/>
        </g:link>
    </li>
    <li class="${pageProperty(name: 'page.menu.item') == 'tabs' ? 'active' : ''}">
        <g:link controller="tabConfig">
            <g:message code="account.menu.tabs"/>
        </g:link>
    </li>
</ul>
