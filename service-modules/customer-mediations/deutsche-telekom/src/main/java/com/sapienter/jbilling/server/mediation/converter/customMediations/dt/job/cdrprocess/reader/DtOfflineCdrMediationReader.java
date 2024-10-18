package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.reader;

import com.sapienter.jbilling.server.mediation.MapCallDataRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResourceAware;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DtOfflineCdrMediationReader extends AbstractItemStreamItemReader<MapCallDataRecord> implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String RESOURCE_KEY = "resourceIndex";

    private ResourceAwareItemReaderItemStream<? extends MapCallDataRecord> delegate;

    private Resource[] resources;

    private boolean saveState = true;

    private int currentResource = -1;

    // signals there are no resources to read -> just return null on first read
    private boolean noInput;

    private boolean strict = false;

    private String archiveFolder;
    private boolean preserveProcessedFile;

    /**
     * In strict mode the reader will throw an exception on
     * {@link #open(org.springframework.batch.item.ExecutionContext)} if there are no resources to read.
     *
     * @param strict false by default
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    private Comparator<Resource> comparator = new Comparator<Resource>() {

        /**
         * Compares resource filenames.
         */
        @Override
        public int compare(Resource r1, Resource r2) {
            return r1.getFilename().compareTo(r2.getFilename());
        }

    };

    public DtOfflineCdrMediationReader() {
        this.setExecutionContextName(ClassUtils.getShortName(MultiResourceItemReader.class));
    }

    /**
     * Reads the next item, jumping to next resource if necessary.
     */
    @Override
    public synchronized MapCallDataRecord read() throws Exception, UnexpectedInputException, ParseException {

        if (noInput) {
            return null;
        }

        // If there is no resource, then this is the first item, set the current
        // resource to 0 and open the first delegate.
        if (currentResource == -1) {

            if (currentResource == -1) {
                currentResource = 0;
                logger.debug("About to process {}", resources[currentResource]);
                delegate.setResource(resources[currentResource]);
                delegate.open(new ExecutionContext());
            }

        }

        return readNextItem();
    }

    private void cleanUpResource(Resource r) {
        if (preserveProcessedFile) {
            moveResource(r);
        } else {
            deleteResource(r);
        }
    }

    private void deleteResource(Resource r) {
        if(r != null) {
            ((FileSystemResource)r).getFile().delete();
        }
    }

    private void moveResource(Resource r) {
        if (r != null) {
            Path source = ((FileSystemResource) r).getFile().toPath();
            try {
                Path target = Paths.get(archiveFolder, source.getFileName().toString());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            } catch (IOException e) {
                logger.error("Error moving file to processed folder", e);
                deleteResource(r);
            }
        }
    }

    /**
     * Use the delegate to read the next item, jump to next resource if current one is exhausted. Items are appended to
     * the buffer.
     *
     * @return next item from input
     */
    private MapCallDataRecord readNextItem() throws Exception {

        MapCallDataRecord item = readFromDelegate();

        if(item == null) {

            while (item == null) {
                currentResource++;

                if (currentResource >= resources.length) {
                    return null;
                }

                delegate.close();
                cleanUpResource(resources[currentResource - 1]);
                logger.debug("About to process {}", resources[currentResource]);
                delegate.setResource(resources[currentResource]);
                delegate.open(new ExecutionContext());

                item = readFromDelegate();
            }
        }

        return item;
    }

    private MapCallDataRecord readFromDelegate() throws Exception {
        MapCallDataRecord item = delegate.read();
        if (item instanceof ResourceAware) {
            ((ResourceAware) item).setResource(getCurrentResource());
        }
        return item;
    }

    /**
     * Close the {@link #setDelegate(org.springframework.batch.item.file.ResourceAwareItemReaderItemStream)} reader and reset instance variable values.
     */
    @Override
    public void close() throws ItemStreamException {
        super.close();

        if (!this.noInput) {
            delegate.close();
            cleanUpResource(getCurrentResource() == null && resources.length > 0 ? resources[resources.length-1] : getCurrentResource());
        }

        noInput = false;
    }

    /**
     * Figure out which resource to start with in case of restart, open the delegate and restore delegate's position in
     * the resource.
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
        Assert.notNull(resources, "Resources must be set");

        noInput = false;
        if (resources.length == 0) {
            if (strict) {
                throw new IllegalStateException(
                        "No resources to read. Set strict=false if this is not an error condition.");
            } else {
                logger.warn("No resources to read. Set strict=true if this should be an error condition.");
                noInput = true;
                return;
            }
        }

        Arrays.sort(resources, comparator);

        if (executionContext.containsKey(getExecutionContextKey(RESOURCE_KEY))) {
            currentResource = executionContext.getInt(getExecutionContextKey(RESOURCE_KEY));

            // context could have been saved before reading anything
            if (currentResource == -1) {
                currentResource = 0;
            }

            logger.debug("About to process {}", resources[currentResource]);
            delegate.setResource(resources[currentResource]);
            delegate.open(executionContext);
        } else {
            currentResource = -1;
        }
    }

    /**
     * Store the current resource index and position in the resource.
     */
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        if (saveState) {
            executionContext.putInt(getExecutionContextKey(RESOURCE_KEY), currentResource);
            delegate.update(executionContext);
        }
    }

    /**
     * @param delegate reads items from single {@link Resource}.
     */
    public void setDelegate(ResourceAwareItemReaderItemStream<? extends MapCallDataRecord> delegate) {
        this.delegate = delegate;
    }

    /**
     * Set the boolean indicating whether or not state should be saved in the provided {@link org.springframework.batch.item.ExecutionContext} during
     * the {@link org.springframework.batch.item.ItemStream} call to update.
     *
     * @param saveState
     */
    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    /**
     * @param comparator used to order the injected resources, by default compares {@link Resource#getFilename()}
     *                   values.
     */
    public void setComparator(Comparator<Resource> comparator) {
        this.comparator = comparator;
    }

    /**
     * @param resources input resources
     */
    public void setResources(Resource[] resources) {
        Assert.notNull(resources, "The resources must not be null");
        this.resources = Arrays.asList(resources).toArray(new Resource[resources.length]);
    }

    public Resource getCurrentResource() {
        if (currentResource >= resources.length || currentResource < 0) {
            return null;
        }
        return resources[currentResource];
    }


    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        try {
            String workFolder = stepExecution.getJobExecution().getExecutionContext().getString("work_folder") + "/cdr/";
            List<Resource> resourceList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : stepExecution.getExecutionContext().entrySet()) {
                if (entry.getKey().startsWith("file")) {
                    logger.debug("Will process: {}", entry.getValue().toString());
                    resourceList.add(new FileSystemResource(workFolder + entry.getValue().toString()));
                }
            }
            setResources(resourceList.toArray(new Resource[0]));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setArchiveFolder(String archiveFolder) {
        this.archiveFolder = archiveFolder;
    }

    public void setPreserveProcessedFile(boolean preserveProcessedFile) {
        this.preserveProcessedFile = preserveProcessedFile;
    }
}
