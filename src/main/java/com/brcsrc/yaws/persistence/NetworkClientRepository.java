package com.brcsrc.yaws.persistence;

import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NetworkClientRepository extends JpaRepository<NetworkClient, Long> {

    @Query("SELECT nc.client FROM NetworkClient nc WHERE nc.network.networkName = :networkName")
    List<Client> findClientsByNetworkName(@Param("networkName") String networkName);

    /**
     * used for determining if the network already has a client for
     * an address which would make it unavailable
     * @param networkName String
     * @param clientCidr String
     * @return boolean
     */
    @Query("SELECT COUNT(nc) > 0 FROM NetworkClient nc " +
            "WHERE nc.network.networkName = :networkName " +
            "AND nc.client.clientCidr = :clientCidr")
    boolean existsByNetworkNameAndClientCidr(
            @Param("networkName") String networkName,
            @Param("clientCidr") String clientCidr);

    // TODO use this in place of existsByNetworkNameAndClientCidr
    NetworkClient findNetworkClientByNetwork_NetworkNameAndClient_ClientCidr(String networkName, String clientCidr);

    /**
     * used for determining if a network has a client by that name
     * @param networkName String
     * @param clientName String
     * @return boolean
     */
    @Query("SELECT COUNT(nc) > 0 FROM NetworkClient nc " +
            "WHERE nc.network.networkName = :networkName " +
            "AND nc.client.clientName = :clientName")
    boolean existsByClientNameAndNetworkName(@Param("networkName") String networkName,
                                             @Param("clientName") String clientName);

    NetworkClient findNetworkClientByNetwork_NetworkNameAndClient_ClientName(String networkName, String clientName);

    int deleteNetworkClientByNetwork_NetworkNameAndClient_ClientName(String networkName, String clientName);
}
