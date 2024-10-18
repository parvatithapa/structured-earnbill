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


<p>In tiered pricing model the product is calculated at a different price when larger quantities are purchased.
However, tiered breaks down and groups the number of products purchased.</p>
<p>Rate changes are based on a selected group of quantities purchased, and is calculated in the following manner:</p>
<p>Tiered Pricing Example:</p>
<p>Product - cell phones</p>
<p>Tier 1: 0 to 10 = $10.00</p>
<p>Tier 2: 10 to 20 = $9.50</p>
<p>Tier 3: Greater than 20 = $8.00</p>
<p>If a customer were to purchase 13 cell phones:</p>
<p>Tier 1: (10 x $10.00) + Tier 2: (3 x $9.50) = $128.50</p>
<p>If a customer were to purchase 25 cell phones:</p>
<p>Tier 1: (10 x $10.00) + Tier 2: (10 x 9.50) + Tier 3 (5 x $8.00) = $152.50</p>
<p>Please note: a tier can be set to any price, and you can create as many tiers as needed to generate the different prices for the product.</p>