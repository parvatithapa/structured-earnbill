%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="org.joda.time.DateTime;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldValueWS;java.util.Arrays; com.sapienter.jbilling.server.user.db.UserDTO; com.sapienter.jbilling.server.user.UserBL;com.sapienter.jbilling.server.user.db.CompanyDTO; org.hibernate.criterion.CriteriaSpecification; com.sapienter.jbilling.server.util.EnumerationBL; com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.util.db.EnumerationValueDTO;" %>
<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS;" %>
<%@ page import= "java.util.Arrays; java.util.ArrayList;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.PlanDTO;" %>

<style>
   #crm-report select, #crm-report .select-holder{
        width:221px;
        padding-left: 2px;
   }
</style>

<%--
  crm-user report

  @author Nitin Yewale
  @since  04-Nov-2022
--%>

<%
    /* Fetching all jbilling users */
    def users =  new UserBL().getAllUsers(session['company_id'] as Integer);
    def List<EnumerationValueDTO> governorates;

    /* fetching all Governorate type Enumeration values */
    EnumerationDTO enumerationDTO = new EnumerationBL().getEnumerationByName("Governorate", session['company_id'] as Integer)

    if(enumerationDTO != null){
        governorates = enumerationDTO.getValues();
    }

    /*fetching all userLevel MetaField*/
   def List<MetaFieldValueWS> userMetaFieldWS= new UserBL(session['user_id'] as Integer).getUserWS().getMetaFields()

     def userDTOs =  new UserBL().getAllUsers(session['company_id'] as Integer);
        def activeUsersName = new ArrayList<>();
        def inactiveUsersName = new ArrayList<>();
        for (UserDTO userDto : userDTOs) {
            if (userDto.accountDisabledDate == null && userDto.accountLockedTime == null) {
                   activeUsersName.add(userDto.userName);
            } else {
                   inactiveUsersName.add(userDto.userName);
            }
    }

   /* Fetching  all plans */
   def plans =  PlanDTO.createCriteria().list() {
       createAlias("item","item")
       createAlias("item.entities","entities", CriteriaSpecification.LEFT_JOIN)
       and {
            'in'('entities.id', session['company_id'])
       }
       order('id', 'asc')
   }

    def actions = new ArrayList<>();
    def processTypes = new ArrayList<>();
    def source = new ArrayList<>();

    // Actions
    actions.addAll(Arrays.asList(
              'Recharge'
            , 'Refund of Recharge'
            , 'Buy Subscription'
            , 'Refund of Buy Subscription'
            , 'SIM Reissue'
            , 'Refund of SIM Reissue'
            , 'Plan upgrade request'
            , 'Refund of Plan upgrade request'
            , 'Plan downgrade request'
            , 'Refund of Plan downgrade request'
            , 'Top up'
            , 'Refund of wallet top up'
    ));

    // Process Types
    processTypes.addAll(Arrays.asList(
            'Plan price'
            , 'Top up'
            , 'Sim Card Fee'
            , 'Downgrade Fee'
            , 'SIM reissue fee'
            , 'Modem'
    ));

    //Transaction Source
    source.addAll(Arrays.asList('Refund'));
%>

<g:set var="loggedInUserName" value="${new UserBL(session['user_id'] as Integer).getDto().getUserName()}"/>

