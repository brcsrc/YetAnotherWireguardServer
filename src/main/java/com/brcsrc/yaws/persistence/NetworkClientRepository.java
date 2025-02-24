package com.brcsrc.yaws.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.NetworkClient;

public interface NetworkClientRepository extends JpaRepository<NetworkClient, Long> {

    @Query("SELECT nc.client FROM NetworkClient nc WHERE nc.network.networkName = :networkName")
    List<Client> findClientsByNetworkName(@Param("networkName") String networkName);

    @Query("SELECT COUNT(nc) > 0 FROM NetworkClient nc "
            + "WHERE nc.network.networkName = :networkName "
            + "AND nc.client.clientCidr = :clientCidr")
    boolean existsByNetworkNameAndClientCidr(
            @Param("networkName") String networkName,
            @Param("clientCidr") String clientCidr);

    @Query("SELECT nc FROM NetworkClient nc "
            + "WHERE nc.network.networkName = :networkName "
            + "AND nc.client.clientName = :clientName")
    NetworkClient findByNetworkClientByNetworkNameAndClientName(
            @Param("networkName") String networkName,
            @Param("clientName") String clientName);
}
