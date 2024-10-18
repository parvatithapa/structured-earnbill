%{--
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE: All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 --}%

<p>Rate Cards are a popular pricing model for determining the metered rate of a product based on information collected through mediation.</p>
<p>Rate Cards are tables that contain various prices and descriptions for different products. A common type of rate card is one that is used
by phone companies to indicate the prices for different locations for phone calls. For example, a call to Switzerland is going to be charged at a
different rate than a call to Dubai. Calls made to another state or province will have a different rate than local calls.</p>
<p>jBilling is able to handle these different rates by uploading a CSV file with the rate card information.
When the mediation process runs, events match to a product from the rate card, and the customer will be charged for that product at the indicated rate.</p>
<p>The rate card that you have uploaded into the system is now ready to translate the events coming in from mediation data.
In order for this data to be applied to products, you need to set a product up in jBilling using the rate card pricing model.</p>
<p>Lookup Field:</p>
<p>Refers to the mediation file column in your event record that you want the pricing
strategy to try to match against in the 'Match' column of your CSV rate card. For example, if you
set the Lookup Field to 'call_destination_code', every record in the event record is going to try
and match values in that column against every record in the rate card CSV file 'match' column,
until it finds one. </p>
<p>Match Type:</p>
<p>can be one of two options. EXACT means that when the mediation process runs
the system will look for events that match the product exactly (letter for letter).
BEST_MATCH means that the system will look for events that are the best match for the product.
It will continue to shorten the letters it searches for until it finds a match in the table.</p>
<br/>
<p>Once you save the Rate Card product, any events that apply to the rate card will be created in a
one­time purchase order with the rate provided on the rate card CSV file.</p>