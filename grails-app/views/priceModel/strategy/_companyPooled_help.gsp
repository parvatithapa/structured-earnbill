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

<p>There are pricing situations where a particular quantity of a product is shared over several
accounts using jBilling’s sub­account relationship.</p>
<p>In order to configure the Company Pooled pricing model, the following fields must be configured:</p>
<p>Rate:</p>
<p>The value entered in this field indicates how much each quantity of the product will cost once the
pooled quantity is consumed.</p>
<p>For example, if the pooled quantity is 1000, the rate is $1 and only 800 pooled units are
consumed, the charge is: $0. However, if all 1000 pooled units are used and the customer
consumes a quantity of 300, the charge is: $300.</p>
<p>Pool Category ID:</p>
<p>This field must contain an existing category ID value. Once entered the system will know that all
products belonging to that category will share the same pool. For example, if you have Product
A and Product B, and they both belong to the same ‘Pool Category’, buying either one will
decrease the pool amount.</p>
<p>Included Quantity:</p>
<p>When configuring products to use this pricing model, this field is always zero (0). When you
create the plan that includes a free quantity (minutes for example), this is when you enter a
value. This field captures the pool size for the plan. For example, you create a plan called,
Premium Plan ­ 1000 minutes free. The Included Quantity value in this case is: 1000.</p>
<br/>
<p>CHAIN PRICES WITH COMPANY POOLED</p>
<p>This pricing model is sometimes used in tandem with other pricing models. (for example, Rate
Card + Company Pooled).</p>
<p>Note: There are a few limitations attached to this pricing model. </p>
<p>First, an order can only have a single line/product with a price type of Company Pooled.</p>
<p>Second, never edit the company pooled price on an order if there is a more recent order that
has a product that belongs to the same period and pool. To delete an old order line with this
pricing model, create a new order using the same product but negative quantity.</p>