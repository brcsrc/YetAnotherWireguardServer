package com.brcsrc.yaws.persistence;

import com.brcsrc.yaws.model.NetworkClient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkClientRepository extends JpaRepository<NetworkClient, Long> {}
