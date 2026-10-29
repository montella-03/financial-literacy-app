package com.financetracker.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financetracker.model.NetWorthItem;
import com.financetracker.model.NetWorthItem.ItemType;
import com.financetracker.repository.NetWorthItemRepository;

@Service
public class NetWorthService {

	private final NetWorthItemRepository repository;

	public NetWorthService(NetWorthItemRepository repository) {
		this.repository = repository;
	}

	public List<NetWorthItem> findAll() {
		return repository.findAll();
	}

	public BigDecimal netWorth() {
		BigDecimal assets = sum(ItemType.ASSET);
		BigDecimal liabilities = sum(ItemType.LIABILITY);
		return assets.subtract(liabilities);
	}

	public BigDecimal sum(ItemType type) {
		return repository.findAll().stream()
				.filter(item -> item.getType() == type)
				.map(NetWorthItem::getValue)
				.filter(value -> value != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Transactional
	public NetWorthItem save(NetWorthItem item) {
		item.setUpdatedAt(LocalDateTime.now());
		return repository.save(item);
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}
}
