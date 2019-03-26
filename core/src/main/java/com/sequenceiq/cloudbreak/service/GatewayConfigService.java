package com.sequenceiq.cloudbreak.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@Service
public class GatewayConfigService {

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<GatewayConfig> getAllGatewayConfigs(Stack stack) {
        List<GatewayConfig> result = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getGatewayInstanceMetadata()) {
            result.add(getGatewayConfig(stack, instanceMetaData, stack.getCluster().hasGateway()));
        }
        return result;
    }

    public GatewayConfig getPrimaryGatewayConfig(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        if (gatewayInstance == null) {
            throw new NotFoundException("Gateway instance does not found");
        }
        return getGatewayConfig(stack, gatewayInstance, stack.getCluster().hasGateway());
    }

    public GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance, Boolean knoxGatewayEnabled) {
        return tlsSecurityService.buildGatewayConfig(stack.getId(), gatewayInstance, stack.getGatewayPort(), getSaltClientConfig(stack), knoxGatewayEnabled);
    }

    public String getPrimaryGatewayIp(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        return gatewayInstance == null ? null : getGatewayIp(stack, gatewayInstance);
    }

    public String getPrimaryGatewayIp(AutoscaleStack stack) {
        String gatewayIP = Optional.ofNullable(stack.getPublicIp()).orElse(stack.getPrivateIp());
        if (stack.getUsePrivateIpToTls()) {
            gatewayIP = stack.getPrivateIp();
        }
        return gatewayIP;
    }

    public String getGatewayIp(Stack stack, InstanceMetaData gatewayInstance) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (stack.getSecurityConfig().usePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    private SaltClientConfig getSaltClientConfig(Stack stack) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        return new SaltClientConfig(securityConfig.getSaltPassword(), securityConfig.getSaltBootPassword(), securityConfig.getCloudbreakSshPrivateKeyDecoded());
    }
}
