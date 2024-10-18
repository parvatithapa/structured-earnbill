package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.batch.item.validator.ValidationException;

import java.io.File;
import java.math.BigDecimal;

/**
 * Created by Taimoor Choudhary on 12/13/17.
 */
public class BulkLoaderUtility {

    //Method is used to trim the currency sign (e.g. $) from the price.
    public static BigDecimal getPrice(String data)
    {
        if(Character.isDigit(data.charAt(0))) {
            return new BigDecimal(data.trim());
        }
        else {
            return new BigDecimal(data.substring(1).trim());
        }
    }

    //Method is used to validate the price obtained from CSV files.
    public static boolean isValidPrice(String data)
    {
        if(!Character.isDigit(data.charAt(0))) {
            data = data.substring(1);
        }

        if (!NumberUtils.isNumber(data.trim())) {
            return false;
        }
        else
        {
            return true;
        }

    }

    public static File createDirectory(String directoryPath) {
        File theDir = new File(directoryPath);
        if (!theDir.exists()) {
            try {
                theDir.mkdirs();
            } catch (SecurityException se) {
                System.out.println("Exception while creating directory");
            }
        }
        return theDir;
    }

    public static String checkPriceStrategy(String name){
        if (PriceModelStrategy.FLAT.name().compareTo(name) == 0) return PriceModelStrategy.FLAT.name();
        if (PriceModelStrategy.LINE_PERCENTAGE.name().compareTo(name) == 0) return PriceModelStrategy.LINE_PERCENTAGE.name();
        if (PriceModelStrategy.RATE_CARD.name().compareTo(name) == 0) return PriceModelStrategy.RATE_CARD.name();
        if (PriceModelStrategy.ZERO.name().compareTo(name) == 0) return PriceModelStrategy.ZERO.name();
        if (PriceModelStrategy.COMPANY_POOLED.name().compareTo(name) == 0) return PriceModelStrategy.COMPANY_POOLED.name();
        if (PriceModelStrategy.POOLED.name().compareTo(name) == 0) return PriceModelStrategy.POOLED.name();
        if (PriceModelStrategy.CAPPED_GRADUATED.name().compareTo(name) == 0) return PriceModelStrategy.CAPPED_GRADUATED.name();
        if (PriceModelStrategy.GRADUATED.name().compareTo(name) == 0) return PriceModelStrategy.GRADUATED.name();
        if (PriceModelStrategy.TIERED.name().compareTo(name) == 0) return PriceModelStrategy.TIERED.name();
        if (PriceModelStrategy.VOLUME_PRICING.name().compareTo(name) == 0) return PriceModelStrategy.VOLUME_PRICING.name();
        if (PriceModelStrategy.TIME_OF_DAY.name().compareTo(name) == 0) return PriceModelStrategy.TIME_OF_DAY.name();
        return PriceModelStrategy.ZERO.name();
    }

    public static boolean PriceModelInitializationRequired(PriceModelWS priceModelWS, String newPriceModelType){

        if( null == priceModelWS){
            return true;
        }

        if(!(newPriceModelType.equals("TIER") && priceModelWS.getType().equals(PriceModelStrategy.TIERED.name()))) {
            return true;
        }

        return false;
    }

    public static void validatePricingModel(PriceModelWS price, PriceModelWS newPriceModel) throws ValidationException{
        if(price.getType().equals(PriceModelStrategy.FLAT.name()) &&
                newPriceModel.getType().equals(PriceModelStrategy.FLAT.name())){
            throw new ValidationException("Invalid pricing model. Cannot " +
                    "add two Flat pricing model in a chain");
        }

        if((price.getType().equals(PriceModelStrategy.FLAT.name()) &&
                newPriceModel.getType().equals(PriceModelStrategy.TIERED.name())) ||
                (price.getType().equals(PriceModelStrategy.TIERED.name()) &&
                        newPriceModel.getType().equals(PriceModelStrategy.FLAT.name())))
        {
            throw new ValidationException("Invalid pricing model. Cannot " +
                    "add Flat and Tiered pricing model in a chain");
        }

        if(price.getType().equals(PriceModelStrategy.TIERED.name()) &&
                newPriceModel.getType().equals(PriceModelStrategy.TIERED.name())){
            throw new ValidationException("Invalid pricing model. Cannot " +
                    "add two Tiered pricing model in a chain");
        }
    }

    public static void AddPriceModelToChain(ProductFileItem fileItem, PriceModelWS priceModel) {

        if (null == fileItem.getPriceModelWS()) {
            fileItem.setPriceModelWS(priceModel);
        } else {
            PriceModelWS parentPriceModel = fileItem.getPriceModelWS();

            while (true) {
                validatePricingModel(parentPriceModel, priceModel);

                if (null == parentPriceModel.getNext()) {
                    parentPriceModel.setNext(priceModel);
                    break;
                } else {

                    parentPriceModel = parentPriceModel.getNext();
                }
            }
        }
    }
}
