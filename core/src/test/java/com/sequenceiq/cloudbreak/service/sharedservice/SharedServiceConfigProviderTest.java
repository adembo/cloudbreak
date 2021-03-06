package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class SharedServiceConfigProviderTest {

    private static final Long TEST_LONG_VALUE = 1L;

    @InjectMocks
    private SharedServiceConfigProvider underTest;

    @Mock
    private StackService stackService;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private Blueprint blueprint;

    @Mock
    private Stack publicStack;

    @Mock
    private Cluster publicStackCluster;

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

    @Mock
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Mock
    private StackInputs stackInputs;

    @Mock
    private DatalakeConfigApiConnector datalakeConfigApiConnector;

    @Mock
    private RemoteDataContextWorkaroundService remoteDataContextWorkaroundService;

    @Mock
    private DatalakeConfigApi datalakeConfigApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(remoteDataContextWorkaroundService.prepareRdsConfigs(any(Cluster.class), any(DatalakeResources.class))).thenReturn(new HashSet<>());
        when(remoteDataContextWorkaroundService.prepareFilesytem(any(Cluster.class), any(DatalakeResources.class))).thenReturn(new FileSystem());
        when(datalakeConfigApiConnector.getConnector(any(Stack.class))).thenReturn(datalakeConfigApi);
        when(datalakeConfigApiConnector.getConnector(any(URL.class), anyString(), anyString())).thenReturn(datalakeConfigApi);
    }

    @Test
    public void testConfigureClusterWhenConnectedClusterRequestIsNullThenOriginalClusterInstanceShouldReturn() {
        Cluster cluster = new Cluster();
        cluster.setStack(publicStack);
        when(publicStack.getDatalakeResourceId()).thenReturn(null);

        Cluster result = underTest.configureCluster(cluster, user, workspace);

        Assert.assertEquals(cluster, result);
        verify(datalakeResourcesService, times(0)).findById(anyLong());
        assertNull(cluster.getRdsConfigs());
    }

    @Test
    public void testConfigureClusterWithDl() {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        DatalakeResources datalakeResources = new DatalakeResources();

        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(datalakeResourcesService.findById(anyLong())).thenReturn(Optional.of(datalakeResources));

        Cluster result = underTest.configureCluster(requestedCluster, user, workspace);

        Assert.assertTrue(result.getRdsConfigs().isEmpty());
        verify(datalakeResourcesService, times(1)).findById(anyLong());
    }

    @Test
    public void testConfigureClusterIfSourceClusterContainsDifferentResourceStatusThenTheDefaultOnesWouldNotBeStoredInTheReturnCluster() {
        Cluster requestedCluster = createBarelyConfiguredRequestedCluster();
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setRdsConfigs(createRdsConfigs(DEFAULT, DEFAULT_DELETED, USER_MANAGED));

        when(stackService.getById(TEST_LONG_VALUE)).thenReturn(publicStack);
        when(publicStack.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStackCluster.getId()).thenReturn(TEST_LONG_VALUE);
        when(publicStack.getCluster()).thenReturn(publicStackCluster);
        when(datalakeResourcesService.findById(anyLong())).thenReturn(Optional.of(datalakeResources));

        Cluster result = underTest.configureCluster(requestedCluster, user, workspace);

        Assert.assertEquals(0L, result.getRdsConfigs().size());
        result.getRdsConfigs().forEach(rdsConfig -> Assert.assertNotEquals(DEFAULT, rdsConfig.getStatus()));
    }

    @Test
    public void testPrepareDLConfNotAttached() {
        when(publicStack.getDatalakeResourceId()).thenReturn(null);
        when(publicStack.getEnvironmentCrn()).thenReturn("");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        when(publicStack.getWorkspace()).thenReturn(workspace);
        when(datalakeResourcesService.findDatalakeResourcesByWorkspaceAndEnvironment(anyLong(), anyString()))
                .thenReturn(Collections.emptySet());

        Stack stack = underTest.prepareDatalakeConfigs(publicStack);

        assertNull(stack.getDatalakeResourceId());
        assertEquals(publicStack.getInputs(), stack.getInputs());
        verify(datalakeResourcesService, times(0)).findById(anyLong());
    }

