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

<%@ page import="com.sapienter.jbilling.server.item.ItemDTOEx; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.metafields.db.MetaFieldValue; com.sapienter.jbilling.server.item.db.PlanDAS; com.sapienter.jbilling.server.item.PlanBL; com.sapienter.jbilling.server.util.EnumerationBL; com.sapienter.jbilling.server.util.db.EnumerationValueDTO; com.sapienter.jbilling.server.item.ItemTypeBL; com.sapienter.jbilling.server.item.ItemTypeWS; com.sapienter.jbilling.server.item.ItemBL; com.sapienter.jbilling.server.item.db.ItemDTO; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.user.db.CompanyDAS; java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<%--
  Summary Billing Register

  @author Gerhard Maree
  @since  21-Sep-2015
--%>

<div class="form-columns">

    %{-- Negation of test to display child entities in _show.gsp --}%
    <input type="hidden" name="entity_id" value="${!(childEntities?.size() > 0 && company?.parent == null) ? session['company_id'] : 0}" />

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start_date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field" name="start_date" value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().minusMonths(1), session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end_date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date" value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <%
        def items = ItemDTO.executeQuery (
                "select i from ItemDTO as i, StringMetaFieldValue smf " +
                        " join i.metaFields mf join i.entities e" +
                        " where (e.id="+session['company_id']+" or e.parent.id="+session['company_id']+")"+
                        " and smf.field.name='COMMODITY' and length(smf.value) > 0"+
                        " and mf.id=smf.id"
        )
    %>
    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.billing.register.label.item"/></content>
        <content tag="label.for">item_id</content>
        <content tag="include.script">true</content>
        <g:select id="item" name="item_id" from="${items}" optionKey="id" optionValue="${{it.description +'(' + it.id+')'}}" noSelection="${['':'All']}" />
    </g:applyLayout>
    <input id="itemName" type="hidden" name="itemName" value="" />

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.billing.register.label.plan"/></content>
        <content tag="label.for">plan_id</content>
        <content tag="include.script">true</content>
        <g:select id="planSelect" name="plan_id" from="${new PlanDAS().findAllActive(session['company_id']).collect {[id: it.id, description: it.item.description]}}" optionKey="id" optionValue="${{it.description +'(' + it.id+')'}}" noSelection="${['':'All']}" />
    </g:applyLayout>
    <input id="planName" type="hidden" name="planName" value="" />

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.billing.register.label.state"/></content>
        <content tag="label.for">state</content>
        <content tag="include.script">true</content>
        <g:select name="state" from="${new EnumerationBL().getEnumerationByName('STATE', session['company_id'])?.values}" optionKey="value" optionValue="value" noSelection="${['':'All']}" />
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.billing.register.label.division"/></content>
        <content tag="label.for">division</content>
        <content tag="include.script">true</content>
        <g:select name="division" from="${new EnumerationBL().getEnumerationByName('DIVISION', session['company_id'])?.values}" optionKey="value" optionValue="value" noSelection="${['':'All']}" />
    </g:applyLayout>

    <input id="entityNames" type="hidden" name="entityNames" value="" />
</div>

<script type="text/javascript">
    $(function() {
        $("#item").on("change", function () {

            $.ajax({
                type: 'POST',
                url: '${createLink(controller: 'report', action: 'findPlanByProduct')}',
                data: {
                    item_id:$(this).val() !==''? $(this).val():0
                },
                success: function(data) {
                    var plan=$("#planSelect");
                    var planParent=plan.parent("div");
                    plan.remove();
                    planParent.append(data)
                    $('#planName').val($('#planSelect option:selected').text());
                }
            });
        });

        $(document).on('change', '#planSelect', function() {
            $('#planName').val($('#planSelect option:selected').text());
        });

        $('#item').change(function() {
            $('#itemName').val($('#item option:selected').text());
        });

        $('#childs').change(function() {
            var val = '';
            $('#childs option:selected').each( function() {
                if(val != '') val += ', ';
                val += $(this).text();
            });
            $('#entityNames').val(val);
        });

        $("label[for='childs']").text('<g:message code="report.billing.register.label.company"/>');
    });
</script>
