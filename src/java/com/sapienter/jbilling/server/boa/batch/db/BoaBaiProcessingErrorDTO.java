package com.sapienter.jbilling.server.boa.batch.db;

import com.sapienter.jbilling.server.util.Constants;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Javier Rivero
 * @since 07/01/16.
 */
@Entity
@TableGenerator(
        name = "boa_bai_processing_errors_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "boa_bai_processing_errors",
        allocationSize = 100
)
@Table(name = "boa_bai_processing_errors")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BoaBaiProcessingErrorDTO implements Serializable {
    private int id;
    private String fileName;
    private String rawData;
    private String processingErrors;

    public BoaBaiProcessingErrorDTO() {
    }

    public BoaBaiProcessingErrorDTO(int id) {
        this.id = id;
    }

    public BoaBaiProcessingErrorDTO(String fileName) {
        setFileName(fileName);
    }
    public BoaBaiProcessingErrorDTO(String fileName, String rawData, String processingErrors) {
        setFileName(fileName);
        setRawData(rawData);
        setProcessingErrors(processingErrors);
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_BOA_BAI_PROCESSING_ERRORS;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "boa_bai_processing_errors_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "file_name", length = 200)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Column(name = "raw_data", length = 200)
    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    @Column(name = "processing_errors", length = 4000)
    public String getProcessingErrors() {
        return processingErrors;
    }

    public void setProcessingErrors(String processingErrors) {
        this.processingErrors = processingErrors;
    }
}
