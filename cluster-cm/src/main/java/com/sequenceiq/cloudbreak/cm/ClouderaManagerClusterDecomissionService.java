package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
@Scope("prototype")
public class ClouderaManagerClusterDecomissionService implements ClusterDecomissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterDecomissionService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerDecomissioner clouderaManagerDecomissioner;

    @Inject
    private ApplicationContext applicationContext;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerClusterDecomissionService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            client = clouderaManagerClientFactory.getClient(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public void verifyNodesAreRemovable(Multimap<Long, HostMetadata> hostGroupWithInstances, Set<HostGroup> hostGroups, int defaultRootVolumeSize,
            List<InstanceMetaData> notDeletedNodes) {
        clouderaManagerDecomissioner.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, client);
    }

    @Override
    public Set<String> collectDownscaleCandidates(@Nonnull HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        return clouderaManagerDecomissioner.collectDownscaleCandidates(client, stack, hostGroup, scalingAdjustment, defaultRootVolumeSize,
                instanceMetaDatasInStack);
    }

    @Override
    public Map<String, HostMetadata> collectHostsToRemove(@Nonnull HostGroup hostGroup, Set<String> hostNames) {
        return clouderaManagerDecomissioner.collectHostsToRemove(stack, hostGroup, hostNames, client);
    }

    @Override
    public Set<HostMetadata> decommissionClusterNodes(Map<String, HostMetadata> hostsToRemove) {
        return clouderaManagerDecomissioner.decommissionNodes(stack, hostsToRemove, client);
    }

    @Override
    public void removeManagementServices() {
        clouderaManagerDecomissioner.stopAndRemoveMgmtService(stack, client);
    }

    @Override
    public void deleteHostFromCluster(HostMetadata data) {
        clouderaManagerDecomissioner.deleteHost(stack, data, client);
    }

    @Override
    public void restartStaleServices() throws CloudbreakException {
        try {
            applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig)
                    .restartStaleServices(clouderaManagerClientFactory.getMgmtServiceResourceApi(client),
                            clouderaManagerClientFactory.getClustersResourceApi(client));
        } catch (ApiException e) {
            LOGGER.error("Couldn't restart stale services", e);
            throw new CloudbreakException("Couldn't restart stale services", e);
        }
    }
}