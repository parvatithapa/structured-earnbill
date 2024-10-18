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


<p>A pooled pricing model is based of how much you buy of another item. It is based on free units applied to another product that has been purchased.
The calculation of how many units are included is based off of the quantity of another item.</p>
<p>For example: if a customer buys 100 text messages for $5, they will receive another product: 50 minutes of calls for free.</p>
<p>When this model is selected, a few fields will appear. You will need to configure them in order to set up the product correctly.</p>
<p>Pooled Item ID:</p>
<p>Enter the ID number of the product you wish to pool in this field. For example, if you were creating a product that pooled a 100MB mail box,
and the ID number for that mailbox product is: 2100, you would enter that into the Pooled Item ID field.
As each purchase of the product would give you 100MB, you would want to enter this number in the multiplier field so that the system knows to assign 1 mailbox to each purchase of the pooled product.</p>
<p>This pricing model is different from graduated because the calculation is based on something else that a customer has bought instead of specifying how much of the product the customer is buying.</p>