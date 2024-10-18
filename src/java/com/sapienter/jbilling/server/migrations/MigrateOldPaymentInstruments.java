package com.sapienter.jbilling.server.migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Khobab
 */
public class MigrateOldPaymentInstruments extends AbstractCustomSqlChange {

    private Logger logger = LogFactory.getLogger("MigrateOldPaymentInstruments");

    public String getConfirmationMessage() {
        return "Payment Instruments Migrated";
    }

    //Query for check if exist only one meta_field_name with this field_usage for entity_type PAYMENT_METHOD_TYPE and the current entity
    private StringBuffer queryTypeUsage = new StringBuffer("select count(name.id) from meta_field_name name")
            .append(" inner join payment_method_meta_fields_map pi on pi.meta_field_id = name.id")
            .append(" where name.entity_type='PAYMENT_METHOD_TYPE' and name.field_usage=? and name.entity_id = ? and pi.payment_method_id = (select max(id) from payment_method_type)");

    @Override
    public SqlStatement[] doGenerateStatements(Database database) throws CustomChangeException {
        List<SqlStatement> statements = new LinkedList<SqlStatement>();
        try {
            //check if credit_card table exist if the table
            //does not exist then a migration has already been done
            try {
                connection.prepareStatement(
                        "select count(*) from credit_card")
                        .executeQuery().close();
            } catch (SQLException ex){
                System.out.println("Payment instrument migration will not be performed");
                return new SqlStatement[0];
            }

            // migrate credit cards first
            migrateCreditCards(connection, statements);
            migrateACHs(connection, statements);
            migrateCheques(connection, statements);
            migrateBlacklistedCreditCards(connection, statements);


        } catch (SQLException e) {
            System.out.println("SQL Exception");
            throw new CustomChangeException("SQL Exception", e);
        }
        System.out.println("Number of statements: " + statements.size());

        //convert to array and return
        SqlStatement[] stmts = new SqlStatement[statements.size()];
        for (int i = 0; i < statements.size(); i++) {
            stmts[i] = statements.get(i);
        }
        return stmts;
    }

    private void migrateCreditCards(Connection connection, List<SqlStatement> statements) throws SQLException {
        // check if there are some credit cards to be migrated
        String query = "select count(*) from credit_card";
        PreparedStatement count = connection.prepareStatement(query);
        ResultSet result = count.executeQuery();
        boolean migrate = (result.next() ? 0 != result.getInt(1) : false);

        if(migrate) {
            // there are cards that needs to be migrated
            // migrate credit cards of each company's users
            ResultSet companies = getCompanies(connection);
            while (companies.next()) {
                Integer companyId = companies.getInt("id");
                System.out.println("Creating payment method type for company " + companyId);
                statements.addAll(generateCreditCardPaymentMethodTypeStatements(connection, companyId));
                // verify if the company has users that are using credit cards
                List<Integer> accountTypes = getAccountTypesByCreditCard(connection, companyId);
                if(accountTypes.size() > 0) {
                    System.out.println("Migrating Company ID: " + companyId);
                    Iterator<Integer> iterator = accountTypes.iterator();
                    while(iterator.hasNext()) {
                        // first of all make a connection of payment method with account type
                        Integer accountTypeId= iterator.next();
                        logger.debug("Account Type ID Use " + accountTypeId);
                        statements.add(buildInsertStatement("payment_method_account_type_map")
                                .addColumnValue("account_type_id", accountTypeId)
                                .addColumnValue("payment_method_id",  new StringBuffer("(select max(pmt.id) from payment_method_type pmt)")));
                    }
                }

                // migrate company credit cards depending upon account type id
                statements.addAll(migrateCCByCompany(connection, companyId));
            }
            companies.close();
        }
    }

