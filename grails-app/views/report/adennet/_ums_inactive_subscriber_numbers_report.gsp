<%@ page import="org.joda.time.DateTime;" %>
<%--
   Incctive Subscriber Numbers(Last 6 Months)
  @author Nitin Yewale
  @since  18-AUG-2022
--%>

<div class="form-columns">
    <g:applyLayout name="form/select">
              <content tag="label"><g:message code="duration"/></content>
               <content tag="label.for">inactive_since</content>
               <content tag="include.script">true</content>
                  <g:select onChange="setLable();"
                           id="inactive_since"
                           name="inactive_since"
                           from="${['3 Months', '9 Months', '12 Months']}"
                           keys="${['90 day', '270 day', '360 day']}"
                           noSelection="${['180 day':'6 Months']}"
                  />
     <g:hiddenField name="inactive_month" />
    </g:applyLayout>

  <script type="text/javascript">
        function setLable(){
            $('#inactive_month').val($("#inactive_since option:selected").text())
        }
    </script>

</div>