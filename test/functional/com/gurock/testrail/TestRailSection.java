package com.gurock.testrail;

import java.util.List;

/**
 * Created by branko on 6/10/16.
 */
public class TestRailSection {

    public TestRailSection() {}

    public TestRailSection(Integer id, String name, String description, Integer suiteId, Integer parentId) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.suiteId = suiteId;
        this.parentId = parentId;
    }

    private Integer id;
    private String name;
    private String description;
    private Integer suiteId;
    private String suiteName;
    private Integer parentId;
    private String parentSuiteName;

    private List<TestRailCase> cases;

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

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Integer suiteId) {
        this.suiteId = suiteId;
    }

    public String getParentSuiteName() {
        return parentSuiteName;
    }

    public void setParentSuiteName(String parentSuiteName) {
        this.parentSuiteName = parentSuiteName;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public List<TestRailCase> getCases() {
        return cases;
    }

    public void setCases(List<TestRailCase> cases) {
        this.cases = cases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestRailSection that = (TestRailSection) o;

        if (!name.equals(that.name)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!suiteId.equals(that.suiteId)) return false;
        return parentId != null ? parentId.equals(that.parentId) : that.parentId == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + suiteId.hashCode();
        result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRailSection{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + (description != null ? description : "null") + '\'' +
                ", suiteId=" + suiteId +
                ", parentId=" + (parentId != null ? parentId : "null") +
                '}';
    }
}
