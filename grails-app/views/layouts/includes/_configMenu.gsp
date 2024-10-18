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
    <!-- Menu items, 'All' being first, remaining in alphabetical order -->
    <li class="${pageProperty(name: 'page.menu.item') == 'all' ? 'active' : ''}"> <!-- All -->
        <g:link controller="config">
            <g:message code="configuration.menu.all"/>
        </g:link>
    </li>
    <sec:ifAllGranted roles="CONFIGURATION_2000">
    <li class="${pageProperty(name: 'page.menu.item') == 'accountType' ? 'active' : ''}"><!-- Account Types -->
        <g:link controller="accountType" action="list">
            <g:message code="configuration.menu.accountType"/>
         </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2001">
    <li class="${pageProperty(name: 'page.menu.item') == 'partner' ? 'active' : ''}"><!-- Agent Commission -->
        <g:link controller="config" action="partnerCommission">
            <g:message code="configuration.menu.partner"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_157">
        <li class="${pageProperty(name: 'page.menu.item') == 'Authentication' ? 'active' : ''}"><!-- Authentication -->
        <g:link controller="authentication" action="listCIT">
            <g:message code="configuration.menu.authentication"/>
        </g:link>
        </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2002">
    <g:isRoot>
        <li class="${pageProperty(name: 'page.menu.item') == 'ApiUserDetail' ? 'active' : ''}"><!-- apiuserDetails -->
        <g:link controller="ApiUserDetail" action="list">
            <g:message code="configuration.menu.api.user.details"/>
        </g:link>
        </li>
    </g:isRoot>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2003">
    <li class="${pageProperty(name: 'page.menu.item') == 'billing' ? 'active' : ''}"><!-- Billing Process -->
        <g:link controller="billingconfiguration" action="index">
            <g:message code="configuration.menu.billing"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2004">
    <li class="${pageProperty(name: 'page.menu.item') == 'blacklist' ? 'active' : ''}"><!-- Blacklist -->
        <g:link controller="blacklist" action="list">
            <g:message code="configuration.menu.blacklist"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2005">
    <li class="${pageProperty(name: 'page.menu.item') == 'aging' ? 'active' : ''}"> <!-- Collections -->
        <g:link controller="config" action="aging">
            <g:message code="configuration.menu.collections"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2006">
    <li class="${pageProperty(name: 'page.menu.item') == 'company' ? 'active' : ''}"><!-- Company -->
        <g:link controller="config" action="company">
            <g:message code="configuration.menu.company"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_1911">
        <li class="${pageProperty(name: 'page.menu.item') == 'companies' ? 'active' : ''}"><!-- Companies -->
        <g:link controller="config" action="companies">
            <g:message code="configuration.menu.companies"/>
        </g:link>
        </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2007">
    <li class="${pageProperty(name: 'page.menu.item') == 'currency' ? 'active' : ''}"><!-- Currencies -->
        <g:link controller="config" action="currency">
            <g:message code="configuration.menu.currencies"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2008">
    <li class="${pageProperty(name: 'page.menu.item') == 'route' ? 'active' : ''}"><!-- Data Tables -->
        <g:link controller="route" action="list">
            <g:message code="configuration.menu.route"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2009">
    <li class="${pageProperty(name: 'page.menu.item') == 'email' ? 'active' : ''}"><!-- Email -->
        <g:link controller="config" action="email">
            <g:message code="configuration.menu.email"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2010">
    <li class="${pageProperty(name: 'page.menu.item') == 'uiEntityLogo' ? 'active' : ''}"><!-- Invoice Display -->
        <g:link controller="config" action="uiEntityLogo">
            <g:message code="configuration.menu.entity.logo"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2011">
    <li class="${pageProperty(name: 'page.menu.item') == 'enumerations' ? 'active' : ''}"><!-- Enumerations -->
        <g:link controller="enumerations">
            <g:message code="configuration.menu.enumerations"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2012">
    <li class="${pageProperty(name: 'page.menu.item') == 'usagePools' ? 'active' : ''}"><!-- Free Usage Pools -->
        <g:link controller="usagePool" action="list">
            <g:message code="configuration.menu.freeUsagePools"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2013">
    <li class="${pageProperty(name: 'page.menu.item') == 'invoices' ? 'active' : ''}"><!-- Invoice Display -->
        <g:link controller="config" action="invoice">
            <g:message code="configuration.menu.invoices"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2014">
    <sec:ifAllGranted roles="INVOICE_TEMPLATES_1805">
        <li class="${pageProperty(name: 'page.menu.item') == 'invoiceTemplates' ? 'active' : ''}"><!-- Invoice Templates -->
           <g:link controller="invoiceTemplate" action="list">
               <g:message code="configuration.menu.invoiceTemplates"/>
           </g:link>
        </li>
    </sec:ifAllGranted>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2015">
    <li class="${pageProperty(name: 'page.menu.item') == 'jobExecution' ? 'active' : ''}"><!-- Job Execution -->
        <g:link controller="jobExecution" action="list">
            <g:message code="configuration.menu.jobExecution"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2016">
    <li class="${pageProperty(name: 'page.menu.item') == 'languages' ? 'active' : ''}"><!-- Languages -->
        <g:link controller="language" action="list">
            <g:message code="configuration.menu.languages"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2017">
    <li class="${pageProperty(name: 'page.menu.item') == 'mediation' ? 'active' : ''}"><!-- Mediation -->
        <g:link controller="mediationConfig" action="list">
            <g:message code="configuration.menu.mediation"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2018">
    <li class="${pageProperty(name: 'page.menu.item') == 'metaFields' ? 'active' : ''}"> <!-- Meta Fields -->
        <g:link controller="metaFields">
            <g:message code="configuration.menu.metaFields"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2019">
    <li class="${pageProperty(name: 'page.menu.item') == 'metaFieldGroups' ? 'active' : ''}"><!-- Meta Field groups -->
        <g:link controller="metaFieldGroup" action="listCategories">
            <g:message code="configuration.menu.metaFieldGroups"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2020">
    <li class="${pageProperty(name: 'page.menu.item') == 'notification' ? 'active' : ''}"><!-- Notification -->
        <g:link controller="notifications">
            <g:message code="configuration.menu.notification"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2021">
    <li class="${pageProperty(name: 'page.menu.item') == 'orderChangeStatuses' ? 'active' : ''}"><!-- Order Change Statuses -->
        <g:link controller="config" action="orderChangeStatuses">
            <g:message code="configuration.menu.orderChangeStatuses"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2022">
    <li class="${pageProperty(name: 'page.menu.item') == 'orderChangeTypes' ? 'active' : ''}"><!-- Order Change Types -->
        <g:link controller="orderChangeType" action="list">
            <g:message code="configuration.menu.orderChangeTypes"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2023">
    <li class="${pageProperty(name: 'page.menu.item') == 'periods' ? 'active' : ''}"><!-- Order Periods -->
        <g:link controller="orderPeriod" action="list">
            <g:message code="configuration.menu.order.periods"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2024">
    <li class="${pageProperty(name: 'page.menu.item') == 'orderStatus' ? 'active' : ''}"><!-- Order Statuses -->
        <g:link controller="orderStatus" action="index">
            <g:message code="configuration.menu.orderStatus"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2025">
    <li class="${pageProperty(name: 'page.menu.item') == 'paymentMethod' ? 'active' : ''}"><!-- Payment Methods -->
        <g:link controller="paymentMethodType" action="list">
            <g:message code="configuration.menu.paymentMethod"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2026">
    <li class="${pageProperty(name: 'page.menu.item') == 'plugins' ? 'active' : ''}"><!-- Plugins -->
        <g:link controller="plugin">
            <g:message code="configuration.menu.plugins"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2027">
    <li class="${pageProperty(name: 'page.menu.item') == 'roles' ? 'active' : ''}"><!-- Roles -->
        <g:link controller="role" action="list">
            <g:message code="configuration.menu.roles"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2028">
    <li class="${pageProperty(name: 'page.menu.item') == 'rateCard' ? 'active' : ''}"><!-- Rate Cards -->
        <g:link controller="rateCard" action="list">
            <g:message code="configuration.menu.rate.card"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2029">
    <li class="${pageProperty(name: 'page.menu.item') == 'rating units' ? 'active' : ''}"><!-- Rating Unit -->
        <g:link controller="ratingUnit" action="list">
            <g:message code="configuration.menu.ratingUnit"/>
        </g:link>
    </li>
    </sec:ifAllGranted>

