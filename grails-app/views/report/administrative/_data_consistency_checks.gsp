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

<div class="form-columns" >
                    <g:applyLayout name="form/checkbox">
                       <g:checkBox class="cb checkbox" name="is_parent_inculde" value="true"/>
                        <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10"><g:message code="is.parent.inculde"/></label>
                        <content tag="label.for">is_parent_inculde</content>
                    </g:applyLayout>
                    
                    <g:applyLayout name="form/checkbox">
                    	 <g:checkBox class="cb checkbox" name="include_negative_invoices_reports" value="true"/>
                          <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10"><g:message code="negative.invoices"/></label>
                        <content tag="label.for">include_negative_invoices_reports</content>
                    </g:applyLayout>
                    
                    <g:applyLayout name="form/checkbox">
                    	<g:checkBox class="cb checkbox" name="include_one_time_orders_with_active_since_date_as_1st_Of_the_month_and_status_as_finished" value="true"/>
                        <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
                            <g:message code="one.time.orders.with.active.since.date.as.1st.Of.the.month.and.status.as.finished"/>
                        </label>
                        <content tag="label.for">include_one_time_orders_with_active_since_date_as_1st_Of_the_month_and_status_as_finished</content>
                    </g:applyLayout>

                    <g:applyLayout name="form/checkbox">
                    	<g:checkBox class="cb checkbox" name="include_mediated_orders_with_active_since_date_in_future" value="true" />
                         <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10"><g:message code="mediated.orders.with.active.since.date.in.future"/></label>
                        <content tag="label.for">include_mediated_orders_with_active_since_date_in_future</content>
                    </g:applyLayout>

                     <g:applyLayout name="form/checkbox">
                       <g:checkBox class="cb checkbox" name="include_mediated_orders_with_active_since_date_before_1st.of_current_month_and_status_not_finished" value="true"/>
                       <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
                       		<g:message code="mediated.orders.with.active.since.date.before.1st.of.current.month.and.status.not.finished"/>
                       </label>
                        <content tag="label.for">include_mediated_orders_with_active_since_date_before_1st.of_current_month_and_status_not_finished</content>
                    </g:applyLayout>

                    <g:applyLayout name="form/checkbox">
                    	 <g:checkBox class="cb checkbox" name="include_customer_usage_pools_with_cycle_start_date_or_cycle_end_date_in_past_month" value="true"/>
                        <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
                        	<g:message code="incude.customer.usage.pools.with.cycle.start.date.or.cycle.end.date.in.past.month"/>
                        </label>
                        <content tag="label.for">include_customer_usage_pools_with_cycle_start_date_or_cycle_end_date_in_past_month</content>
                    </g:applyLayout>

                    <g:applyLayout name="form/checkbox">
                    	<g:checkBox class="cb checkbox" name="customer_usage_pools_with_cycle_start_date_and_cycle_end_date_in_future_month" value="true"/>
                        <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
                        	<g:message code="customer.usage.pools.with.cycle.start.date.and.cycle.end.date.in.future.month"/>
                        </label>
                        <content tag="label.for">customer_usage_pools_with_cycle_start_date_and_cycle_end_date_in_future_month</content>
                    </g:applyLayout>

                    <g:applyLayout name="form/checkbox">
							<g:checkBox class="cb checkbox" name="include_next_billable_date_mismatch_between_orders_and_order_changes" value="true"/>
							<label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
							<g:message code="next.billable.date.mismatch.between.orders.and.order.changes"/>
                        </label>
                        <content tag="label.for">include_next_billable_date_mismatch_between_orders_and_order_changes</content>
                    </g:applyLayout>

					<g:applyLayout name="form/checkbox">
						<g:checkBox class="cb checkbox" name="include_plan_customer_usage_pool_mismatch" value="true"/>
                        <content tag="label"><g:message code="include.plan.customer.usage.pool.mismatch"/></content>
                        <content tag="label.for">include_plan_customer_usage_pool_mismatch</content>
                    </g:applyLayout>

                     <g:applyLayout name="form/checkbox">
						<g:checkBox class="cb checkbox" name="include_payments_with_pre_auth" value="true"/>
                          <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10"><g:message code="include.payments.with.pre.auth"/></label>
                        <content tag="label.for">include_payments_with_pre_auth</content>
                    </g:applyLayout>

 					<g:applyLayout name="form/checkbox">
						<g:checkBox class="cb checkbox" name="mediated_orders_with_negative_amounts" value="true"/>
                          <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10"><g:message code="mediated.orders.with.negative.amounts"/></label>
                        <content tag="label.for">mediated_orders_with_negative_amounts</content>
                    </g:applyLayout>
                    
                     <g:applyLayout name="form/checkbox">
                    	 <g:checkBox class="cb checkbox" name="include_mediated_orders_without_usage_pool" />
                          <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10"><g:message code="include.mediated.orders.without.usage.pool"/></label>
                        <content tag="label.for">include_mediated_orders_without_usage_pool</content>
                    </g:applyLayout>
                    
                     <g:applyLayout name="form/checkbox">
                    	<g:checkBox class="cb checkbox" name="subscription_orders_that_dont_have_associated_customer_usage_pool" />
                        <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
                        	<g:message code="subscription.orders.that.dont.have.associated.customer.usage.pool"/>
                        </label>
                        <content tag="label.for">subscription_orders_that_dont_have_associated_customer_usage_pool</content>
                    </g:applyLayout>
                    <g:applyLayout name="form/checkbox">
                        <g:checkBox class="cb checkbox" name="asset_conflict_report" />
                        <label tag="label" style="text-align: left; width:300px; margin-right: 10">
                            <g:message code="assets.conflict.report"/>
                        </label>
                        <content tag="label.for">asset_conflict_report</content>
                    </g:applyLayout>
                    <g:applyLayout name="form/checkbox">
                    	<g:checkBox class="cb checkbox" name="order_active_until_less_than_orderNBD_and_not_finished"/>
                        <label class = "labelWrap" style="text-align: left; width:300px; margin-right: 10">
                        	<g:message code="order.active.until.less.than.order.nbd.and.not.finished"/>
                        </label>
                        <content tag="label.for">order_active_until_less_than_orderNBD_and_not_finished</content>
                    </g:applyLayout>
                    
                    <g:applyLayout name="form/checkbox">
                       <g:checkBox class="cb checkbox" name="customer_NID_less_than_today" />
                        <label tag="label" style="text-align: left; width:300px; margin-right: 10">
                          <g:message code="customer.nid.less.than.today"/>
                        </label>
                        <content tag="label.for">customer_NID_less_than_today</content>
                    </g:applyLayout>
                    <g:applyLayout name="form/checkbox">
                       <g:checkBox class="cb checkbox" name="multiple_plans_on_same_order" />
                        <label tag="label" style="text-align: left; width:300px; margin-right: 10">
                          <g:message code="multiple.plans.on.same.order"/>
                        </label>
                        <content tag="label.for">multiple_plans_on_same_order</content>
                    </g:applyLayout>
                    <g:applyLayout name="form/checkbox">
                       <g:checkBox class="cb checkbox" name="usage_order_quantity_mismatches_customer_usage_pool_quantity" />
                        <label tag="label" style="text-align: left; width:300px; margin-right: 10">
                          <g:message code="usage.order.quantity.mismatches.customer.usage.pool.quantity"/>
                        </label>
                        <content tag="label.for">usage_order_quantity_mismatches_customer_usage_pool_quantity</content>
                    </g:applyLayout>
</div>
