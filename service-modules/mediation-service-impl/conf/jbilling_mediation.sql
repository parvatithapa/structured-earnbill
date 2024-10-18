CREATE TABLE jbilling_mediation_record
(
id int PRIMARY KEY,
status varchar(20),
jbilling_entity_id varchar(255),
mediation_cfg_id varchar(255),
record_key varchar(255),
user_id varchar(255),
event_date varchar(255),
quantity varchar(255),
description varchar(255),
currency_id varchar(255),
item_id varchar(255),
pricing_fields varchar(255),
process_id varchar(255)
);

CREATE TABLE jbilling_mediation_error_record
(
id int PRIMARY KEY,
jbilling_entity_id varchar(255),
mediation_cfg_id varchar(255),
record_key varchar(255),
error_codes varchar(255),
pricing_fields varchar(255),
process_id varchar(255),
status varchar(255)
);

CREATE TABLE jbilling_mediation_process
(
id int PRIMARY KEY,
startDate varchar(255),
end_date varchar(255),
records_processed varchar(255),
done_and_billable varchar(255),
errors varchar(255),
duplicates varchar(255),
);
