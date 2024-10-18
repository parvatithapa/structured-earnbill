<div id="usagePool">
	 <g:hiddenField name="actionIndex" value="${actionIndex}" />
	<g:render template="/usagePool/attributes" model="[usagePool: usagePool, consuptionActions: consuptionActions]"/>
</div>
<script type="text/javascript">
        /**
         * usage pool attribute add & remove functions.
         */
	function addModelAttribute(element, actionIndex) {
            $('#actionIndex').val(actionIndex);

            $.ajax({
                     type: 'POST',
                     url: '${createLink(action: 'addAction')}',
                     data: $('#usagePool').parents('form').serialize(),
                     success: function(data) {
                         $('#usagePool').replaceWith(data);
                         setVisibilityOfActionsInfo();
                     }
                 });
        }

        function removeModelAttribute(element, actionIndex) {
            $('#actionIndex').val(actionIndex);

            $.ajax({
                     type: 'POST',
                     url: '${createLink(action: 'removeAction')}',
                     data: $('#usagePool').parents('form').serialize(),
                     success: function(data) {
                         $('#usagePool').replaceWith(data);
                         setVisibilityOfActionsInfo();
                     }
                 });
        }
 </script>
