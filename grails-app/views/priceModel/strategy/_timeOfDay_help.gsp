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


<p>This model allows you to set pricing based upon the time of day it runs on.</p>
<p>For example: a cell phone company would like to give their customers: an evening free, on all local phone calls. Free calling will start at 6:00 PM, every weekday evening.</p>
<p>When this model is selected, several fields will appear that you will need to configure in order to set up your product correctly:</p>
<ol>
    <li>
        Date Field
        <p>The Date Field represents the date from incoming data, when using mediation.
        If you are using mediation to tie the purchase of a product to an event, you would need to enter a field name.
        If nothing is specified in this field, the system will use the time when it was created.</p>
    </li>
    <li>
        00:00
        <p>This represents midnight (the beginning of a day) and it uses the current system time.
        This field specifies the price after a specific time.</p>
        <p>For example: Entering $10.00 into the 00:00 field indicates that after midnight, the price of the product will be $10:00.</p>
    </li>
    <li>
        Attributes
        <p>In order to indicate a different price starting at a different time, you need to use the attributes available below the 00:00 field.</p>
        <p>For example:
        Entering 12:00 and $8:00 will indicate to the system that after 12:00, the product will be at a price of $8.00.
        Entering 7:00 and $10.00, will let the system know that at 7:00, the price of the product needs to return to its original price of $10.00.
        It is possible to build as many attributes as you'd like, indicating a different price for any hour(s) of the day.</p>
    </li>
</ol>