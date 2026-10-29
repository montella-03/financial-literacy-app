package com.financetracker.repository;

import com.financetracker.model.NetWorthItem;
import com.financetracker.model.NetWorthItem.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface NetWorthItemRepository extends JpaRepository<NetWorthItem, Long> {
    List<NetWorthItem> findByType(ItemType type);

    @Query("SELECT COALESCE(SUM(n.value), 0) FROM NetWorthItem n WHERE n.type = :type")
    BigDecimal sumByType(ItemType type);
}
