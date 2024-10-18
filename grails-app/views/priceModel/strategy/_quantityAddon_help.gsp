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

<p>This pricing model allows you to increase the number of “Included Quantity” of a typical
Graduated Product based on the purchases of other Products. By using the Quantity Addon
model, you have the ability to build a larger chain, or bundle of Products, one that includes
additional products which will adjust the how much gets included for free of the Quantity Addon
Product in a Purchase Order.</p>
<p>Quantity Addon requires a bit more configuration than the other pricing models.
Usually used chained with Graduated pricing model.</p>
<p>In the Attributes fields, the text box on the right will contain the ID of the product you want to 'Addon' to the Graduated pricing model.
The text box to the left will contain the Included Quantity.</p>
<p>Example: </p>
<p>We have a metered product called Additional Minutes with a rate of $1.
This product has an ID of 100.</p>
<p>We create a Product with Quantity Addon pricing strategy called Local Minutes and we chain it with a graduated pricing strategy</p>
<p>The graduated strategy is configured to include 10 items for free and after that each item will be rated for $1.</p>
<p>For the attributes fields of the Quantity Addon strategy, in the Product ID attribute we enter the ID of Additional Minutes product 100,
and we include quantity of let's say 5 items.</p>
<p>This configuration tells us that if we create order with the Local Minutes product(Quantity Addon) only,
the product will be treated as Graduated product.</p>
<p>That means, if a customer buys, say 15 Local Minutes, then he/she is charged for 5 minutes only, that will be $5 total in this case.</p>
<p>If the customer add the Additional Minutes product in the same order, he/she will be charged for this product, because it is metered, $1 in this case,
but will get 5 Local Minutes extra for free.</p>
<p>So in this case the customer will be charged for the Additional Minutes only, the Local Minutes will be for free,
because the included quantity of Local Minutes in the Additional Minutes is 5.</p>
<p>The total amount of this order will be $1.</p>
<p>Note: In this example there is only one Addon product. By clicking on the green plus sign '+',
it is possible to Addon as many products as required.</p>