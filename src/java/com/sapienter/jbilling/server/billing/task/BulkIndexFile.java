package com.sapienter.jbilling.server.billing.task;

import java.util.List;

public class BulkIndexFile {

    private List<IndexFileObject> indexFileObjects;
    private String originalFileName;
    private List<IndexFileObject> failedFileObjects;

    public List<IndexFileObject> getIndexFileObjects() {
        return indexFileObjects;
    }

    public void setIndexFileObjects(List<IndexFileObject> indexFileObjects) {
        this.indexFileObjects = indexFileObjects;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setFailedFileObjects(List<IndexFileObject> failedFileObjects) {
		this.failedFileObjects = failedFileObjects;
	}
    
    public List<IndexFileObject> getFailedFileObjects() {
		return failedFileObjects;
	}
    
}