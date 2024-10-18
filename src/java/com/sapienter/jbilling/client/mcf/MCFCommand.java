package com.sapienter.jbilling.client.mcf;

/**
 * Created by pablo_galera on 15/02/17.
 */
public abstract class MCFCommand {

    private String position;
    private String name;
    private String date;
    static final String comma = ",";

    public MCFCommand(String position, String name, String date) {
        this.position = position;
        this.name = name;
        this.date = date;
    }

    public abstract String getCommand();

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
