package com.financetracker.repository;

import com.financetracker.model.NetWorthItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NetWorthItemRepository extends JpaRepository<NetWorthItem, Long> {
    List<NetWorthItem> findByOwnerId(String ownerId);
}
