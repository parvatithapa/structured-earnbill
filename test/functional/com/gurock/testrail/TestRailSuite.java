package com.gurock.testrail;

import java.util.List;

/**
 * Created by branko on 6/10/16.
 */
public class TestRailSuite {

    public TestRailSuite() {}

    public TestRailSuite(Integer id, String name, String description) {

        this.id = id;
        this.name = name;
        this.description = description;
    }

    private Integer id;
    private String name;
    private String description;
    private List<TestRailSection> sections;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TestRailSection> getSections() {
        return sections;
    }

    public void setSections(List<TestRailSection> sections) {
        this.sections = sections;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestRailSuite that = (TestRailSuite) o;

        if (!name.equals(that.name)) return false;
        return description != null ? description.equals(that.description) : that.description == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRailSuite{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + (description != null ? description : "null") + '\'' +
                '}';
    }
}
