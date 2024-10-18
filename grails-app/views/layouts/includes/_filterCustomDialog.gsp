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


<div id="filter-custom-dialog" title="New Custom Filter Set">
    <g:formRemote name="filter-custom-form" url="[controller: 'filter', action: 'saveCustomizeFilter']"
                  update="filtersetss">
        <div id="filtersetss" class="columns-holder">

            <!-- content rendered using ajax -->

        </div>
    </g:formRemote>
</div>

<script type="text/javascript">
    $(function () {
        $('#filter-custom-dialog').dialog({
            autoOpen: false,
            height: 455,
            width: 550,
            modal: true,
            buttons: {
                Save: function () {
                    $('#filter-custom-form').submit();
                },
                Close: function () {
                    $(this).dialog("close");
                }
            },
            open: function () {
                $('#filtersetss').load("${createLink(controller: 'filter', action: 'editCustomFilter')}");
            },
            close: function () {
                $('#filters').load("${createLink(controller: 'filter', action: 'filters')}");
            }
        });
    });
</script>