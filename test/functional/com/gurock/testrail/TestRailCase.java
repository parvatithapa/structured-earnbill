package com.gurock.testrail;

/**
 * Created by branko on 6/10/16.
 */
public class TestRailCase {

    public TestRailCase() {}

    public TestRailCase(Integer id, String title, String customSummary,
                            String refs, Integer sectionId, Integer suiteId) {

        this.id = id;
        this.title = title;
        this.customSummary = customSummary;
        this.sectionId = sectionId;
        this.suiteId = suiteId;
        this.refs = refs;
    }

    private Integer id;
    private String title;
    private String customSummary;
    private Integer sectionId;
    private Integer suiteId;
    private String refs;

    public String getCustomSummary() {
        return customSummary;
    }

    public void setCustomSummary(String customSummary) {
        this.customSummary = customSummary;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
    }

    public Integer getSuiteId() {return suiteId;}

    public void setSuiteId(Integer suiteId) {this.suiteId = suiteId;}

    public String getRefs() {
        return refs;
    }

    public void setRefs(String refs) {
        this.refs = refs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestRailCase that = (TestRailCase) o;

        if (!title.equals(that.title)) return false;
        if (customSummary != null ? !customSummary.equals(that.customSummary) : that.customSummary != null)
            return false;
        return refs != null ? refs.equals(that.refs) : that.refs == null;

    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + (customSummary != null ? customSummary.hashCode() : 0);
        result = 31 * result + (refs != null ? refs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRailCase{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", customSummary='" + (customSummary != null ? customSummary : "null") + '\'' +
                ", sectionId=" + sectionId +
                ", suiteId=" + suiteId +
                ", refs='" + (refs != null ? refs : "null") + '\'' +
                '}';
    }
}