<sec:ifAllGranted roles="CONFIGURATION_1918">
    <g:isRoot>
        <li class="${pageProperty(name: 'page.menu.item') == 'Rating Schemes' ? 'active' : ''}"><!-- Rating Schemes -->
        <g:link controller="MediationRatingSchemeConfig" action="list">
            <g:message code="configuration.menu.rating.schemes"/>
        </g:link>
        </li>
    </g:isRoot>
</sec:ifAllGranted>

    <sec:ifAllGranted roles="CONFIGURATION_2030">
    <li class="${pageProperty(name: 'page.menu.item') == 'routeBasedConfiguration' ? 'active' : ''}"><!-- Route Rate Card -->
        <g:link controller="routeBasedRateCard" action="list">
            <g:message code="configuration.menu.routeBasedConfiguration"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2031">
    <li class="${pageProperty(name: 'page.menu.item') == 'routeTest' ? 'active' : ''}"><!-- Route Test -->
        <g:link controller="routeTest" action="list">
            <g:message code="configuration.menu.route.test"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_1912">
        <li class="${pageProperty(name: 'page.menu.item') == 'tools' ? 'active' : ''}"><!-- Tools -->
        <g:link controller="config" action="showTools">
            <g:message code="configuration.menu.tools"/>
        </g:link>
        </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2032">
    <li class="${pageProperty(name: 'page.menu.item') == 'users' ? 'active' : ''}"><!-- Users -->
        <g:link controller="user" action="list">
            <g:message code="configuration.menu.users"/>
        </g:link>
    </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2033">
    <g:isRoot>
        <li class="${pageProperty(name: 'page.menu.item') == 'Usage Rating Schemes' ? 'active' : ''}"><!-- Usage Rating Schemes -->
        <g:link controller="UsageRatingScheme" action="list">
            <g:message code="configuration.menu.usage.rating.schemes"/>
        </g:link>
        </li>
    </g:isRoot>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_1917">
        <li class="${pageProperty(name: 'page.menu.item') == 'bulkUpload' ? 'active' : ''}"><!-- Bulk Upload Prices -->
            <g:link controller="bulkUpload" action="uploadPrices">
                Bulk Upload Product
            </g:link>
        </li>
        <li class="${pageProperty(name: 'page.menu.item') == 'bulkDownload' ? 'active' : ''}"><!-- Bulk Download Prices -->
            <g:link controller="bulkDownload" action="downloadPrices">
                Bulk Download Price
            </g:link>
        </li>
    </sec:ifAllGranted>
    <sec:ifAllGranted roles="CONFIGURATION_2110">
       <li class="${pageProperty(name: 'page.menu.item') == 'Ad-hoc Invoice' ? 'active' : ''}"><!-- Ad-hoc Invoice -->
          <g:link controller="CustomInvoice" action="index">
               <g:message code="custom.invoice.config.menu"/>
          </g:link>
       </li>
    </sec:ifAllGranted>
</ul>