<div class="form-columns" id="crm-report">
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start.date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field" name="start_date" value="${formatDate(date: new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).toDate(), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end.date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date" value="${formatDate(date: new DateTime().dayOfMonth().withMaximumValue().toDate(), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="subscriber.number"/></content>
        <content tag="label.for">subscriber_number</content>
        <g:textField name="subscriber_number" class="field"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.package.name"/></content>
        <content tag="label.for">plan_name</content>
        <content tag="include.script">true</content>
        <g:select
            from="${plans}"
            name="plan_name"
            optionKey="${{it.item.description}}"
            optionValue="${{it.item.description}}"
            noSelection="['':message(code: 'select.option.default.value.name')]"
        />
    </g:applyLayout>

    <g:if test="${SpringSecurityUtils.ifNotGranted("${PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS}")}">
        <g:each var='userMetaField' in="${userMetaFieldWS}">
            <g:if test="${userMetaField.fieldName.equals('Governorate')}">
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="governorate.name"/></content>
                    ${userMetaField.value}
                    <g:hiddenField class="field" name="governorate" value="${userMetaField.value}"/>
                </g:applyLayout>
            </g:if>
        </g:each>
        <g:applyLayout name="form/text">
            <content tag="label"><g:message code="user.name"/></content>
            ${loggedInUserName}
            <g:hiddenField class="field" name="user_name" value="${loggedInUserName}"/>
        </g:applyLayout>
    </g:if>
    <g:else>
        <g:applyLayout name="form/select" >
              <content tag="label"><g:message code="governorate.name"/></content>
               <content tag="label.for">governorate</content>
               <content tag="include.script">true</content>
               <g:select
                    from="${governorates}"
                    name="governorate"
                    id="governorate"
                    onChange= "getUsers()"
                    optionKey="${{ it.value}}"
                    optionValue="${{it.value}}"
                    noSelection="['':message(code: 'select.option.default.value.name')]"/>
        </g:applyLayout>
        <g:applyLayout name="form/select_multiple" >
              <content tag="label"><g:message code="active.user.name"/></content>
               <content tag="label.for">user_name</content>
               <content tag="include.script">true</content>
               <g:select
                    multiple="true"
                    from="${activeUsersName}"
                    name="user_name"
                    optionKey="${{ it}}"
                    optionValue="${{it}}"
                    />
        </g:applyLayout>

        <g:applyLayout name="form/select_multiple" >
              <content tag="label"><g:message code="inactive.user.name"/></content>
               <content tag="label.for">inactive_user_name</content>
               <content tag="include.script">true</content>
               <g:select
                    multiple="true"
                    from="${inactiveUsersName}"
                    name="user_name"
                    id="inactive_user_name"
                    />
        </g:applyLayout>

    </g:else>

    <g:applyLayout name="form/select_multiple" >
        <content tag="label"><g:message code="process.type"/></content>
        <content tag="label.for">process_type</content>
        <content tag="include.script">true</content>
        <g:select id="process_type"
            name="process_type"
            multiple="true"
            from="${processTypes}"
        />
    </g:applyLayout>

    <g:applyLayout name="form/select_multiple" >
    <content tag="label"><g:message code="report.action"/></content>
    <content tag="label.for">actions</content>
    <content tag="include.script">true</content>
        <g:select id="actions"
            name="actions"
            multiple="true"
            from="${actions}"
        />
    </g:applyLayout>

    <g:applyLayout name="form/select" >
        <content tag="label"><g:message code="report.transaction.type"/></content>
        <content tag="label.for">trn_source</content>
        <content tag="include.script">true</content>
        <g:select id="trn_source"
            name="trn_source"
            from="${source}"
            noSelection="['':message(code: 'select.option.default.value.name')]"
        />
    </g:applyLayout>

    <input id="entityNames" type="hidden" name="entityNames" value="" />
    %{--If the user can not choose the entity (on show.gsp) run for the current entity--}%
    <g:if test="${!childEntities || company?.parent}">
        <g:hiddenField name="childs" value="${company.id}" />
    </g:if>

<script type="text/javascript">

function getUsers(){
    const governorate = document.getElementById('governorate').value
    const userStatusActive = 'Active';
    const userStatusInactive = 'Inactive';
    const activeUserId = 'user_name';
    const inactiveUserId = 'inactive_user_name';

    $.ajax({
        url: '${createLink(action: 'getUsersLoginNameStatusAndGovernorate')}',
        success : function(data){
        var users = data;
           users=eval(users.replace(/UserRowMapper/g,'')
                      .replace(/\)/g, '\"}')
                      .replace(/\(/g, '{')
                      .replace(/\=/g, ':\"')
                      .replace(/\,/g, '\",')
                      .replace(/\}\",/g, '},'));

           if(governorate.trim() != ''){
               setUserName(users, userStatusActive, activeUserId, governorate);
               setUserName(users, userStatusInactive, inactiveUserId, governorate);
           }else{
               setUserName(users, userStatusActive, activeUserId, governorate);
               setUserName(users, userStatusInactive, inactiveUserId, governorate);
           }
        }
    });
}

function setUserName(users, status, id, governorate){
    var filteredUsers;
    if(governorate!=''){
        filteredUsers= users.filter(user => user.status === status && user.governorate === governorate)
                            .map(user => user.loginName);
    }else{
        filteredUsers= users.filter(user => user.status===status)
                            .map(user=> user.loginName);
    }
    var usersDropdown = document.getElementById(id);
    usersDropdown.innerHTML='';
    for(var i=0; i< filteredUsers.length; i++){
        var newUserOption = document.createElement('option');
        newUserOption.text = filteredUsers[i];
        newUserOption.value = filteredUsers[i];
        usersDropdown.add(newUserOption);
    }
}
</script>
</div>