    private void migrateACHs(Connection connection, List<SqlStatement> statements) throws SQLException {
        // check if there are some credit cards to be migrated
        String query = "select count(*) from ach";
        PreparedStatement count = connection.prepareStatement(query);
        ResultSet result = count.executeQuery();
        boolean migrate = (result.next() ? 0 != result.getInt(1) : false);

        if(migrate) {
            // there are cards that needs to be migrated
            // migrate credit cards of each company's users
            ResultSet companies = getCompanies(connection);
            while (companies.next()) {
                Integer companyId = companies.getInt("id");
                System.out.println("Creating ach payment method type for company " + companyId);
                // verify if the company has users that are using achs
                List<Integer> accountTypes = getAccountTypesByACH(connection, companyId);
                if(accountTypes.size() > 0) {
                    statements.addAll(generateACHPaymentMethodTypeStatements(connection, companyId));
                    System.out.println("Migrating Company ID: " + companyId);
                    Iterator<Integer> iterator = accountTypes.iterator();
                    while(iterator.hasNext()) {
                        // first of all make a connection of payment method with account type
                        Integer accountTypeId= iterator.next();
                        logger.debug("Account Type ID Use " + accountTypeId);
                        statements.add(buildInsertStatement("payment_method_account_type_map")
                                .addColumnValue("account_type_id", accountTypeId)
                                .addColumnValue("payment_method_id",  new StringBuffer("(select max(pmt.id) from payment_method_type pmt)")));
                    }
                    // migrate company credit cards depending upon account type id
                    statements.addAll(migrateACHByCompany(connection, companyId));
                }
            }
            companies.close();
        }
    }

    private void migrateCheques(Connection connection, List<SqlStatement> statements) throws SQLException {
        // check if there are some credit cards to be migrated
        String query = "select count(*) from payment_info_cheque";
        PreparedStatement count = connection.prepareStatement(query);
        ResultSet result = count.executeQuery();
        boolean migrate = (result.next() ? 0 != result.getInt(1) : false);

        if(migrate) {
            // there are cards that needs to be migrated
            // migrate credit cards of each company's users
            ResultSet companies = getCompanies(connection);
            while (companies.next()) {
                Integer companyId = companies.getInt("id");
                System.out.println("Creating cheque payment method type for company " + companyId);
                Integer total = countChequesOfCompany(connection, companyId);
                System.out.println("Total credit cards for company: " + companyId + " are: " + total);
                if( total > 0) {
                    statements.addAll(generateChequePaymentMethodTypeStatements(connection, companyId));

                    List<Integer> accountTypes = getAccountTypesOfChequesByCompany(connection, companyId);

                    // first of all make a connection of payment method with account type
                    for(Integer accountTypeId : accountTypes) {
                        logger.debug("Account Type ID Use " + accountTypeId);
                        statements.add(buildInsertStatement("payment_method_account_type_map")
                                .addColumnValue("account_type_id", accountTypeId)
                                .addColumnValue("payment_method_id",  new StringBuffer("(select max(pmt.id) from payment_method_type pmt)")));
                    }

                    // cheques has only link to payment and no users
                    statements.addAll(migrateChequesByCompany(connection, companyId));
                }
            }
            companies.close();
        }
    }

