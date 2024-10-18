/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.ratecards;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.pricing.RateCardBL;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL;
import com.sapienter.jbilling.server.pricing.RouteRecord;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.cache.RouteFinder;
import com.sapienter.jbilling.server.pricing.db.RateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RateCardDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.user.RouteWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(groups = { "web-services", "route-rate-cards" }, testName = "Bug7935Test")
@TransactionConfiguration(defaultRollback = false)
@ContextConfiguration(classes = Bug7935TestConfig.class, loader = AnnotationConfigContextLoader.class)
public class Bug7935Test extends AbstractTransactionalTestNGSpringContextTests {

    private static final String TEM_ROAMING_PRODUCT = "tem_roaming_product";
    private JbillingAPI api;

    private Integer idRouteDataTable;

    private Integer idFieldRoamingType;
    private Integer idFieldZone;
    private Integer idFieldCountry;

    private static String BUG_7935_ROUTE_DATA = "id,name,next_route,product,routeid,roaming_call_type,zone,country\n"
            + "1,T-ROAM-MT,,T-ROAM-MT,T-ROAM-MT,Roaming-MT,Zone-,\n"
            + "2,T-ROAM-GPRS-NORDIC,,T-ROAM-GPRS-NORDIC,T-ROAM-GPRS-NORDIC,Roaming-Data,Zone-Europe,Faroe Islands\n"
            + "3,T-ROAM-GPRS-NORDIC,,T-ROAM-GPRS-NORDIC,T-ROAM-GPRS-NORDIC,Roaming-Data,Zone-North America,Greenland\n"
            + "4,T-ROAM-GPRS-EU,,T-ROAM-GPRS-EU,T-ROAM-GPRS-EU,Roaming-Data,Zone-EU,\n"
            + "5,T-ROAM-GPRS-EUROPE,,T-ROAM-GPRS-EUROPE,T-ROAM-GPRS-EUROPE,Roaming-Data,Zone-Europe,\n"
            + "6,T-ROAM-GPRS-ROW,,T-ROAM-GPRS-ROW,T-ROAM-GPRS-ROW,Roaming-Data,Zone-,\n"
            + "7,T-ROAM-SMS-EU,,T-ROAM-SMS-EU,T-ROAM-SMS-EU,Roaming-SMS,Zone-EU,\n"
            + "8,T-ROAM-SMS,,T-ROAM-SMS,T-ROAM-SMS,Roaming-SMS,Zone-,\n"
            + "9,T-ROAM-MO-EU,,T-ROAM-MO-EU,T-ROAM-MO-EU,Roaming-MO,Zone-EU,\n"
            + "10,T-ROAM-MO,,T-ROAM-MO,T-ROAM-MO,Roaming-MO,Zone-,\n";

    @Test
    public void roamingMoInZoneWorldShouldBeFound() throws IOException {

        findRouteFor("roaming-call-type=Roaming-MO;zone=Zone-World;country=zxy", "T-ROAM-MO");
    }

    @Test
    public void roamingMoInZoneEuShouldBeFound() throws IOException {

        findRouteFor("roaming-call-type=Roaming-MO;zone=Zone-EU;country=zxy", "T-ROAM-MO-EU");
    }

    @Test
    public void roamingMoInZoneEuropeShouldBeFound() throws IOException {

        findRouteFor("roaming-call-type=Roaming-MO;zone=Zone-Europe;country=zxy", "T-ROAM-MO");
    }

    @Test
    public void roamingMoInZoneXEuropeShouldBeFound() throws IOException {

        findRouteFor("roaming-call-type=Roaming-MO;zone=Zone-xEurope;country=zxy", "T-ROAM-MO");
    }

    @Test
    public void roamingMtInZoneXyzShouldBeFound() throws IOException {

        findRouteFor("roaming-call-type=Roaming-MT;zone=Zone-xyz;country=zxy", "T-ROAM-MT");
    }

    @Test
    public void roamingMtInZoneEuShouldBeFound() throws IOException {

        findRouteFor("roaming-call-type=Roaming-MT;zone=Zone-EU;country=zxy", "T-ROAM-MT");
    }

    @BeforeClass
    protected void setUpBeforeClass() throws Exception {
        api = JbillingAPIFactory.getAPI();
        com.sapienter.jbilling.server.util.Context.setApplicationContext(applicationContext);
        registerRateBeans();

        RouteDTO r = new RouteDAS().getRoute(1, TEM_ROAMING_PRODUCT);
        if (r != null) {
            api.deleteRoute(r.getId());
        }

        idRouteDataTable = createRouteDataTable(TEM_ROAMING_PRODUCT, TEM_ROAMING_PRODUCT, BUG_7935_ROUTE_DATA);

        createMatchingFields();
        removeRateBeans();
        registerRateBeans();

    }

