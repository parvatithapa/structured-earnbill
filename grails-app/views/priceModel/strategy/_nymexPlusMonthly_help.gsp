<p>This pricing model calculates rates based on route rate card. If start and end date of order belongs to more than one rate in route rate card then it will get the blended rates based on average usage per day</p>
<P>Except this, there is one more factor “Adder fee” which should also be defined at pricing model but its value can be different for each customer. So total price will be sum of these two rates.</p>
<pre style='font-family: "HelveticaNeue-Light", "Helvetica Neue Light", "Helvetica Neue", Helvetica, Arial, "Lucida Grande", sans-serif;'>
This model will calculate usage ratio for different spans divided by Break Point.
   For example, From : 27 Jan, To : 10 Feb. Total usage : 1000.
   And route rate card has rows :
   1 Jan - 1 Feb : 0.01
   1 Feb - 1 Mar : 0.02 NOTE : End date (right date is exclusive)
Here 1 Feb is break point.

Usage will be divided as:

Usage for 27 Jan to 31 Jan
   (27 Jan - 31 Jan) * 1000 / (27 Jan - 10 Feb)
   => 5 * 1000 / 15
   => 333.334

Usage for 1 Feb to 10 Feb
   (1 Feb - 10 Feb) * 1000 / (27 Jan - 10 Feb)
   => 10 * 1000 / 15
   => 666.667

Rate will be calculated as:
For example, From : 27 Jan, To : 5 Feb.
Firstly it will pass 27 Jan as input and get January month rate. 
In that row, end date is 31 Jan. If that date is smaller or equal than TO date
which is 5 Feb, it will search rate for 1 Feb then. It will return rate 0.02

Total price: (333.33 * 0.01 + Adder_fee) + (666.66 * 0.02 + Adder_fee)
</pre>