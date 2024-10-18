package com.sapienter.jbilling.server.provisioning;

import com.sapienter.jbilling.client.process.SchedulerCloudHelper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Created by marcolin on 28/06/16.
 */
public abstract class ClusterAwareProvisioningMDB implements MessageListener {

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onMessage(Message message) {
        if (SchedulerCloudHelper.launchMessageDriveBeanTrackingBatch(messageDrivenBeanClass(), message)) {
            doOnMessage(message);
        }
    }

    public abstract void doOnMessage(Message message);

    public Class messageDrivenBeanClass() {
        return this.getClass();
    }
}
