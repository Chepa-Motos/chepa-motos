package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.infrastructure.mapper.InvoiceEntityMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
	public BigDecimal sumActiveServiceLaborByMechanicAndDate(Long mechanicId, LocalDate date) {
		return springDataInvoiceRepository.sumActiveServiceLaborByMechanicAndDate(InvoiceType.SERVICE, mechanicId, date);
	}

	@Override
	public int countActiveServiceInvoicesByMechanicAndDate(Long mechanicId, LocalDate date) {
		return Math.toIntExact(springDataInvoiceRepository.countActiveServiceInvoicesByMechanicAndDate(InvoiceType.SERVICE, mechanicId, date));
	}

	@Override
	public List<Long> findActiveMechanicIdsWithActiveServiceInvoicesByDate(LocalDate date) {
		return springDataInvoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(InvoiceType.SERVICE, date);
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
