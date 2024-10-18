package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.sapienter.jbilling.server.util.LogoType.getImages;

/**
 * Created by igutierrez on 7/27/16.
 */
public class ResourcesCopyTask extends AbstractCopyTask {

    private static final Class dependencies[] = new Class[]{};
    private static final String DASH = "-%d.";
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ResourcesCopyTask.class));

    @Override
    public void create(Integer entityId, Integer targetEntityId) {
        try {
            for(File f: getImages(entityId)) {
                if (f.exists()) {
                    FileUtils.copyFile(f, new File(f.getAbsolutePath().replace(String.format(DASH, entityId),
                                                                               String.format(DASH, targetEntityId))));
                } else {
                    LOG.warn("Could not find the logo image with name %s under %slogos/ folder", f.getName(), f.getParent());
                }
            }
        } catch (IOException e) {
            LOG.error("Exception during copy file. ", e);
        }
    }

    @Override
    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        Boolean isCopied = true;
        for (File f: getImages(targetEntityId)) {
            isCopied = f.exists();
        }

        return isCopied;
    }

    @Override
    public Class[] getDependencies() {
        return dependencies;
    }

    @Override
    public void cleanUp(Integer targetEntityId) {
        try {
            for (File f: getImages(targetEntityId)) {
                if (f.exists()) {
                    FileUtils.forceDelete(f);
                }
            }
		} catch (IOException e) {
            LOG.error("Exception during clean up task. ", e);
        }

    }
}
