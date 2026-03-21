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
        mechanicRepositoryAdapter.save(Mechanic.createNew("Jose"));
        Mechanic inactive = mechanicRepositoryAdapter.save(Mechanic.createNew("Andres"));
        mechanicRepositoryAdapter.save(inactive.withStatus(false));

        List<Mechanic> activeMechanics = mechanicRepositoryAdapter.findAllByActive(true);
        List<Mechanic> inactiveMechanics = mechanicRepositoryAdapter.findAllByActive(false);

        assertEquals(1, activeMechanics.size());
        assertEquals("Jose", activeMechanics.getFirst().name());

        assertEquals(1, inactiveMechanics.size());
        assertEquals("Andres", inactiveMechanics.getFirst().name());
        assertFalse(inactiveMechanics.getFirst().active());
    }

    @Test
    void findAll_returnsAllSavedMechanics() {
        mechanicRepositoryAdapter.save(Mechanic.createNew("Jose"));
        mechanicRepositoryAdapter.save(Mechanic.createNew("Carlos"));

        List<Mechanic> all = mechanicRepositoryAdapter.findAll();

        assertEquals(2, all.size());
    }
}
