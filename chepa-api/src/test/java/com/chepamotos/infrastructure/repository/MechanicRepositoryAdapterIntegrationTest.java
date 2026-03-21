package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Mechanic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MechanicRepositoryAdapter.class)
class MechanicRepositoryAdapterIntegrationTest {

    @Autowired
    private MechanicRepositoryAdapter mechanicRepositoryAdapter;

    @Test
    void save_newMechanic_persistsAndReturnsGeneratedId() {
        Mechanic toSave = Mechanic.createNew("  Jose  ");

        Mechanic saved = mechanicRepositoryAdapter.save(toSave);

        assertNotNull(saved.id());
        assertEquals("Jose", saved.name());
        assertTrue(saved.active());
    }

    @Test
    void findById_returnsSavedMechanic() {
        Mechanic saved = mechanicRepositoryAdapter.save(Mechanic.createNew("Carlos"));

        var result = mechanicRepositoryAdapter.findById(saved.id());

        assertTrue(result.isPresent());
        assertEquals(saved.id(), result.get().id());
        assertEquals("Carlos", result.get().name());
        assertTrue(result.get().active());
    }

    @Test
    void findAllByActive_filtersByStatus() {
        int baselineActive = mechanicRepositoryAdapter.findAllByActive(true).size();
        int baselineInactive = mechanicRepositoryAdapter.findAllByActive(false).size();

        mechanicRepositoryAdapter.save(Mechanic.createNew("Jose_test_active"));
        Mechanic inactive = mechanicRepositoryAdapter.save(Mechanic.createNew("Andres_test_inactive"));
        mechanicRepositoryAdapter.save(inactive.withStatus(false));

        List<Mechanic> activeMechanics = mechanicRepositoryAdapter.findAllByActive(true);
        List<Mechanic> inactiveMechanics = mechanicRepositoryAdapter.findAllByActive(false);

        assertEquals(baselineActive + 1, activeMechanics.size());
        assertTrue(activeMechanics.stream().anyMatch(mechanic -> "Jose_test_active".equals(mechanic.name()) && mechanic.active()));

        assertEquals(baselineInactive + 1, inactiveMechanics.size());
        assertTrue(inactiveMechanics.stream().anyMatch(mechanic -> "Andres_test_inactive".equals(mechanic.name()) && !mechanic.active()));
    }

    @Test
    void findAll_returnsAllSavedMechanics() {
        int baselineTotal = mechanicRepositoryAdapter.findAll().size();

        mechanicRepositoryAdapter.save(Mechanic.createNew("Jose_test_total"));
        mechanicRepositoryAdapter.save(Mechanic.createNew("Carlos_test_total"));

        List<Mechanic> all = mechanicRepositoryAdapter.findAll();

        assertEquals(baselineTotal + 2, all.size());
    }
}
