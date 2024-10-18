package com.sapienter.jbilling.server.fileProcessing.xmlParser;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aman on 24/8/15.
 */
public class Values {
    List<Option> options=new LinkedList<Option>();

    public void addOption(String value, String comment) {
        Option option = new Option();
        option.setValue(value);
        option.setComment(comment);
        options.add(option);
    }

    private static class Option{
        String value;
        String comment;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public boolean isValueExistsInOptions(String value){
        for(Option option :options){
            if(option.getValue().equals(value)){
                return true;
            }
        }
        return false;
    }
}
