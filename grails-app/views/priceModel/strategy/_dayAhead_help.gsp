<p>Product’s rates will change on daily basis. So, we need easy way to change rates. </p>
<P>Except this rate, there is one more factor “Adder fee” which should also be defined at pricing model but its value can be different for each customer. So total price will be sum of these two rates.</p>
<p>Since rates are changing every day, we need to send rates to LDC every day for customers subscribed to these special plans.
Due to same, we can’t process meter read and invoice read.</p>
<p>Because we do not have valid rates to apply on consumption, we could end up having inconsistent rates and so amount.
Invoices created for those orders will not match with shadow invoices sent by LDC.
So, it is better to not create order and invoices for those customers.</p>