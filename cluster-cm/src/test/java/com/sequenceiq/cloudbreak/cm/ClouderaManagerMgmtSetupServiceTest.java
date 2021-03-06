package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloudera.api.swagger.model.ApiConfig;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ClouderaManagerMgmtSetupServiceTest {

    @Spy
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Injected by Mockito")
    private DatabaseCommon databaseCommon = new DatabaseCommon();

    @InjectMocks
    private ClouderaManagerMgmtSetupService setupService = new ClouderaManagerMgmtSetupService();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void baseRCGNameTest() {
        assertThat(setupService.getBaseRoleConfigGroupName("test")).isEqualTo("MGMT-test-BASE");
    }

    @Test
    public void buildApiConfigList() {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL("jdbc:postgresql://somehost.com:5432/dbName");
        rdsConfig.setConnectionUserName("testUser");
        rdsConfig.setConnectionPassword("testPassword");

        List<ApiConfig> configList = setupService.buildApiConfigList("testPrefix", rdsConfig).getItems();

        assertThat(configList).hasSize(5);

        apiConfigAssertion(configList.get(0), "testPrefix_database_type", "postgresql");
        apiConfigAssertion(configList.get(1), "testPrefix_database_host", "somehost.com:5432");
        apiConfigAssertion(configList.get(2), "testPrefix_database_name", "dbName");
        apiConfigAssertion(configList.get(3), "testPrefix_database_user", "testUser");
        apiConfigAssertion(configList.get(4), "testPrefix_database_password", "testPassword");
    }

    private void apiConfigAssertion(ApiConfig item, String expectedKey, String expectedValue) {
        assertThat(item.getName()).isEqualTo(expectedKey);
        assertThat(item.getValue()).isEqualTo(expectedValue);
    }
}
