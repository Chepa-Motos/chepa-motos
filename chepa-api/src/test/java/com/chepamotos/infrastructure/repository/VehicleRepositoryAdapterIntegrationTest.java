package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VehicleRepositoryAdapter.class)
class VehicleRepositoryAdapterIntegrationTest {

    @Autowired
    private VehicleRepositoryAdapter vehicleRepositoryAdapter;

    @Test
    void save_newVehicle_persistsAndReturnsGeneratedId() {
        Vehicle toSave = Vehicle.createNew("  tst123a  ", "  Boxer 150 2021  ");

        Vehicle saved = vehicleRepositoryAdapter.save(toSave);

        assertNotNull(saved.id());
        assertEquals("TST123A", saved.plate());
        assertEquals("Boxer 150 2021", saved.model());
    }

    @Test
    void findById_returnsSavedVehicle() {
        Vehicle saved = vehicleRepositoryAdapter.save(Vehicle.createNew("TST123B", "Yamaha FZ 2022"));

        var result = vehicleRepositoryAdapter.findById(saved.id());

        assertTrue(result.isPresent());
        assertEquals(saved.id(), result.get().id());
        assertEquals("TST123B", result.get().plate());
        assertEquals("Yamaha FZ 2022", result.get().model());
    }

    @Test
    void findByPlate_matchesExactNormalizedPlate() {
        vehicleRepositoryAdapter.save(Vehicle.createNew("TST123C", "Honda CB 2020"));

        var exact = vehicleRepositoryAdapter.findByPlate("TST123C");
        var lowercase = vehicleRepositoryAdapter.findByPlate("tst123c");
        var spaced = vehicleRepositoryAdapter.findByPlate("  TST123C  ");

        assertTrue(exact.isPresent());
        assertTrue(lowercase.isEmpty());
        assertTrue(spaced.isEmpty());
    }
}
