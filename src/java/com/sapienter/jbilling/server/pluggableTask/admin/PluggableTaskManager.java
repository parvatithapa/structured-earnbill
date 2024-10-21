/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

/*
 * Created on Apr 15, 2003
 *
 */
package com.sapienter.jbilling.server.pluggableTask.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.util.Context;

public class PluggableTaskManager<T> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PluggableTaskManager.class));

    private List<PluggableTaskDTO> classes = null;

    private Iterator it = null;

    private int lastProcessingOrder;

    private static Map<Integer, Object[]> pluggableTaskCache = new ConcurrentHashMap<>();

    public PluggableTaskManager(Integer entityId, Integer taskCategory)
            throws PluggableTaskException {

        try {
            long pluginLoadTime = System.currentTimeMillis();
            lastProcessingOrder = 0;
            LOG.debug("Entered PluggableTaskManager");
            classes = new ArrayList<>();
            classes.addAll(((PluggableTaskDAS) Context.getBean(Context.Name.PLUGGABLE_TASK_DAS)).findByEntityCategory(
                    entityId, taskCategory));
            it = classes.iterator();
            LOG.debug("total classes = " + classes.size());
            LOG.debug("plugin load Took %s", (System.currentTimeMillis() - pluginLoadTime));

        } catch (Exception e) {
            throw new PluggableTaskException(e);
        }
    }

    public List<PluggableTaskDTO> getAllTasks() {
        return classes;
    }

    public T getNextClass() throws PluggableTaskException {
        if (it != null && it.hasNext()) {
            PluggableTaskDTO aRule = (PluggableTaskDTO) it.next();
            // check if the order by is in place
            int processingOrder = aRule.getProcessingOrder().intValue();
            // this is helpful also to identify bad data in the table
            if (processingOrder <= lastProcessingOrder) {
                // means that the results are not ordered !
                LOG.fatal("Results of processing tasks are not ordered");
                throw new PluggableTaskException("Processing tasks not ordered");
            }
            lastProcessingOrder = processingOrder;

            String className = aRule.getType().getClassName();
            String interfaceName = aRule.getType().getCategory().getInterfaceName();

            LOG.debug("Applying task " + className);

            return getInstance(className, interfaceName, aRule);
        }

        return null;

    }

    /**
     * Get a plug-in instance initialized using the parameters from the given PluggableTaskDTO entity. Used to
     * load and initialize a plug-in from the database.
     *
     * @param className class name to get instance of
     * @param interfaceName plug-in interface
     * @param pluggableTask pluggable task entity to initialize plug-in parameters from
     * @return instance of the plug-in class initialized with parameters
     * @throws PluggableTaskException throw if an unhandled exception occurs
     */
    @SuppressWarnings("unchecked")
    public T getInstance(String className, String interfaceName, PluggableTaskDTO pluggableTask)
            throws PluggableTaskException {

        try {
            Integer key = pluggableTask.getId();
            Object[] cacheObj = pluggableTaskCache.get(key);
            if(cacheObj != null && cacheObj[0].equals(pluggableTask.getVersionNum())) {
                return (T)cacheObj[1];
            }
            T instance = (T) getInstance(className, interfaceName);
            PluggableTask task = (PluggableTask) instance;
            task.initializeParamters(pluggableTask);

            if(task.isSingleton()) {
                pluggableTaskCache.put(key, new Object[] {pluggableTask.getVersionNum(), task});
            }
            return instance;

        } catch (Exception e) {
            throw new PluggableTaskException("Unhandled exception initializing plug-in instance", e);
        }
    }

    /**
     * Get a plug-in instance for the given class name, ensuring that the resulting instance matches
     * the desired plug-in interface.
     *
     * @param className class name to get instance of
     * @param interfaceName plug-in interface
     * @return instance of the plug-in class
     * @throws PluggableTaskException thrown if plug-in class or interface could not be found, or if plug-in
     *                                does not implement the interface.
     */
    public static Object getInstance(String className, String interfaceName) throws PluggableTaskException {
        try {
            Class task = getClass(className);
            Class iface =  getClass(interfaceName);

            if (task == null) {
                throw new PluggableTaskException("Could not load plug-in class '" + className + "', class not found.");
            }

            if (iface == null) {
                throw new PluggableTaskException("Could not load interface '" + interfaceName + "', class not found.");
            }

            if (!iface.isAssignableFrom(task)) {
                throw new PluggableTaskException("Plug-in '" + className + "' does not implement '" + interfaceName + "'");
            }

            LOG.debug("Creating a new instance of " + className);
            return task.newInstance();

        } catch (Exception e) {
            throw new PluggableTaskException("Unhandled exception fetching plug-in instance", e);
        }
    }

    /**
     * Attempts to fetch the class by name. This method goes through all the common class loaders
     * allow the loading of plug-ins from 3rd party libraries and to ensure portability across containers.
     *
     * @param className class to load
     * @return class if name found and loadable, null if class could not be found in any class loader
     */
    private static Class getClass(String className) {
        // attempt to load from the thread class loader
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return loader.loadClass(className);

        } catch (ClassNotFoundException e) {
            LOG.debug("Cannot load class from the current thread context class loader.");
        }

        // last ditch attempt to load from whatever class loader was used to execute this code
        try {
            return Class.forName(className);

        } catch (ClassNotFoundException e) {
            LOG.fatal("Cannot load class from the caller class loader.", e);
        }

        return null;
    }

}
