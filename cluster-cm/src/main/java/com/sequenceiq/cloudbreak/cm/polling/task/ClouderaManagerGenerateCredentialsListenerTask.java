package com.sequenceiq.cloudbreak.cm.polling.task;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

@Service
public class ClouderaManagerGenerateCredentialsListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to generate credentials.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Successfully generated credentials.";
    }

    @Override
    protected String getCommandName() {
        return "Generate Credentials";
    }
}
