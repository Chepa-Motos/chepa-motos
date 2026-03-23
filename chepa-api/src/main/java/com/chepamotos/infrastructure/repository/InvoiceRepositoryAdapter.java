package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.infrastructure.mapper.InvoiceEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InvoiceRepositoryAdapter implements InvoiceRepository {

	private final SpringDataInvoiceRepository springDataInvoiceRepository;

	public InvoiceRepositoryAdapter(SpringDataInvoiceRepository springDataInvoiceRepository) {
		this.springDataInvoiceRepository = springDataInvoiceRepository;
	}

	@Override
	public List<Invoice> findAll() {
		return springDataInvoiceRepository.findAllWithDetails()
				.stream()
				.map(InvoiceEntityMapper::toDomain)
				.toList();
	}

	@Override
	public Optional<Invoice> findById(Long id) {
		return springDataInvoiceRepository.findByIdWithDetails(id)
				.map(InvoiceEntityMapper::toDomain);
	}

	@Override
	public Invoice save(Invoice invoice) {
		var entity = InvoiceEntityMapper.toEntity(invoice);
		springDataInvoiceRepository.save(entity);
		return springDataInvoiceRepository.findByIdWithDetails(entity.getId())
				.map(InvoiceEntityMapper::toDomain)
				.orElseThrow();
	}
}