//    @Test
//    public void testPrepareDLConfCumulus() throws IOException {
//        Stack stackIn = new Stack();
//        stackIn.setDatalakeResourceId(1L);
//
//        Workspace workspace = new Workspace();
//        workspace.setId(1L);
//        stackIn.setWorkspace(workspace);
//        when(datalakeResourcesService.findDatalakeResourcesByWorkspaceAndEnvironment(anyLong(), anyString()))
//                .thenReturn(Collections.emptySet());
//
//
//        stackIn.setCredentialCrn("aCredentialCRN");
//
//        DatalakeResources datalakeResources = new DatalakeResources();
//        datalakeResources.setDatalakeStackId(1L);
//        when(datalakeResourcesService.findById(anyLong())).thenReturn(Optional.of(datalakeResources));
//
//        DatalakeConfigApi connector = mock(DatalakeConfigApi.class);
//        when(ambariDatalakeConfigProvider.getAdditionalParameters(stackIn, datalakeResources)).thenReturn(Collections.singletonMap("test", "data"));
//        when(ambariDatalakeConfigProvider.getBlueprintConfigParameters(datalakeResources, stackIn, connector))
//                .thenReturn(Collections.singletonMap("test", "data"));
//        when(stackService.save(stackIn)).thenReturn(stackIn);
//        stackIn.setInputs(new Json(stackInputs));
//
//        Stack stack = underTest.prepareDatalakeConfigs(stackIn);
//
//        verify(stackService, times(0)).getById(anyLong());
//
//        StackInputs stackInputs = stack.getInputs().get(StackInputs.class);
//
//        assertEquals(1L, stackInputs.getDatalakeInputs().size());
//        assertEquals(1L, stackInputs.getFixInputs().size());
//    }

    @Test
    public void testPrepareDLConfWithCloudDL() throws IOException {
        Stack stackIn = new Stack();
        stackIn.setDatalakeResourceId(1L);
        DatalakeResources datalakeResources = new DatalakeResources();
        long datalakeStackId = 11L;
        datalakeResources.setDatalakeStackId(datalakeStackId);

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        stackIn.setWorkspace(workspace);
        when(datalakeResourcesService.findDatalakeResourcesByWorkspaceAndEnvironment(anyLong(), anyString()))
                .thenReturn(Collections.emptySet());

        when(datalakeResourcesService.findById(anyLong())).thenReturn(Optional.of(datalakeResources));
        when(ambariDatalakeConfigProvider.getAdditionalParameters(stackIn, datalakeResources)).thenReturn(Collections.singletonMap("test", "data"));
        when(ambariDatalakeConfigProvider.getBlueprintConfigParameters(eq(datalakeResources), eq(stackIn), any(DatalakeConfigApi.class)))
                .thenReturn(Collections.singletonMap("test", "data"));
        when(stackService.save(stackIn)).thenReturn(stackIn);
        Stack dlStack = new Stack();
        dlStack.setId(datalakeStackId);
        dlStack.setCluster(createBarelyConfiguredRequestedCluster());
        when(stackService.getById(dlStack.getId())).thenReturn(dlStack);
        stackIn.setInputs(new Json(stackInputs));

        Stack stack = underTest.prepareDatalakeConfigs(stackIn);

        verify(stackService, times(1)).getById(datalakeResources.getDatalakeStackId());

        StackInputs stackInputs = stack.getInputs().get(StackInputs.class);
        assertEquals(1L, stackInputs.getDatalakeInputs().size());
        assertEquals(1L, stackInputs.getFixInputs().size());
    }

    private Cluster createBarelyConfiguredRequestedCluster() {
        Cluster requestedCluster = new Cluster();
        requestedCluster.setRdsConfigs(new LinkedHashSet<>());
        requestedCluster.setBlueprint(blueprint);
        requestedCluster.setStack(publicStack);
        return requestedCluster;
    }

    private Set<RDSConfig> createRdsConfigs(ResourceStatus... statuses) {
        Set<RDSConfig> configs = new LinkedHashSet<>(statuses.length);
        long id = 0;
        for (ResourceStatus status : statuses) {
            RDSConfig config = new RDSConfig();
            config.setId(id++);
            config.setStatus(status);
            configs.add(config);
        }
        return configs;
    }

}