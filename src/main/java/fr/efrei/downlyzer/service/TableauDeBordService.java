package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.EtatEquipement;
import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.typeequipement.TypeEquipement;
import fr.efrei.downlyzer.modele.typeequipement.TypeEquipementRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// F11 : tableau de bord du responsable d'exploitation
@Service
public class TableauDeBordService {

    // Période du délai moyen de résolution : assez longue pour avoir des résolutions, assez courte pour rester actuelle
    static final Duration PERIODE_DELAI_MOYEN = Duration.ofDays(30);

    private static final Comparator<LigneTableauDeBord> PAR_URGENCE =
            Comparator.comparing(LigneTableauDeBord::niveau)
                    .thenComparing(LigneTableauDeBord::dureeOuverture, Comparator.reverseOrder());

    private static final Map<OrdreTableauDeBord, Comparator<LigneTableauDeBord>> ORDRES = Map.of(
            OrdreTableauDeBord.URGENCE, PAR_URGENCE,
            OrdreTableauDeBord.STATION, Comparator.<LigneTableauDeBord, String>comparing(
                    ligne -> ligne.incident().getEquipement().getStation().getNom()).thenComparing(PAR_URGENCE),
            OrdreTableauDeBord.PLUS_RECENTS, Comparator.comparing(LigneTableauDeBord::dureeOuverture));

    private final IncidentRepository incidentRepository;
    private final EquipementRepository equipementRepository;
    private final TypeEquipementRepository typeEquipementRepository;
    private final PolitiqueUrgence politiqueUrgence;
    private final Clock horloge;

    public TableauDeBordService(IncidentRepository incidentRepository, EquipementRepository equipementRepository,
                                TypeEquipementRepository typeEquipementRepository, PolitiqueUrgence politiqueUrgence,
                                Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementRepository = equipementRepository;
        this.typeEquipementRepository = typeEquipementRepository;
        this.politiqueUrgence = politiqueUrgence;
        this.horloge = horloge;
    }

    // Les filtres station et type sont appliqués par la base ; le niveau, calculé à partir de l'heure, ne peut l'être qu'ici.
    // Un filtre ou un ordre null n'est pas appliqué (ordre par défaut : urgence)
    public List<LigneTableauDeBord> listerIncidentsEnCours(Long stationId, Long typeEquipementId,
                                                           NiveauAttention niveau, OrdreTableauDeBord ordre) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        return incidentRepository.trouverEnCours(StatutIncident.RESOLU, stationId, typeEquipementId).stream()
                .map(incident -> new LigneTableauDeBord(incident, politiqueUrgence.evaluer(incident, maintenant),
                        incident.dureeOuverture(maintenant)))
                .filter(ligne -> niveau == null || ligne.niveau() == niveau)
                .sorted(ORDRES.get(ordre == null ? OrdreTableauDeBord.URGENCE : ordre))
                .toList();
    }

    // Vue en colonnes : une colonne par niveau, du plus urgent au moins urgent, toujours présente même vide ;
    // l'ordre des lignes à l'intérieur d'une colonne est celui de la liste reçue
    public Map<NiveauAttention, List<LigneTableauDeBord>> regrouperParNiveau(List<LigneTableauDeBord> lignes) {
        Map<NiveauAttention, List<LigneTableauDeBord>> colonnes = new EnumMap<>(NiveauAttention.class);
        for (NiveauAttention niveau : NiveauAttention.values()) {
            colonnes.put(niveau, lignes.stream().filter(ligne -> ligne.niveau() == niveau).toList());
        }
        return colonnes;
    }

    // Calculés sur tout le réseau, indépendamment des filtres : ils résument la situation générale
    public IndicateursTableauDeBord calculerIndicateurs() {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        List<LigneTableauDeBord> enCours = listerIncidentsEnCours(null, null, null, OrdreTableauDeBord.URGENCE);
        List<PartNiveau> repartition = Arrays.stream(NiveauAttention.values())
                .map(niveau -> {
                    long nombre = compter(enCours, niveau);
                    return new PartNiveau(niveau, nombre, enCours.isEmpty() ? 0 : 100.0 * nombre / enCours.size());
                })
                .toList();
        Double delaiEnMinutes = incidentRepository.calculerDelaiMoyenDeResolutionEnMinutes(
                maintenant.minus(PERIODE_DELAI_MOYEN));
        return new IndicateursTableauDeBord(maintenant, enCours.size(), repartition,
                equipementRepository.countByEtat(EtatEquipement.HORS_SERVICE),
                delaiEnMinutes == null ? null : Duration.ofMinutes(Math.round(delaiEnMinutes)));
    }

    public List<TypeEquipement> listerTypesEquipement() {
        return typeEquipementRepository.findAllByOrderByLibelleAsc();
    }

    private static long compter(List<LigneTableauDeBord> lignes, NiveauAttention niveau) {
        return lignes.stream().filter(ligne -> ligne.niveau() == niveau).count();
    }
}
