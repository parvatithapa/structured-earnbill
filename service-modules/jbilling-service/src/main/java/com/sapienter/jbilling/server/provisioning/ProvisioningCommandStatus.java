package com.sapienter.jbilling.server.provisioning;

public enum ProvisioningCommandStatus {
	 IN_PROCESS(0, "IN_PROCESS"),
     PROCESSED(1, "PROCESSED"),
     SUCCESSFUL(2, "SUCCESSFUL"),
     FAILED(3, "FAILED"),
     UNAVAILABLE(4, "UNAVAILABLE"),
     CANCELLED(5, "CANCELLED");

     private Integer key;
     private String value;
     private ProvisioningCommandStatus(Integer key, String value) {
         this.key = key;
         this.value = value;
     }

     @Override
     public String toString(){
         return value;
     }

     public Integer toInteger(){
         return key;
     }
}
