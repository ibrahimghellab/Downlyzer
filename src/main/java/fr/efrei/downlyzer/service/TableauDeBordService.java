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
import java.util.Set;

@Service
public class TableauDeBordService {

    // assez long pour avoir des résolutions, assez court pour rester d'actualité
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
    private final EquipementService equipementService;
    private final Clock horloge;

    public TableauDeBordService(IncidentRepository incidentRepository, EquipementRepository equipementRepository,
                                TypeEquipementRepository typeEquipementRepository, PolitiqueUrgence politiqueUrgence,
                                EquipementService equipementService, Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementRepository = equipementRepository;
        this.typeEquipementRepository = typeEquipementRepository;
        this.politiqueUrgence = politiqueUrgence;
        this.equipementService = equipementService;
        this.horloge = horloge;
    }

    // station et type filtrés par la base, le niveau ici car il dépend de l'heure
    public List<LigneTableauDeBord> listerIncidentsEnCours(Long stationId, Long typeEquipementId,
                                                           NiveauAttention niveau, OrdreTableauDeBord ordre) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Set<Long> equipementsFragiles = equipementService.listerEquipementsFragiles();
        return incidentRepository.trouverEnCours(StatutIncident.RESOLU, stationId, typeEquipementId).stream()
                .map(incident -> new LigneTableauDeBord(incident, politiqueUrgence.evaluer(incident, maintenant),
                        incident.dureeOuverture(maintenant),
                        equipementsFragiles.contains(incident.getEquipement().getId())))
                .filter(ligne -> niveau == null || ligne.niveau() == niveau)
                .sorted(ORDRES.get(ordre == null ? OrdreTableauDeBord.URGENCE : ordre))
                .toList();
    }

    // une colonne par niveau, même vide, en gardant l'ordre de tri
    public Map<NiveauAttention, List<LigneTableauDeBord>> regrouperParNiveau(List<LigneTableauDeBord> lignes) {
        Map<NiveauAttention, List<LigneTableauDeBord>> colonnes = new EnumMap<>(NiveauAttention.class);
        for (NiveauAttention niveau : NiveauAttention.values()) {
            colonnes.put(niveau, lignes.stream().filter(ligne -> ligne.niveau() == niveau).toList());
        }
        return colonnes;
    }

    // calculés sur tout le réseau, sans tenir compte des filtres
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
