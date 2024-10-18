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

<div class="content form-columns" id="commission_exceptions">
    <table id="commission_exceptions_table" class="dataTable" cellspacing="0" cellpadding="0" style="width: 50%">
        <thead>
        <tr>
            <th style="width: 165px"><g:message code="product.id"/></th>
            <th style="width: 165px"><g:message code="start_date"/></th>
            <th style="width: 165px"><g:message code="end_date"/></th>
            <th style="width: 165px"><g:message code="partner.commission.exception.percentage.rate"/></th>
            <th>
                <a id="commission_exceptions_add_button" class="addButton plus-icon" href="#"
                   onclick="addCommissionException(this);
                   return false;"
                   style="${partner?.commissionExceptions?.size() > 0 ? 'display: none;' : ''}">
                    &#xe026;
                </a>
            </th>
        </tr>
        </thead>
        <tbody>
        <g:if test="${partner?.commissionExceptions}">
            <g:each in="${partner?.commissionExceptions}" var="commissionException" status="idx">
                <g:render template="commissionException" model="[commissionException: commissionException]"/>
            </g:each>
        </g:if>
        </tbody>
    </table>

    <div class="optionsDivInvisible">
        <table>
            <tbody>
            <g:render template="commissionException" model="[hidden: true]"/>
            </tbody>
        </table>
    </div>

    <script type="text/javascript">

        $(function () {
            // If the Partner has Commission Exceptions defined we open the container so they are visible.
            if (${partner?.commissionExceptions?.size() > 0}) {
                toggleSlide($('#commission-exception.box-cards'));
            }

            // Create jQuery UI datepickers in "exception.endDate" inputs.
            var commissionsExceptionsTable = $("#commission_exceptions_table");
            var startDateInputs = commissionsExceptionsTable.find('input[name="exception.startDate"]');
            var endDateInputs = commissionsExceptionsTable.find('input[name="exception.endDate"]');
            var options = getCommissionExceptionDatePickerOptions();
            startDateInputs.removeAttr("id").datepicker(options);
            endDateInputs.removeAttr("id").datepicker(options);
            addCommissionExceptionButtons();
        });

        function getCommissionExceptionDatePickerOptions() {
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

        function addCommissionException(button) {
            var $tr = $("#commission_exceptions_template");
            var $clone = $tr.clone();
            $clone.find(':text').prop('disabled', false);
            $clone.removeAttr('id');

            $('#commission_exceptions').find('.dataTable').find('tbody').append($clone);

            var options = getCommissionExceptionDatePickerOptions();

            $clone.find('input[name="exception.referralId"]').removeAttr('id').val('');
            $clone.find('input[name="exception.startDate"]').removeAttr("id").datepicker(options);
            $clone.find('input[name="exception.endDate"]').removeAttr("id").datepicker(options);
            $clone.find('input[name="exception.percentage"]').removeAttr('id').val('');

            addCommissionExceptionButtons();
        }

        function removeCommissionException(button) {
            $(button).closest('tr').remove();
            addCommissionExceptionButtons()
        }

        function addCommissionExceptionButtons() {
            var $commissionExceptionsTable = $('#commission_exceptions_table');
            var $addButtons = $commissionExceptionsTable.find('.addButton');
            if ($addButtons.length == 0) {
                $('#commission_exceptions_add_button').show();
            } else {
                $addButtons.hide();
                $commissionExceptionsTable.find('tr').last().find('.addButton').show();
            }
        }
    </script>
</div>
