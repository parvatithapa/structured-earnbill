<p>A new pricing model which work on route rate card(same as NYMEXPlusMonthlyPricingStrategy pricing model).</p>
<p>But rate will be calculated as percentage instead of per unit. Blending the rates<p>
<pre style='font-family: "HelveticaNeue-Light", "Helvetica Neue Light", "Helvetica Neue", Helvetica, Arial, "Lucida Grande", sans-serif;'>
This pricing model will calculate average percentage for different spans 
divided by Break Point.

This calculation based on existing line.
If linePrice = 2.0581904
then lineAmount = (linePrice * Total usage) = (2.0581904 * 1848) = 3803.5358592

For example, From : 20 May, To : 10 June. Total usage : 1848.

No. of days = 21
usage per day = (Total usage/No. of days) = (1848/21) = 88

And route rate card has rows :
1 May - 1 June : 5%
1 June - 1 July : 6% : End date(right date is exclusive)
Here 1 June is break point.

First Part of usage = usage for 20 May to 1 June 
= (usage per day * days from 20 May to 1 June) = 88 * 11 = 968

Second Part of usage = usage for 1 June to 10 June 
= (usage per day * days from 1 June to 10 June) = 88 * 10 = 880

Percentage will be calculated as :
=> [{((First Part of usage * linePrice * (1 May - 1 June))/100) 
     + ((Second Part of usage * linePrice * (1 June - 1 July)) / 100)} * 100]
     / lineAmount

=> [{((968 * 2.0581904 * 5)/100) + ((880 * 2.0581904 * 6) / 100)} * 100] 
     / 3803.5358592

=> 5.47619%
</pre>