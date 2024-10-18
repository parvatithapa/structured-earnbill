package com.sapienter.jbilling.server.customerEnrollment.helper;

/**
 * Created by vivek on 28/9/15.
 */
public class CustomerEnrollmentFileGenerationHelper {

    public enum USState {
        ALABAMA("Alabama","AL"),
        ALASKA("Alaska","AK"),
        ARIZONA("Arizona","AZ"),
        ARKANSAS("Arkansas","AR"),
        CALIFORNIA("California","CA"),
        COLORADO("Colorado","CO"),
        CONNECTICUT("Connecticut","CT"),
        DELAWARE("Delaware","DE"),
        DISTRICT_OF_COLUMBIA("District of Columbia","DC"),
        FLORIDA("Florida","FL"),
        GEORGIA("Georgia","GA"),
        HAWAII("Hawaii","HI"),
        IDAHO("Idaho","ID"),
        ILLINOIS("Illinois","IL"),
        INDIANA("Indiana","IN"),
        IOWA("Iowa","IA"),
        KANSAS("Kansas","KS"),
        KENTUCKY("Kentucky","KY"),
        LOUISIANA("Louisiana","LA"),
        MAINE("Maine","ME"),
        MONTANA("Montana","MT"),
        NEBRASKA("Nebraska","NE"),
        NEVADA("Nevada","NV"),
        NEW_HAMPSHIRE("New Hampshire","NH"),
        NEW_JERSEY("New Jersey","NJ"),
        NEW_MEXICO("New Mexico","NM"),
        NEW_YORK("New York","NY"),
        NORTH_CAROLINA("North Carolina","NC"),
        NORTH_DAKOTA("North Dakota","ND"),
        OHIO("Ohio","OH"),
        OKLAHOMA("Oklahoma","OK"),
        OREGON("Oregon","OR"),
        MARYLAND("Maryland","MD"),
        MASSACHUSETTS("Massachusetts","MA"),
        MICHIGAN("Michigan","MI"),
        MINNESOTA("Minnesota","MN"),
        MISSISSIPPI("Mississippi","MS"),
        MISSOURI("Missouri","MO"),
        PENNSYLVANIA("Pennsylvania","PA"),
        RHODE_ISLAND("Rhode Island","RI"),
        SOUTH_CAROLINA("South Carolina","SC"),
        SOUTH_DAKOTA("South Dakota","SD"),
        TENNESSEE("Tennessee","TN"),
        TEXAS("Texas","TX"),
        UTAH("Utah","UT"),
        VERMONT("Vermont","VT"),
        VIRGINIA("Virginia","VA"),
        WASHINGTON("Washington","WA"),
        WEST_VIRGINIA("West Virginia","WV"),
        WISCONSIN("Wisconsin","WI"),
        WYOMING("Wyoming","WY");

        String stateName;
        String abbreviation;

        USState(String stateName, String abbreviation) {
            this.stateName = stateName;
            this.abbreviation = abbreviation;
        }

        public static String getAbbreviationForState(String stateName) {
            //if state has only two character then this is the abbreviation state code
            if(stateName.trim().length()==2){
                return stateName.toUpperCase();
            }

            String abbreviation = "";
            for (USState state : USState.values()) {
                if(state.stateName.equalsIgnoreCase(stateName)) {
                    abbreviation = state.abbreviation;
                }
            }
            return abbreviation;
        }
    }
}
