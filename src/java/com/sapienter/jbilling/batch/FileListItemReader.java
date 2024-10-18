package com.sapienter.jbilling.batch;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileListItemReader  implements ItemStreamReader<File> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FileListItemReader.class));
    public static final String PARAM_FOLDER = "folder";
    public static final String PARAM_RECURSIVE = "recursive";

    private String folder;
    private String extension = null;
    private boolean recursive = false;
    private FilenameFilter filter = null;

    private List<File> files = new ArrayList<>();
    private Iterator<File> fileIterator;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        LOG.debug("Folder: %s", folder);
        filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return extension == null || name.endsWith(extension);
            }
        };
        File file = new File(folder);
        listFiles(file, true);

        fileIterator = files.iterator();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
        fileIterator = null;
    }

    @Override
    public synchronized File read() throws Exception, ParseException, NonTransientResourceException {
        if(fileIterator.hasNext()) {
            File f = fileIterator.next();
            return f;
        } else {
            return null;
        }
    }

    private void listFiles(File file, boolean forceRecursive) {
        LOG.debug("Checking file %s", file);
        if(!file.exists()) {
        } else if(file.isFile()) {
            files.add(file);
        } else if(file.isDirectory() && (forceRecursive || recursive) ) {
            for(File f : file.listFiles(filter)) {
                listFiles(f, false);
            }
        }
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
