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
  java script to use with select when it is included in ajax call.

  Usage:

    <g:applyLayout name="include/select_small_script"/>

--%>

<script type="text/javascript" xmlns="http://www.w3.org/1999/html">
    $(document).ready(function() {
        $("${raw(selector)}").each(function () {
            updateSelectLabel(this);
        });

        $("${raw(selector)}").change(function () {
            updateSelectLabel(this);
        });
    });
</script>