    private void migrateBlacklistedCreditCards(Connection connection, List<SqlStatement> statements) throws SQLException {
        // check if there are some credit cards to be migrated
        String query = "select count(*) from blacklist where credit_card_id is not null";
        PreparedStatement count = connection.prepareStatement(query);
        ResultSet result = count.executeQuery();
        boolean migrate = (result.next() ? 0 != result.getInt(1) : false);

        if(migrate) {

            ResultSet companyId = connection.prepareStatement("select min(id) as min from entity").executeQuery();
            companyId.next();
            Integer entityId = companyId.getInt(1);

            // we need to create a payment method type for blacklisted cards. We do not care for company id
            statements.addAll(generateCreditCardPaymentMethodTypeStatements(connection, entityId));

            query = "select id, credit_card_id from blacklist where credit_card_id is not null";
            PreparedStatement blacklisted = connection.prepareStatement(query);
            ResultSet cards = blacklisted.executeQuery();

            while (cards.next()) {
                Integer cardId = cards.getInt("credit_card_id");

                String getCard = "select * from credit_card where id = " + cardId;
                ResultSet creditCard = connection.prepareStatement(getCard).executeQuery();
                if (creditCard.next()) {
	                // migrate blacklisted credit cards depending upon account type id
	                statements.addAll(moveCreditCard(connection, entityId, creditCard));
	                creditCard.close();

	                // update blacklist column value
	                statements.add(buildUpdateStatement("blacklist")
	                        .addNewColumnValue("credit_card_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
	                        .setWhereClause("id = " + cards.getInt("id")));
                }
            }
            cards.close();
        }

    }

    private List<SqlStatement> migrateCCByCompany(Connection connection, Integer companyId)
            throws SQLException {
        List<SqlStatement> inserts = new LinkedList<SqlStatement>();

        // get credit cards
        ResultSet creditCards = getCreditCards(connection, companyId);

        while(creditCards.next()) {
            inserts.addAll(moveCreditCard(connection, companyId, creditCards));

        }

        // move credit cards that are only related to payments and no users
        creditCards = getPaymentCreditCards(connection, companyId);
        while(creditCards.next()) {
            inserts.addAll(moveCreditCard(connection, companyId, creditCards));

        }
        creditCards.close();
        return inserts;
    }

    private List<SqlStatement> moveCreditCard(Connection connection, Integer companyId, ResultSet creditCards) throws SQLException {
        DateTimeFormatter format = DateTimeFormat.forPattern("MM/yyyy");

        List<SqlStatement> statements = new ArrayList<SqlStatement>();

        Integer creditCardId = creditCards.getInt("id");

        // if this card is linked with the payment then create a record in payment_instrument_info
        // check if there is a payment record to be updated
        String query = "select count(*) from payment where credit_card_id = " + creditCardId;
        PreparedStatement count = connection.prepareStatement(query);
        ResultSet result = count.executeQuery();
        boolean create = (result.next() ? 0 != result.getInt(1) : false);
        result.close();
        
        String userIdQry = "select min(user_id) from user_credit_card_map where credit_card_id = " + creditCardId;
        PreparedStatement userIdStmt = connection.prepareStatement(userIdQry);
        ResultSet userIdResult = userIdStmt.executeQuery();
        Integer userId = userIdResult.next() ? userIdResult.getInt(1) : null;
        userIdResult.close();

        statements.add(buildInsertStatement("payment_information")
                .addColumnValue("id",  new StringBuffer("(coalesce((select max(pi.id)+1 from payment_information pi), 1))"))
                .addColumnValue("user_id", create ? null : userId)
                .addColumnValue("payment_method_id", new StringBuffer("(select max(pmt.id) from payment_method_type pmt)"))
                .addColumnValue("processing_order", create ? 1 : 
                	new StringBuffer("(coalesce((select max(pi.processing_order)+1 from payment_information pi " +
                			"where pi.user_id = " + userId + "), 1))"))
                .addColumnValue("deleted", creditCards.getInt("deleted"))
                .addColumnValue("optlock", new Integer(1)));

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "TITLE");
        count.setInt(2, companyId);
        result = count.executeQuery();
        Integer total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "TITLE", creditCards.getString("name")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "PAYMENT_CARD_NUMBER");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "PAYMENT_CARD_NUMBER", creditCards.getString("cc_number")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "DATE");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "DATE", format.print(creditCards.getDate("cc_expiry").getTime())));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "GATEWAY_KEY");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "GATEWAY_KEY", creditCards.getString("gateway_key")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "CC_TYPE");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "CC_TYPE", creditCards.getInt("cc_type")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        if(create) {
            query = "select * from payment where credit_card_id = " + creditCardId;
            PreparedStatement paymentQuery = connection.prepareStatement(query);
            ResultSet payment = paymentQuery.executeQuery();
            payment.next();

            Integer paymentId = payment.getInt("id");

            //If we've a payment with result_id null, we need to avoid the following insert/update
            if (payment.getInt("result_id") > 0) {
                statements.add(buildInsertStatement("payment_instrument_info")
                        .addColumnValue("id", new StringBuffer("(coalesce((select max(pi.id)+1 from payment_instrument_info pi), 1))"))
                        .addColumnValue("result_id", payment.getInt("result_id"))
                        .addColumnValue("instrument_id", new StringBuffer("(select max(t.id) from payment_information t)"))
                        .addColumnValue("payment_id", paymentId)
                        .addColumnValue("method_id", payment.getInt("method_id")));

                // remove link from payment
                statements.add(buildUpdateStatement("payment")
                        .addNewColumnValue("credit_card_id", null)
                        .setWhereClause("id = " + paymentId));
            }

            payment.close();
        }

        // delete moved card
        //We need to avoid the deleted of any table until we make sure that the migration is ok
        /*statements.add(buildDeleteStatement("user_credit_card_map")
                .setWhere("credit_card_id = " + creditCardId));

        statements.add(buildDeleteStatement("credit_card")
                .setWhere("id = " + creditCardId));*/

        return statements;
    }

    private List<SqlStatement> migrateACHByCompany(Connection connection, Integer companyId)
            throws SQLException {

        List<SqlStatement> inserts = new LinkedList<SqlStatement>();

        // get credit cards
        ResultSet ach = getACHsByCompany(connection, companyId);
        while(ach.next()) {
            migrateAch(connection, ach, companyId);
        }
        ach.close();

        // migrate payment related achs
        ach = getPaymentAchsByCompany(connection, companyId);
        while(ach.next()) {
            migrateAch(connection, ach, companyId);
        }
        ach.close();

        return inserts;
    }

    private List<SqlStatement> migrateAch(Connection connection, ResultSet ach, Integer companyId) throws SQLException {
        List<SqlStatement> statements = new ArrayList<SqlStatement>();

        Integer achId = ach.getInt("id");

        // if this ach is linked with the payment then we have create a record in payment_instrument_info
        // check if there is a payment record to be updated
        String query = "select count(*) from payment where ach_id = " + achId;
        PreparedStatement count = connection.prepareStatement(query);
        ResultSet result = count.executeQuery();
        boolean create = (result.next() ? 0 != result.getInt(1) : false);
        result.close();

        statements.add(buildInsertStatement("payment_information")
                .addColumnValue("id",  new StringBuffer("(coalesce((select max(pi.id)+1 from payment_information pi), 1))"))
        // if ach is linked to payment then do not link it with user
                .addColumnValue("user_id", create ? null : ach.getInt("user_id"))
                .addColumnValue("payment_method_id", new StringBuffer("(select max(pmt.id) from payment_method_type pmt)"))
        .addColumnValue("processing_order", 1)
        .addColumnValue("deleted", 0)
        .addColumnValue("optlock", 1));

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "TITLE");
        count.setInt(2, companyId);
        result = count.executeQuery();
        Integer total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "TITLE", ach.getString("account_name")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "BANK_ROUTING_NUMBER");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "BANK_ROUTING_NUMBER", ach.getString("aba_routing")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "BANK_ACCOUNT_NUMBER");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "BANK_ACCOUNT_NUMBER", ach.getString("bank_account")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "GATEWAY_KEY");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "GATEWAY_KEY", ach.getString("gateway_key")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "BANK_NAME");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "BANK_NAME", ach.getString("bank_name")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        count = connection.prepareStatement(queryTypeUsage.toString());
        count.setString(1, "BANK_ACCOUNT_TYPE");
        count.setString(1, "BANK_ACCOUNT_TYPE");
        count.setInt(2, companyId);
        result = count.executeQuery();
        total = result.next() ? result.getInt(1) : 0;
        result.close();

        if(total == 1) {
            statements.add(buildMFInsertStm(companyId, "BANK_ACCOUNT_TYPE", ach.getString("account_type")));

            statements.add(buildInsertStatement("payment_information_meta_fields_map")
                    .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                    .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
        }

        if(create) {
            query = "select * from payment where ach_id = " + achId;
            PreparedStatement paymentQuery = connection.prepareStatement(query);
            ResultSet payment = paymentQuery.executeQuery();
            payment.next();

            Integer paymentId = payment.getInt("id");

            //If we've a payment with result_id null, we need to avoid the following insert/update
            if(payment.getInt("result_id") > 0) {
                statements.add(buildInsertStatement("payment_instrument_info")
                        .addColumnValue("id", new StringBuffer("(coalesce((select max(pi.id)+1 from payment_instrument_info pi), 1))"))
                        .addColumnValue("result_id", payment.getInt("result_id"))
                        .addColumnValue("instrument_id", new StringBuffer("(select max(t.id) from payment_information t)"))
                        .addColumnValue("payment_id", paymentId)
                        .addColumnValue("method_id", payment.getInt("method_id")));

                // remove link from payment
                statements.add(buildUpdateStatement("payment")
                        .addNewColumnValue("ach_id", null)
                        .setWhereClause("id = " + paymentId));
            }

            payment.close();
        }

        //We need to avoid the deleted of any table until we make sure that the migration is ok
        /*statements.add(buildDeleteStatement("ach")
                .setWhere("id = " + ach.getInt("id")));*/

        return statements;
    }

    private List<SqlStatement> migrateChequesByCompany(Connection connection, Integer companyId)
            throws SQLException {

        List<SqlStatement> statements = new LinkedList<SqlStatement>();
        // get credit cards
        ResultSet cheque = getChequesOfCompany(connection, companyId);
        while(cheque.next()) {
            // currently cheques are not associated with any user, they are only associated with payments
            statements.add(buildInsertStatement("payment_information")
                    .addColumnValue("id", new StringBuffer("(coalesce((select max(pi.id)+1 from payment_information pi), 1))"))
                    .addColumnValue("user_id", null)
                    .addColumnValue("payment_method_id", new StringBuffer("(select max(pmt.id) from payment_method_type pmt)"))
                    .addColumnValue("processing_order", 1)
                    .addColumnValue("deleted", 0)
                    .addColumnValue("optlock", 1));

            PreparedStatement count = connection.prepareStatement(queryTypeUsage.toString());
            count.setString(1, "CHEQUE_NUMBER");
            count.setInt(2, companyId);
            ResultSet result = count.executeQuery();
            Integer total = result.next() ? result.getInt(1) : 0;
            result.close();

            if (total == 1) {
                statements.add(buildMFInsertStm(companyId, "CHEQUE_NUMBER", cheque.getString("cheque_number")));

                statements.add(buildInsertStatement("payment_information_meta_fields_map")
                        .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                        .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
            }

            count = connection.prepareStatement(queryTypeUsage.toString());
            count.setString(1, "DATE");
            count.setInt(2, companyId);
            result = count.executeQuery();
            total = result.next() ? result.getInt(1) : 0;
            result.close();

            if (total == 1) {
                statements.add(buildMFInsertStm(companyId, "DATE", cheque.getDate("cheque_date")));

                statements.add(buildInsertStatement("payment_information_meta_fields_map")
                        .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                        .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
            }

            count = connection.prepareStatement(queryTypeUsage.toString());
            count.setString(1, "BANK_NAME");
            count.setInt(2, companyId);
            result = count.executeQuery();
            total = result.next() ? result.getInt(1) : 0;
            result.close();

            if (total == 1) {
                statements.add(buildMFInsertStm(companyId, "BANK_NAME", cheque.getString("bank")));

                statements.add(buildInsertStatement("payment_information_meta_fields_map")
                        .addColumnValue("payment_information_id", new StringBuffer("(select max(pi.id) from payment_information pi)"))
                        .addColumnValue("meta_field_value_id", new StringBuffer("(select max(value.id) from meta_field_value value)")));
            }

            Integer paymentId = cheque.getInt("payment_id");

            String query = "select * from payment where id = " + paymentId;
            PreparedStatement paymentQuery = connection.prepareStatement(query);
            ResultSet payment = paymentQuery.executeQuery();
            payment.next();
            result.close();

            //If we've a payment with result_id null, we need to avoid the following insert/update
            if (payment.getInt("result_id") > 0) {
                statements.add(buildInsertStatement("payment_instrument_info")
                        .addColumnValue("id", new StringBuffer("(coalesce((select max(pi.id)+1 from payment_instrument_info pi), 1))"))
                        .addColumnValue("result_id", payment.getInt("result_id"))
                        .addColumnValue("instrument_id", new StringBuffer("(select max(t.id) from payment_information t)"))
                        .addColumnValue("payment_id", paymentId)
                        .addColumnValue("method_id", payment.getInt("method_id")));

                //We need to avoid the deleted of any table until we make sure that the migration is ok
                /*statements.add(buildDeleteStatement("payment_info_cheque")
                        .setWhere("id = " + cheque.getInt("id")));*/
            }
        }

        cheque.close();
        return statements;
    }

    private ResultSet getTemplateByName(Connection connection, String templateName) throws SQLException {
        String templateQuery = "select * from payment_method_template where template_name = ?";
        PreparedStatement metaFieldStatement = connection.prepareStatement(templateQuery);
        metaFieldStatement.setString(1, templateName);
        return metaFieldStatement.executeQuery();
    }

    private List<SqlStatement> generateCreditCardPaymentMethodTypeStatements(
            Connection connection, Integer companyId) throws SQLException {
        return generatPaymentMethodStatements(connection, getTemplateByName(connection, "Payment Card"), "PAYMENT_METHOD_TYPE" ,
                companyId, true);
    }

    private List<SqlStatement> generateACHPaymentMethodTypeStatements(
            Connection connection, Integer companyId) throws SQLException {
        return generatPaymentMethodStatements(connection, getTemplateByName(connection, "ACH"), "PAYMENT_METHOD_TYPE" ,
                companyId, true);
    }

    private List<SqlStatement> generateChequePaymentMethodTypeStatements(
            Connection connection, Integer companyId) throws SQLException {
        return generatPaymentMethodStatements(connection, getTemplateByName(connection, "Cheque"), "PAYMENT_METHOD_TYPE" ,
                companyId, false);
    }

    private List<SqlStatement> generatPaymentMethodStatements(Connection connection, ResultSet template, String entityType, Integer companyId,
                                                              boolean isRecurring) throws SQLException {

        List<SqlStatement> statements = new LinkedList<SqlStatement>();
        if(!template.next()) {
            throw new IllegalArgumentException("Payment method template must exist");
        }

        statements.add(buildInsertStatement("payment_method_type")
                .addColumnValue("id",  new StringBuffer("(coalesce((select max(pmt.id)+1 from payment_method_type pmt), 1))"))
                .addColumnValue("optlock", Integer.valueOf(1))
                .addColumnValue("entity_id", companyId)
                .addColumnValue("is_recurring", isRecurring)
                .addColumnValue("template_id", template.getInt("id"))
                .addColumnValue("method_name", template.getString("template_name")));

        statements.addAll(generateMetaFieldInserts(getMetaFieldsByTemplate(connection, template.getInt("id"), companyId), entityType, companyId, connection));

        return statements;
    }

    private List<SqlStatement> generateMetaFieldInserts(ResultSet templateMetaField, String entityType, Integer companyId, Connection connection) throws SQLException {

        List<SqlStatement> inserts = new LinkedList<SqlStatement>();

        while (templateMetaField.next()) {
            String dataType = templateMetaField.getString("data_type");
            String name = templateMetaField.getString("name");
            Integer validationRule = templateMetaField.getInt("validation_rule_id");

            inserts.add(buildInsertStatement("meta_field_name")
                    .addColumnValue("id", new StringBuffer("(coalesce((select max(mfn.id)+1 from meta_field_name mfn), 1))"))
                    .addColumnValue("name", name)
                    .addColumnValue("entity_type", entityType)
                    .addColumnValue("data_type", dataType)
                    .addColumnValue("is_disabled", templateMetaField.getBoolean("is_disabled"))
                    .addColumnValue("is_mandatory", templateMetaField.getBoolean("is_mandatory"))
                    .addColumnValue("display_order", templateMetaField.getInt("display_order"))
                    .addColumnValue("optlock", Integer.valueOf(1))
                    .addColumnValue("field_usage", templateMetaField.getString("field_usage"))
                    .addColumnValue("entity_id", companyId)
                    .addColumnValue("is_primary", templateMetaField.getBoolean("is_primary"))
                    .addColumnValue("validation_rule_id", validationRule != null && validationRule != 0 ? validationRule : null));

            inserts.add(buildInsertStatement("payment_method_meta_fields_map")
                    .addColumnValue("meta_field_id", new StringBuffer("(select max(mfn.id) from meta_field_name mfn)"))
                    .addColumnValue("payment_method_id",  new StringBuffer("(select max(pmt.id) from payment_method_type pmt)")));

            // if its an enumeration then enumeration needs to be inserted.
            if("ENUMERATION".equalsIgnoreCase(dataType)) {
                PreparedStatement enumerationStatement = connection.prepareStatement("select * from enumeration where entity_id = ? and name = ?");
                enumerationStatement.setInt(1, companyId);
                enumerationStatement.setString(2, name);

                ResultSet result = enumerationStatement.executeQuery();
                if(result.next()) {
                    inserts.add(buildInsertStatement("enumeration")
                            .addColumnValue("id", new StringBuffer("(select max(id)+1 from enumeration)"))
                            .addColumnValue("entity_id", companyId)
                            .addColumnValue("name", name)
                            .addColumnValue("optlock", new Integer(1)));

                    Integer enumerationId = result.getInt("id");
                    enumerationStatement = connection.prepareStatement("select * from enumeration_values where enumeration_id = ?");
                    enumerationStatement.setInt(1, enumerationId);
                    ResultSet result1 = enumerationStatement.executeQuery();
                    while(result1.next()) {
                        inserts.add(buildInsertStatement("enumeration_values")
                                .addColumnValue("id", new StringBuffer("(select max(id)+1 from enumeration_values)"))
                                .addColumnValue("enumeration_id", enumerationId)
                                .addColumnValue("value", result1.getString("value"))
                                .addColumnValue("optlock", new Integer(1)));
                    }
                }
            }
        }
        return inserts;
    }

    private InsertStatement buildMFInsertStm(Integer companyId, String usage, Object value){
        String type;
        if(value != null) {
            type = value instanceof Integer ? "integer" : "string";
        } else {
            type = "string";
        }

        //string buffer is used to trick LB not to quote the string
        return buildInsertStatement("meta_field_value")
                .addColumnValue("meta_field_name_id", getFieldIdQuery(companyId, usage))
                .addColumnValue("dtype", type)
                .addColumnValue(type + "_value", value)
                .addColumnValue("id", new StringBuffer("(select max(mv.id) + 1 from meta_field_value mv)"));
    }

    private StringBuffer getFieldIdQuery(Integer companyId, String usage){
        return new StringBuffer(
                "(select name.id from meta_field_name name" +
                " inner join payment_method_meta_fields_map pi on pi.meta_field_id = name.id" +
                " where name.entity_type='PAYMENT_METHOD_TYPE' and name.field_usage='" + usage + "' and name.entity_id = " + companyId + " and pi.payment_method_id = (select max(id) from payment_method_type))");
    }

    private ResultSet getCompanies (Connection connection) throws SQLException {
        String companiesQuery = "select id as \"id\" from entity order by id asc";
        PreparedStatement companiesStatement = connection.prepareStatement(companiesQuery);
        return companiesStatement.executeQuery();
    }

    private List<Integer> getAccountTypesByACH(Connection connection, Integer companyId) throws SQLException {
        String accountTypesQuery = "select c.account_type_id from ach a" +
                " inner join base_user bu on bu.id = a.user_id" +
                " inner join customer c on c.user_id = bu.id" +
                " where bu.entity_id = ?" +
                " group by c.account_type_id";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesQuery);
        accountTypesStatement.setInt(1, companyId);
        ResultSet accountTypesPointer = accountTypesStatement.executeQuery();

        List<Integer> accountTypes = new LinkedList<Integer>();
        while(accountTypesPointer.next()) {
            accountTypes.add(accountTypesPointer.getInt("id"));
        }
        return accountTypes;
    }

    private List<Integer> getAccountTypesByCreditCard(Connection connection, Integer companyId) throws SQLException {
        String accountTypesQuery = "select c.account_type_id as \"id\" from customer c " +
                " inner join base_user bu on bu.id = c.user_id " +
                " inner join user_credit_card_map map on map.user_id = bu.id " +
                " where bu.entity_id = ? " +
                " group by c.account_type_id";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesQuery);
        accountTypesStatement.setInt(1, companyId);
        ResultSet accountTypesPointer = accountTypesStatement.executeQuery();

        List<Integer> accountTypes = new LinkedList<Integer>();
        while(accountTypesPointer.next()) {
            accountTypes.add(accountTypesPointer.getInt("id"));
        }
        return accountTypes;
    }

    private Integer countChequesOfCompany(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select count(*) from payment_info_cheque c" +
                " inner join payment p on p.id = c.payment_id" +
                " inner join base_user bu on bu.id = p.user_id " +
                " where bu.entity_id = ?";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        ResultSet accountTypesPointer = accountTypesStatement.executeQuery();

        return accountTypesPointer.next() ? accountTypesPointer.getInt(1) : 0;
    }

    private List<Integer> getAccountTypesOfChequesByCompany(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select  cu.account_type_id from payment_info_cheque c" +
                " inner join payment p on p.id = c.payment_id" +
                " inner join base_user bu on bu.id = p.user_id" +
                " inner join customer cu on cu.user_id = bu.id" +
                " where bu.entity_id = ?" +
                " group by cu.account_type_id";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        ResultSet accountTypesPointer = accountTypesStatement.executeQuery();

        List<Integer> accountTypes = new LinkedList<Integer>();
        while(accountTypesPointer.next()) {
            accountTypes.add(accountTypesPointer.getInt(1));
        }

        return accountTypes;
    }

    private ResultSet getCreditCards(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select * from credit_card cc" +
                " inner join user_credit_card_map map on map.credit_card_id = cc.id" +
                " inner join base_user bu on bu.id = map.user_id" +
                " where bu.entity_id = ?";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        return accountTypesStatement.executeQuery();
    }

    private ResultSet getPaymentCreditCards(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select * from credit_card where id in (select credit_card_id from payment p " +
                "inner join base_user bu on p.user_id = bu.id where p.credit_card_id is not null and bu.entity_id = ?)";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        return accountTypesStatement.executeQuery();
    }

    private ResultSet getACHsByCompany(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select * from ach a" +
                " inner join base_user bu on bu.id = a.user_id" +
                " where bu.entity_id = ?";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        return accountTypesStatement.executeQuery();
    }

    private ResultSet getPaymentAchsByCompany(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select * from ach where id in (select ach_id from payment p " +
                "inner join base_user bu on p.user_id = bu.id where p.ach_id is not null and bu.entity_id = ?)";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        return accountTypesStatement.executeQuery();
    }

    private ResultSet getChequesOfCompany(Connection connection, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select * from payment_info_cheque c" +
                " inner join payment p on p.id = c.payment_id" +
                " inner join base_user bu on bu.id = p.user_id " +
                " where bu.entity_id = ?";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, companyId);
        return accountTypesStatement.executeQuery();
    }

    private ResultSet getMetaFieldsByTemplate(Connection connection, Integer templateId, Integer companyId) throws SQLException {
        String accountTypesCountQuery = "select * from meta_field_name name" +
                " inner join payment_method_template_meta_fields_map map on map.meta_field_id = name.id" +
                " where method_template_id = ? and name.entity_id = ?";
        PreparedStatement accountTypesStatement = connection.prepareStatement(accountTypesCountQuery);
        accountTypesStatement.setInt(1, templateId);
        accountTypesStatement.setInt(2, companyId);
        return accountTypesStatement.executeQuery();
    }
}
