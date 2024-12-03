package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
@AutoConfigureTestDatabase(replace = Replace.NONE)
// @TestPropertySource("/application-test.properties")
class ClinicServiceTests {

    @Autowired
    protected OwnerRepository owners;

    @Autowired
    protected VetRepository vets;

    Pageable pageable = PageRequest.of(0, 10); // Initialize pageable with default values

    @Test
    void shouldFindOwnerByLastName() {
        Page<Owner> owners = this.owners.findByLastName("Davis", pageable);
        assertThat(owners.getContent()).hasSize(2);

        owners = this.owners.findByLastName("UnknownName", pageable);
        assertThat(owners.getContent()).isEmpty();
    }

    @Test
    void shouldFindSingleOwnerWithPet() {
        Owner owner = this.owners.findById(1);
        assertThat(owner.getLastName()).startsWith("Franklin");
        assertThat(owner.getPets()).hasSize(1);
        assertThat(owner.getPets().get(0).getType().getName()).isEqualTo("cat");
    }

    @Test
    @Transactional
    void shouldInsertOwner() {
        Page<Owner> ownersBefore = this.owners.findByLastName("Schultz", pageable);
        int countBefore = (int) ownersBefore.getTotalElements();

        Owner owner = new Owner();
        owner.setFirstName("Sam");
        owner.setLastName("Schultz");
        owner.setAddress("4, Evans Street");
        owner.setCity("Wollongong");
        owner.setTelephone("4444444444");
        this.owners.save(owner);

        assertThat(owner.getId()).isNotZero();

        Page<Owner> ownersAfter = this.owners.findByLastName("Schultz", pageable);
        assertThat(ownersAfter.getTotalElements()).isEqualTo(countBefore + 1);
    }

    @Test
    @Transactional
    void shouldUpdateOwnerLastName() {
        Owner owner = this.owners.findById(1);
        String oldLastName = owner.getLastName();

        owner.setLastName(oldLastName + "X");
        this.owners.save(owner);

        Owner updatedOwner = this.owners.findById(1);
        assertThat(updatedOwner.getLastName()).isEqualTo(oldLastName + "X");
    }

    @Test
    void shouldFindAllPetTypes() {
        Collection<PetType> petTypes = this.owners.findPetTypes();

        assertThat(petTypes).isNotEmpty();
        assertThat(petTypes).extracting(PetType::getName).contains("cat", "dog", "snake");
    }

    @Test
    @Transactional
    void shouldInsertPetForOwner() {
        Owner owner = this.owners.findById(6);
        int initialPetCount = owner.getPets().size();

        Pet pet = new Pet();
        pet.setName("Bowser");
        pet.setType(EntityUtils.getById(this.owners.findPetTypes(), PetType.class, 2)); // Assuming type id 2 exists
        pet.setBirthDate(LocalDate.now());
        owner.addPet(pet);

        this.owners.save(owner);

        Owner updatedOwner = this.owners.findById(6);
        assertThat(updatedOwner.getPets()).hasSize(initialPetCount + 1);
        assertThat(updatedOwner.getPets().stream().anyMatch(p -> p.getName().equals("Bowser"))).isTrue();
    }

    @Test
    void shouldFindAllVets() {
        Collection<Vet> vets = this.vets.findAll();

        assertThat(vets).isNotEmpty();
        Vet vet = EntityUtils.getById(vets, Vet.class, 3);
        assertThat(vet.getLastName()).isEqualTo("Douglas");
        assertThat(vet.getSpecialties().stream().map(s -> s.getName())).containsExactlyInAnyOrder("dentistry", "surgery");
    }

    @Test
    @Transactional
    void shouldAddNewVisitForPet() {
        Owner owner = this.owners.findById(6);
        Pet pet = owner.getPet(7);

        int initialVisitCount = pet.getVisits().size();

        Visit visit = new Visit();
        visit.setDescription("Routine Checkup");
        owner.addVisit(pet.getId(), visit);

        this.owners.save(owner);

        Owner updatedOwner = this.owners.findById(6);
        Pet updatedPet = updatedOwner.getPet(7);

        assertThat(updatedPet.getVisits()).hasSize(initialVisitCount + 1);
        assertThat(updatedPet.getVisits()).extracting(Visit::getDescription).contains("Routine Checkup");
    }
}
