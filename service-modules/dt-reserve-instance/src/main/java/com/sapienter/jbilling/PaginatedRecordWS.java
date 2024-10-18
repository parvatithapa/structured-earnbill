package com.sapienter.jbilling;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PaginatedRecordWS<E> {
    private Integer pageNumber = 1;
    private Integer pageSize = 10;
    private Integer totalRecordCount;
    private List<E> records;

    public PaginatedRecordWS(Integer pageNumber, Integer pageSize, List<E> records){
        this.pageNumber = pageNumber >0 ? pageNumber : this.pageNumber;
        this.pageSize = pageSize > 0 ? pageSize : this.pageSize;
        this.totalRecordCount = records.size();
        paginateRecords(records);
    }

    private void paginateRecords(List<E> records) {
        pageNumber -= 1;  // To consider the first page as zero. Getting the first page as 1 so doing decrement to make it as zero for records calculation
        this.records = records.stream()
                        .skip(pageNumber * pageSize)
                        .limit(pageSize)
                        .collect(Collectors.toCollection(ArrayList::new));
        pageNumber += 1;    // reset page number value to pass as json response
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalRecordCount() {
        return totalRecordCount;
    }

    public void setTotalRecordCount(Integer totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }

    public List<E> getRecords() {
        return records;
    }

    public void setRecords(List<E> records) {
        this.records = records;
    }
}