    @AfterClass
    protected void cleanUp() throws Exception {

        if (idFieldCountry != null)
            api.deleteMatchingField(idFieldCountry);
        if (idFieldZone != null)
            api.deleteMatchingField(idFieldZone);
        if (idFieldRoamingType != null)
            api.deleteMatchingField(idFieldRoamingType);

        if (idRouteDataTable != null)
            api.deleteRoute(idRouteDataTable);

        removeRateBeans();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void registerRateBeans() {

        for (RateCardDTO rateCard : new RateCardDAS().findAll()) {
            new RateCardBL(rateCard).registerSpringBeans();
        }
        for (RouteDTO routeDTO : new RouteDAS().findAll()) {
            new RouteBL(routeDTO).registerSpringBeans();
        }
        for (RouteRateCardDTO routeRateCardDTO : new RouteRateCardDAS().findAll()) {
            new RouteBasedRateCardBL(routeRateCardDTO).registerSpringBeans();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void removeRateBeans() {

        for (RouteRateCardDTO routeRateCardDTO : new RouteRateCardDAS().findAll()) {
            new RouteBasedRateCardBL(routeRateCardDTO).removeSpringBeans();
        }
        for (RouteDTO routeDTO : new RouteDAS().findAll()) {
            new RouteBL(routeDTO).removeSpringBeans();
        }
        for (RateCardDTO rateCard : new RateCardDAS().findAll()) {
            new RateCardBL(rateCard).removeSpringBeans();
        }
    }

    private File prepareCsvFileWithRouteData(String fileName, String content) throws IOException {
        File routeFile = File.createTempFile(fileName, ".csv");
        routeFile.deleteOnExit();
        writeToFile(routeFile, content);
        return routeFile;
    }

    private void writeToFile(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }

    private Integer createRouteDataTable(String name, String tableName, String content) throws IOException {

        RouteWS routeWS = new RouteWS();
        routeWS.setRootTable(true);
        routeWS.setName(name);
        routeWS.setTableName(tableName);
        return api.createRoute(routeWS, prepareCsvFileWithRouteData(tableName, content));
    }

    private MatchingFieldWS buildMatchingField(String name, String order, MatchingFieldType type, boolean required,
            String mediationField, String matchingField) {

        MatchingFieldWS matchingFieldWS = new MatchingFieldWS();
        matchingFieldWS.setDescription(name);
        matchingFieldWS.setMatchingField(matchingField);
        matchingFieldWS.setMediationField(mediationField);
        matchingFieldWS.setOrderSequence(order);
        matchingFieldWS.setType(type.name());
        matchingFieldWS.setRequired(required);

        return matchingFieldWS;
    }

    private Integer attachMatchingFieldToDataTable(MatchingFieldWS matchingField, Integer idRouteDataTable) {

        matchingField.setRouteId(idRouteDataTable);
        return api.createMatchingField(matchingField);
    }

    private List<PricingField> constructPricingFields(String fields) {

        List<PricingField> pricingFields = new ArrayList<PricingField>();

        String[] tokens = fields.split(";");
        for (String token : tokens) {
            String[] parts = token.split("=");
            PricingField pricingField = new PricingField();
            pricingField.setName(parts[0].trim());
            pricingField.setType(PricingField.Type.STRING);
            pricingField.setStrValue(parts[1].trim());
            PricingField.add(pricingFields, pricingField);
        }
        return pricingFields;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private RouteRecord findRouteRecord(Integer idRouteDataTable, List<PricingField> pricingFields) {

        RouteBL routeBL = new RouteBL(idRouteDataTable);
        RouteDTO rootRoute = new RouteDAS().getRoute(idRouteDataTable);
        RouteFinder routeFinder = routeBL.getBeanFactory().getFinderInstance();
        return routeFinder.findTreeRoute(rootRoute, pricingFields);
    }

    private void createMatchingFields() {
        /* @formatter:off */
        /* 
         * |Roaming Call Type|1 |EXACT     |Yes |roaming-call-type|roaming_call_type
         * |Roaming Zone     |2 |BEST_MATCH|Yes |zone             |zone
         * |Roaming Country  |3 |BEST_MATCH|Yes |country          |country
         */
        /* @formatter:on */

        MatchingFieldWS matchingField = buildMatchingField("Roaming Call Type", "1", MatchingFieldType.EXACT, true,
                "roaming-call-type", "roaming_call_type");

        idFieldRoamingType = attachMatchingFieldToDataTable(matchingField, idRouteDataTable);

        matchingField = buildMatchingField("Roaming Zone", "2", MatchingFieldType.BEST_MATCH, true, "zone", "zone");
        idFieldZone = attachMatchingFieldToDataTable(matchingField, idRouteDataTable);

        matchingField = buildMatchingField("Roaming Country", "3", MatchingFieldType.BEST_MATCH, true, "country",
                "country");
        idFieldCountry = attachMatchingFieldToDataTable(matchingField, idRouteDataTable);
    }

    private void findRouteFor(String testInput, String testOutput) {

        RouteRecord rr = findRouteRecord(idRouteDataTable, constructPricingFields(testInput));
        if (rr != null) {
            assertEquals(testOutput, rr.getRouteId());
        } else {
            fail("Failed on input: " + testInput + ", expected output: " + testOutput);
        }
    }
}
