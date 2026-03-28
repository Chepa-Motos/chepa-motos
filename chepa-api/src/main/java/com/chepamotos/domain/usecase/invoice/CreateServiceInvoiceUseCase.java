package com.chepamotos.domain.usecase.invoice;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;

import java.math.BigDecimal;
import java.util.List;

public class CreateServiceInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final MechanicRepository mechanicRepository;
    private final ResolveVehicleForServiceInvoiceUseCase resolveVehicleUseCase;

    public CreateServiceInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            MechanicRepository mechanicRepository,
            ResolveVehicleForServiceInvoiceUseCase resolveVehicleUseCase) {
        this.invoiceRepository = invoiceRepository;
        this.mechanicRepository = mechanicRepository;
        this.resolveVehicleUseCase = resolveVehicleUseCase;
    }

    public Invoice execute(
            Long mechanicId,
            String vehiclePlate,
            String vehicleModel,
            BigDecimal laborAmount,
            List<InvoiceItemInput> itemInputs) {
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new MechanicNotFoundException(mechanicId));

        Vehicle vehicle = resolveVehicleUseCase.execute(vehiclePlate, vehicleModel);

        List<InvoiceItem> items = itemInputs.stream()
                .map(i -> InvoiceItem.createNew(i.description(), i.quantity(), i.unitPrice()))
                .toList();

        return invoiceRepository.save(Invoice.createService(mechanic, vehicle, laborAmount, items));
    }
}