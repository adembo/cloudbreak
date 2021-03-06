package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus;

@Service
public class OpenStackInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackInstanceConnector.class);

    private static final int CONSOLE_OUTPUT_LINES = Integer.MAX_VALUE;

    @Inject
    private OpenStackClient openStackClient;

    @Value("${cb.openstack.hostkey.verify:}")
    private boolean verifyHostKey;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        if (!verifyHostKey) {
            throw new CloudOperationNotSupportedException("Host key verification is disabled on OPENSTACK");
        }
        OSClient<?> osClient = openStackClient.createOSClient(authenticatedContext);
        return osClient.compute().servers().getConsoleOutput(vm.getInstanceId(), CONSOLE_OUTPUT_LINES);
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return executeAction(ac, vms, Action.START);
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return executeAction(ac, vms, Action.STOP);
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        OSClient<?> osClient = openStackClient.createOSClient(ac);
        for (CloudInstance vm : vms) {
            Optional<Server> server = Optional.ofNullable(vm.getInstanceId())
                                                .map(iid -> osClient.compute().servers().get(iid));
            if (server.isPresent()) {
                statuses.add(new CloudVmInstanceStatus(vm, NovaInstanceStatus.get(server.get())));
            } else {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
            }
        }
        return statuses;
    }

    private List<CloudVmInstanceStatus> executeAction(AuthenticatedContext ac, Iterable<CloudInstance> cloudInstances, Action action) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        OSClient<?> osClient = openStackClient.createOSClient(ac);
        for (CloudInstance cloudInstance : cloudInstances) {
            ActionResponse actionResponse = osClient.compute().servers().action(cloudInstance.getInstanceId(), action);
            if (actionResponse.isSuccess()) {
                statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS));
            } else {
                statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, actionResponse.getFault()));
            }
        }
        return statuses;
    }
}
