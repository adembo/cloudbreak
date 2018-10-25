package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Type;

import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "InstanceMetaData.findHostInStack",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.stack.id= :stackId "
                        + "AND i.discoveryFQDN= :hostName "
                        + "AND i.instanceStatus <> 'TERMINATED' "),
        @NamedQuery(
                name = "InstanceMetaData.findUnregisteredHostsInInstanceGroup",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.id= :instanceGroupId "
                        + "AND i.instanceStatus = 'UNREGISTERED'"),
        @NamedQuery(
                name = "InstanceMetaData.findUnusedHostsInInstanceGroup",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.id= :instanceGroupId "
                        + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED')"),

        @NamedQuery(
                name = "InstanceMetaData.findNotTerminatedForStack",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.stack.id= :stackId "
                        + "AND i.instanceStatus <> 'TERMINATED' "),
        @NamedQuery(
                name = "InstanceMetaData.findAllInStack",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.stack.id= :stackId"),
        @NamedQuery(
                name = "InstanceMetaData.findByInstanceId",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceId= :instanceId AND i.instanceGroup.stack.id= :stackId "),
        @NamedQuery(
                name = "InstanceMetaData.findAliveInstancesHostNamesInInstanceGroup",
                query = "SELECT i.discoveryFQDN FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.id = :instanceGroupId "
                        + "AND i.instanceStatus <> 'TERMINATED' "),
        @NamedQuery(
                name = "InstanceMetaData.findAliveInstancesInInstanceGroup",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.id = :instanceGroupId "
                        + "AND i.instanceStatus <> 'TERMINATED' "),
        @NamedQuery(
                name = "InstanceMetaData.findRemovableInstances",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.stack.id= :stackId "
                        + "AND i.instanceGroup.groupName= :groupName "
                        + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED', 'DECOMMISSIONED', 'FAILED', 'STOPPED')"),
        @NamedQuery(
                name = "InstanceMetaData.findNotTerminatedByPrivateAddress",
                query = "SELECT i FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.stack.id= :stackId "
                        + "AND i.privateIp= :privateAddress "
                        + "AND i.instanceStatus <> 'TERMINATED' "),
        @NamedQuery(
                name = "InstanceMetaData.getServerCertByStackId",
                query = "SELECT i.serverCert FROM InstanceMetaData i "
                        + "WHERE i.instanceGroup.stack.id= :stackId "
                        + "AND i.instanceMetadataType = 'GATEWAY_PRIMARY'")
})
public class InstanceMetaData implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancemetadata_generator")
    @SequenceGenerator(name = "instancemetadata_generator", sequenceName = "instancemetadata_id_seq", allocationSize = 1)
    private Long id;

    private Long privateId;

    private String privateIp;

    private String publicIp;

    private Integer sshPort;

    private String instanceId;

    private Boolean ambariServer;

    private Boolean consulServer;

    private String discoveryFQDN;

    @Type(type = "encrypted_string")
    @Column(columnDefinition = "TEXT")
    private String serverCert;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstanceStatus instanceStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InstanceMetadataType instanceMetadataType;

    private String localityIndicator;

    @ManyToOne
    private InstanceGroup instanceGroup;

    private Long startDate;

    private Long terminationDate;

    private String subnetId;

    public InstanceMetaData() {
    }

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public String getInstanceGroupName() {
        return instanceGroup.getGroupName();
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public void setPrivateId(Long privateId) {
        this.privateId = privateId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Boolean getAmbariServer() {
        return ambariServer;
    }

    public void setAmbariServer(Boolean ambariServer) {
        this.ambariServer = ambariServer;
    }

    public String getDiscoveryFQDN() {
        return discoveryFQDN;
    }

    public void setDiscoveryFQDN(String discoveryFQDN) {
        this.discoveryFQDN = discoveryFQDN;
    }

    public String getDiscoveryName() {
        if (discoveryFQDN == null) {
            return null;
        }
        return discoveryFQDN.split("\\.")[0];
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(Long terminationDate) {
        this.terminationDate = terminationDate;
    }

    public Boolean getConsulServer() {
        return consulServer;
    }

    public void setConsulServer(Boolean consulServer) {
        this.consulServer = consulServer;
    }

    public boolean isCreated() {
        return InstanceStatus.CREATED.equals(instanceStatus);
    }

    public boolean isFailed() {
        return InstanceStatus.FAILED.equals(instanceStatus);
    }

    public boolean isDecommissioned() {
        return InstanceStatus.DECOMMISSIONED.equals(instanceStatus);
    }

    public boolean isUnRegistered() {
        return InstanceStatus.UNREGISTERED.equals(instanceStatus);
    }

    public boolean isTerminated() {
        return InstanceStatus.TERMINATED.equals(instanceStatus);
    }

    public boolean isRegistered() {
        return InstanceStatus.REGISTERED.equals(instanceStatus);
    }

    public boolean isRunning() {
        return InstanceStatus.REGISTERED.equals(instanceStatus) || InstanceStatus.UNREGISTERED.equals(instanceStatus);
    }

    public String getLocalityIndicator() {
        return localityIndicator;
    }

    public void setLocalityIndicator(String localityIndicator) {
        this.localityIndicator = localityIndicator;
    }

    public String getPublicIpWrapper() {
        if (publicIp == null) {
            return privateIp;
        }
        return publicIp;
    }

    public InstanceMetadataType getInstanceMetadataType() {
        return instanceMetadataType;
    }

    public void setInstanceMetadataType(InstanceMetadataType instanceMetadataType) {
        this.instanceMetadataType = instanceMetadataType;
    }

    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }
}