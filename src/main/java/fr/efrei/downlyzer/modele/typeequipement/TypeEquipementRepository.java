package fr.efrei.downlyzer.modele.typeequipement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TypeEquipementRepository extends JpaRepository<TypeEquipement, Long> {
    List<TypeEquipement> findAllByOrderByLibelleAsc();
}
