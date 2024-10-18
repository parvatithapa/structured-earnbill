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

<div class="content form-columns" id="referral_commissions">
    <table id="referral_commissions_table" class="dataTable" cellspacing="0" cellpadding="0" style="width: 50%">
        <thead>
        <tr>
            <th style="width: 165px"><g:message code="bean.PartnerWS.id"/></th>
            <th style="width: 165px"><g:message code="start_date"/></th>
            <th style="width: 165px"><g:message code="end_date"/></th>
            <th style="width: 165px"><g:message code="partner.commission.exception.percentage.rate"/></th>
            <th>
                <a id="referrer_commission_add_button" class="addReferralButton plus-icon" href="#"
                   onclick="addReferralCommission(this);
                   return false;"
                   style="${partner?.referrerCommissions?.size() > 0 ? 'display: none;' : ''}">
                    &#xe026;
                </a>
            </th>
        </tr>
        </thead>
        <tbody>
        <g:if test="${partner?.referrerCommissions}">
            <g:each in="${partner?.referrerCommissions}" var="referrerCommission" status="idx">
                <g:render template="referralCommission" model="[referrerCommission: referrerCommission]"/>
            </g:each>
        </g:if>
        </tbody>
    </table>

    <div class="optionsDivInvisible">
        <table>
            <tbody>
            <g:render template="referralCommission" model="[hidden: true]"/>
            </tbody>
        </table>
    </div>

    <script type="text/javascript">

        $(function () {
            // If the Partner has Referral Commissions defined we open the container so they are visible.
            if (${partner?.referrerCommissions?.size() > 0}) {
                toggleSlide($('#referral-commissions.box-cards'));
            }

            // Create jQuery UI datepickers in "referral.endDate" inputs.
            var commissionsExceptionsTable = $("#referral_commissions_table");
            var startDateInputs = commissionsExceptionsTable.find('input[name="referrer.startDate"]');
            var endDateInputs = commissionsExceptionsTable.find('input[name="referrer.endDate"]');
            var options = getReferralCommissionsDatePickerOptions();
            startDateInputs.removeAttr("id").datepicker(options);
            endDateInputs.removeAttr("id").datepicker(options);
            addReferralCommissionsButtons();
        });

        function getReferralCommissionsDatePickerOptions() {
            var options = $.datepicker.regional['${session.locale.language}'];
            if (options == null) {
                options = $.datepicker.regional[''];
            }
            options.dateFormat = "${message(code: 'datepicker.jquery.ui.format')}";
            options.showOn = "both";
            options.buttonImage = "${resource(dir:'images', file:'icon04.gif')}";
            options.buttonImageOnly = true;
            return options;
        }

        function addReferralCommission(button) {
            var $tr = $("#referrer_commissions_template");
            var $clone = $tr.clone();
            $clone.removeAttr('id');

            $('#referral_commissions').find('.dataTable').find('tbody').append($clone);

            var options = getReferralCommissionsDatePickerOptions();

            $clone.find('input[name="referrer.referralId"]').removeAttr('id').prop('disabled', false).val('');
            $clone.find('input[name="referrer.startDate"]').removeAttr("id").prop('disabled', false).datepicker(options);
            $clone.find('input[name="referrer.endDate"]').removeAttr("id").prop('disabled', false).datepicker(options);
            $clone.find('input[name="referrer.percentage"]').removeAttr('id').prop('disabled', false).val('');

            addReferralCommissionsButtons();
        }

        function removeReferralCommission(button) {
            $(button).closest('tr').remove();
            addReferralCommissionsButtons();
        }

        function addReferralCommissionsButtons() {
            var $referralCommissionsTable = $('#referral_commissions').find('.dataTable');
            var $addButtons = $referralCommissionsTable.find('.addReferralButton');
            $addButtons.hide();
            if ($addButtons.length == 0) {
                $('#referrer_commission_add_button').show();
            } else {
                $addButtons.hide();
                $referralCommissionsTable.find('tr').last().find('.addReferralButton').show();
            }
        }
    </script>
</div>
