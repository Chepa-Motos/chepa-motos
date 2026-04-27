package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.infrastructure.mapper.InvoiceEntityMapper;
import com.chepamotos.infrastructure.mapper.InvoiceItemEntityMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	public List<Invoice> findAllByFilters(LocalDate date, InvoiceType type, Long mechanicId, boolean cancelled) {
		boolean useDateFilter = date != null;
		boolean useTypeFilter = type != null;
		boolean useMechanicFilter = mechanicId != null;
		LocalDateTime startDateTime = date == null ? null : date.atStartOfDay();
		LocalDateTime endDateTime = date == null ? null : date.plusDays(1).atStartOfDay();

		return springDataInvoiceRepository.findAllByDateRangeAndFiltersWithDetails(
				useDateFilter,
				startDateTime,
				endDateTime,
				cancelled,
				useTypeFilter,
				type,
				useMechanicFilter,
				mechanicId)
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
		LocalDateTime startDateTime = date.atStartOfDay();
		LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
		return springDataInvoiceRepository.sumActiveServiceLaborByMechanicAndDate(
				InvoiceType.SERVICE,
				mechanicId,
				startDateTime,
				endDateTime);
	}

	@Override
	public int countActiveServiceInvoicesByMechanicAndDate(Long mechanicId, LocalDate date) {
		LocalDateTime startDateTime = date.atStartOfDay();
		LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
		return Math.toIntExact(springDataInvoiceRepository.countActiveServiceInvoicesByMechanicAndDate(
				InvoiceType.SERVICE,
				mechanicId,
				startDateTime,
				endDateTime));
	}

	@Override
	public List<Long> findActiveMechanicIdsWithActiveServiceInvoicesByDate(LocalDate date) {
		LocalDateTime startDateTime = date.atStartOfDay();
		LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
		return springDataInvoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(
				InvoiceType.SERVICE,
				startDateTime,
				endDateTime);
	}

	@Override
	public List<InvoiceItem> findSuggestionsByModelAndDescription(String model, String descriptionPrefix) {
		return springDataInvoiceRepository.findSuggestionsByModelAndDescription(InvoiceType.SERVICE.name(), model, descriptionPrefix)
				.stream()
				.map(InvoiceItemEntityMapper::toDomain)
				.toList();
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
