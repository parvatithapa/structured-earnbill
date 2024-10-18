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


<p>This pricing model adds a quantity of a product to an order based on the customer's existing purchase.
However, the selector doesn't just look at a single Product. It looks at the purchases across an entire Category.</p>
<p>Example:</p>
<p>Category ID 1: Smart Phones</p>
<p>Within this Category there are the following Products:</p>
<ul>
    <li>Product ID 100, Blackberry</li>
    <li>Product ID 101, IPhone</li>
    <li>Product ID 102, Android</li>
</ul>
<p>One of the requirements for this particular company is that when any Product(s) under category ID 1 is added to a customer's account,
they also need to receive a set number of Mailboxes.
When the quantity of smart phones on an account is changed (increased or decreased), the 'Mailbox' product should change accordingly.
This means that the category of smart phones is considered as a whole. It does not deal with each product separately.</p>
<p>Here are the mailbox products that need to be added, based on how many Smart Phone products a customer purchases.
(These Products need to be created under their own category):</p>
<ul>
    <li>Product ID 200: up to 10 Mailboxes - $25</li>
    <li>Product ID 201: up to 25 Mailboxes - $50</li>
    <li>Product ID 202: unlimited Mailboxes - $100</li>
</ul>
<p>Therefore, the Mailbox Item Selector Product would look like this:</p>
<ul>
    <li>Selection Category: 1 (category ID: Smart Phones)</li>
    <li> 1 = 200 (Product ID: up to 10 Mailboxes)</li>
    <li>10 = 201 (Product ID: up to 25 Mailboxes)</li>
    <li>25 = 202 (Product ID: up to unlimited Mailboxes)</li>
</ul>
<p>In this particular setup, the billing administrator has specified that if a customer has up to a quantity of 10 Smart Phone, he/she also needs the '10 Mailbox' item purchased.
If the customer has more than 10 Smart Phones, then they need to be moved up to '25 Mailboxes'. If they have more than 25, they will need 'Unlimited Mailboxes'.</p>
<p>Selection Category:</p>
<p>This field represents the Category you want to link the Item Selector Product to. You need to select the Category ID from the drop-down.
Based on the example, the Category ID for Smart Phones is: 1. Therefore, you would enter: 1 into the field.</p>
<p>1 and Attributes:</p>
<p>In the remaining fields you need to enter the ID of the product you want added, based on the number of Selection Category Products purchased.</p>
<p>The fields on the left hand side represent the hierarchy of quantities that pertain to the quantity of Smart Phones the customer purchases.</p>
<p>Please note: it is possible to add as many attributes as needed to create the structure for your product.</p>