package com.brcsrc.yaws.persistence;

import com.brcsrc.yaws.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, String> {}
