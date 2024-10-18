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
        name = "boa_bai_processed_files_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "boa_bai_processed_files",
        allocationSize = 100
)
@Table(name = "boa_bai_processed_files")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BoaBaiProcessedFileDTO implements Serializable {
    private int id;
    private String fileName;

    public BoaBaiProcessedFileDTO() {
    }

    public BoaBaiProcessedFileDTO(int id) {
        this.id = id;
    }

    public BoaBaiProcessedFileDTO(String fileName) {
        setFileName(fileName);
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_BOA_BAI_PROCESSED_FILES;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "boa_bai_processed_files_GEN")
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
}
