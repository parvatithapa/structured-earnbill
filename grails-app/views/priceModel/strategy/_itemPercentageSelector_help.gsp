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


<p>The Item Percentage Selector is very similar to the Item Selector in that the pricing model will add a quantity of a product based on the customer's existing purchase.
However, the difference is, this pricing model uses a percentage instead of a quantity.</p>
<p>There are several fields that you will see on this pricing model. The following are definitions for each:</p>
<p>Selection Category and Percent of Category:</p>
<p>Each of these fields needs to have a Category ID entered into it.
The reason for this is because the products that belong to these categories are what the system will be searching for when the Item Percentage Selector product is added to a Purchase Order.
Based on a calculation, the system will figure out the percentage value from these two category's products, then include the appropriate '0 and Attributes' product.</p>
<p>The calculation is as follows:</p>
<p>Quantity of Selection Category Products DIVIDED BY Quantity of Percentage of Category Products MULTIPLIED BY 100 = Percentage that the system will use to define the ranges found in the '0 and Attributes' set up.</p>
<p>0 and Attributes:</p>
<p>This section of the pricing model functions the same way as Item Selector.
The only difference is, instead of representing a quantity, the column on the left represents a percentage.
The column on the right still represents the product that should be added based on the determined ranges.</p>
<p>As stated above, the system will search the customer's purchase orders for products that belong to the categories entered in the Selection Category and Percent of Category fields.
When adding the Item Percentage Selector product to a purchase order,
the system will perform a calculation, the result of which is the percent amount that will then be used to determine the range, or tier.
When the system finds the correct range, it will automatically apply the product associated to it.</p>
