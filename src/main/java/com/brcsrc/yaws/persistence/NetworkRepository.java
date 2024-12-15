package com.brcsrc.yaws.persistence;


import com.brcsrc.yaws.model.Network;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NetworkRepository extends JpaRepository<Network, String> {

    Optional<Network> findByNetworkName(String networkName);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Network n " +
            "WHERE (n.networkName = :networkName AND n.networkName IS NOT NULL) " +
            "OR (n.networkCidr = :networkCidr AND n.networkCidr IS NOT NULL) " +
            "OR (n.networkListenPort = :networkListenPort AND n.networkListenPort IS NOT NULL)")
    boolean existsByNetworkNameOrNetworkCidrOrListenPort(@Param("networkName") String networkName,
                                                         @Param("networkCidr") String networkCidr,
                                                         @Param("networkListenPort") String networkListenPort);

}
