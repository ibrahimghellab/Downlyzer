package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.modele.SourceIncident;
import fr.efrei.downlyzer.exception.IncidentDejaOuvertException;
import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.modele.EtatEquipement;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.prestataire.PrestataireRepository;
import fr.efrei.downlyzer.modele.typeequipement.TypeEquipement;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

// sans base : repositories simulés et horloge figée
class IncidentServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 20, 12, 0);

    private IncidentRepository incidentRepository;
    private EquipementRepository equipementRepository;
    private UtilisateurRepository utilisateurRepository;
    private PrestataireRepository prestataireRepository;
    private IncidentService incidentService;
    private Equipement equipement;

    @BeforeEach
    void preparer() {
        incidentRepository = mock(IncidentRepository.class);
        equipementRepository = mock(EquipementRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        prestataireRepository = mock(PrestataireRepository.class);
        Clock horloge = Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        incidentService = new IncidentService(incidentRepository, equipementRepository, utilisateurRepository,
                prestataireRepository, horloge);

        equipement = new Equipement();
        equipement.setTypeEquipement(new TypeEquipement());
        equipement.setEtat(EtatEquipement.HORS_SERVICE);
    }

    private Incident incidentEnregistre(Incident incident) {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        return incident;
    }

    private Incident incidentNonAffecte() {
        return Incident.declarerManuellement(equipement, new Utilisateur(),
                "Ascenseur bloqué entre deux étages", MAINTENANT.minusHours(47));
    }

    private Incident incidentAffecte() {
        Incident incident = incidentNonAffecte();
        incident.affecter(new Prestataire(), MAINTENANT.minusHours(2));
        return incident;
    }

    private Incident incidentResolu() {
        Incident incident = incidentAffecte();
        incident.resoudre(MAINTENANT.minusHours(1), "Câble remplacé");
        return incident;
    }

    private Prestataire prestataireEnregistre(long id, String nom) {
        Prestataire prestataire = new Prestataire();
        prestataire.setNom(nom);
        when(prestataireRepository.findById(id)).thenReturn(Optional.of(prestataire));
        return prestataire;
    }

    private void prestatairesSpecialises(Prestataire... prestataires) {
        when(prestataireRepository.trouverActifsSpecialisesEn(any())).thenReturn(List.of(prestataires));
    }

    private void autreIncidentEnCoursSurLEquipement(boolean enCours) {
        when(incidentRepository.existsByEquipementIdAndIdNotAndStatutNot(any(), eq(1L), eq(StatutIncident.RESOLU)))
                .thenReturn(enCours);
    }

    @Test
    void declarerUnIncidentMetLEquipementHorsService() {
        equipement.setEtat(EtatEquipement.EN_SERVICE);
        when(equipementRepository.findById(1L)).thenReturn(Optional.of(equipement));
        when(utilisateurRepository.findByLogin("karim.dupont")).thenReturn(Optional.of(new Utilisateur()));
        when(incidentRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));

        Incident incident = incidentService.declarerIncident(1L, "Ascenseur bloqué entre deux étages", "karim.dupont");

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
        assertThat(incident.getDateOuverture()).isEqualTo(MAINTENANT);
        assertThat(equipement.getEtat()).isEqualTo(EtatEquipement.HORS_SERVICE);
    }

    @Test
    void unCapteurDeclareUnIncidentSansDeclarantEtMetLEquipementHorsService() {
        equipement.setEtat(EtatEquipement.EN_SERVICE);
        when(equipementRepository.findByReference("ASC-004")).thenReturn(Optional.of(equipement));
        when(incidentRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));

        Incident incident = incidentService.declarerParCapteur("ASC-004", "Porte bloquée en position ouverte");

        assertThat(incident.getSource()).isEqualTo(SourceIncident.CAPTEUR);
        assertThat(incident.getDeclarant()).isNull();
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
        assertThat(incident.getDateOuverture()).isEqualTo(MAINTENANT);
        assertThat(equipement.getEtat()).isEqualTo(EtatEquipement.HORS_SERVICE);
    }

    @Test
    void unCapteurQuiDesigneUneReferenceInconnueEstSignale() {
        when(equipementRepository.findByReference("XXX-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.declarerParCapteur("XXX-999", "Porte bloquée en position ouverte"))
                .isInstanceOf(EquipementIntrouvableException.class)
                .hasMessageContaining("XXX-999");
    }

    @Test
    void unCapteurNeCreePasDeDoublonQuandLEquipementADejaUnIncidentEnCours() {
        Incident existant = incidentNonAffecte();
        ReflectionTestUtils.setField(existant, "id", 9L);
        when(equipementRepository.findByReference("ASC-001")).thenReturn(Optional.of(equipement));
        when(incidentRepository.findFirstByEquipementIdAndStatutNot(any(), eq(StatutIncident.RESOLU)))
                .thenReturn(Optional.of(existant));

        assertThatThrownBy(() -> incidentService.declarerParCapteur("ASC-001", "Porte bloquée en position ouverte"))
                .isInstanceOf(IncidentDejaOuvertException.class)
                .extracting(erreur -> ((IncidentDejaOuvertException) erreur).getIncidentExistantId()).isEqualTo(9L);
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void lApiListeTousLesIncidentsOuCeuxDUnStatut() {
        List<Incident> tous = List.of(incidentNonAffecte(), incidentResolu());
        List<Incident> resolus = List.of(incidentResolu());
        when(incidentRepository.findAllByOrderByDateOuvertureDesc()).thenReturn(tous);
        when(incidentRepository.findByStatutOrderByDateOuvertureDesc(StatutIncident.RESOLU)).thenReturn(resolus);

        assertThat(incidentService.listerIncidents(null)).isSameAs(tous);
        assertThat(incidentService.listerIncidents(StatutIncident.RESOLU)).isSameAs(resolus);
    }

    @Test
    void unAgentPeutModifierLaDescriptionDUnIncidentNonAffecte() {
        Incident incident = incidentEnregistre(incidentNonAffecte());

        incidentService.modifierDescription(1L, "Ascenseur bloqué au 2e étage", Profil.AGENT);

        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué au 2e étage");
    }

    @Test
    void unAgentNePeutPlusModifierLaDescriptionDUnIncidentAffecte() {
        Incident incident = incidentEnregistre(incidentAffecte());

        assertThat(incidentService.peutModifierDescription(incident, Profil.AGENT)).isFalse();
        assertThatThrownBy(() -> incidentService.modifierDescription(1L, "Nouvelle description", Profil.AGENT))
                .isInstanceOf(ActionNonAutoriseeException.class);
        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué entre deux étages");
    }

    @Test
    void unResponsablePeutModifierLaDescriptionDUnIncidentAffecte() {
        Incident incident = incidentEnregistre(incidentAffecte());

        incidentService.modifierDescription(1L, "Ascenseur bloqué au 2e étage", Profil.RESPONSABLE);

        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué au 2e étage");
    }

    @Test
    void personneNePeutModifierLaDescriptionDUnIncidentResolu() {
        Incident incident = incidentEnregistre(incidentResolu());

        for (Profil profil : Profil.values()) {
            assertThat(incidentService.peutModifierDescription(incident, profil)).isFalse();
            assertThatThrownBy(() -> incidentService.modifierDescription(1L, "Nouvelle description", profil))
                    .isInstanceOf(TransitionInterditeException.class);
        }
        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué entre deux étages");
    }

    @Test
    void unIncidentInexistantEstSignale() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.trouverIncident(999L))
                .isInstanceOf(IncidentIntrouvableException.class)
                .hasMessage("L'incident n° 999 n'existe pas.");
    }

    @Test
    void lHistoriqueDemandeLesIncidentsResolusDuPlusRecentAuPlusAncien() {
        List<Incident> resolus = List.of(incidentResolu());
        when(incidentRepository.findByStatutOrderByDateResolutionDesc(StatutIncident.RESOLU)).thenReturn(resolus);

        assertThat(incidentService.listerIncidentsResolus()).isSameAs(resolus);
    }

    @Test
    void laDureeDOuvertureDUnIncidentEnCoursVaJusquAMaintenant() {
        assertThat(incidentService.calculerDureeOuverture(incidentNonAffecte())).isEqualTo(Duration.ofHours(47));
    }

    @Test
    void laDureeDOuvertureDUnIncidentResoluSArreteASaResolution() {
        assertThat(incidentService.calculerDureeOuverture(incidentResolu())).isEqualTo(Duration.ofHours(46));
    }

    @Test
    void seulUnResponsablePeutAffecterEtResoudreUnIncidentNonResolu() {
        assertThat(incidentService.peutAffecterEtResoudre(incidentNonAffecte(), Profil.RESPONSABLE)).isTrue();
        assertThat(incidentService.peutAffecterEtResoudre(incidentNonAffecte(), Profil.AGENT)).isFalse();
        assertThat(incidentService.peutAffecterEtResoudre(incidentResolu(), Profil.RESPONSABLE)).isFalse();
    }

    @Test
    void affecterUnPrestataireSpecialiseRendLIncidentAffecte() {
        Incident incident = incidentEnregistre(incidentNonAffecte());
        Prestataire ascenso = prestataireEnregistre(5L, "Ascenso Maintenance");
        prestatairesSpecialises(ascenso);

        incidentService.affecterPrestataire(1L, 5L);

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.AFFECTE);
        assertThat(incident.getPrestataire()).isSameAs(ascenso);
        assertThat(incident.getDateAffectation()).isEqualTo(MAINTENANT);
    }

    @Test
    void reaffecterRemplaceLePrestataireEtNeProposePlusLAncien() {
        Incident incident = incidentEnregistre(incidentNonAffecte());
        Prestataire ascenso = prestataireEnregistre(5L, "Ascenso Maintenance");
        Prestataire rapid = prestataireEnregistre(6L, "Rapid'Intervention");
        prestatairesSpecialises(ascenso, rapid);
        incidentService.affecterPrestataire(1L, 5L);

        assertThat(incidentService.listerPrestatairesAffectables(incident)).containsExactly(rapid);
        incidentService.affecterPrestataire(1L, 6L);

        assertThat(incident.getPrestataire()).isSameAs(rapid);
    }

    @Test
    void unPrestataireNonProposeNePeutPasEtreAffecte() {
        Incident incident = incidentEnregistre(incidentNonAffecte());
        prestataireEnregistre(5L, "InfoAffichage Pro");
        prestatairesSpecialises();

        assertThatThrownBy(() -> incidentService.affecterPrestataire(1L, 5L))
                .isInstanceOf(DonneeInvalideException.class);
        assertThat(incident.getStatut()).isEqualTo(StatutIncident.NON_AFFECTE);
    }

    @Test
    void unIncidentResoluNePeutPlusEtreAffecte() {
        incidentEnregistre(incidentResolu());
        prestataireEnregistre(5L, "Ascenso Maintenance");

        assertThatThrownBy(() -> incidentService.affecterPrestataire(1L, 5L))
                .isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void resoudreEnregistreLaDateEtRemetLEquipementEnServiceSIlNAPlusDIncidentEnCours() {
        Incident incident = incidentEnregistre(incidentAffecte());
        autreIncidentEnCoursSurLEquipement(false);

        incidentService.resoudreIncident(1L, "Câble de traction remplacé");

        assertThat(incident.getStatut()).isEqualTo(StatutIncident.RESOLU);
        assertThat(incident.getDateResolution()).isEqualTo(MAINTENANT);
        assertThat(incident.getCommentaireResolution()).isEqualTo("Câble de traction remplacé");
        assertThat(equipement.getEtat()).isEqualTo(EtatEquipement.EN_SERVICE);
    }

    @Test
    void lEquipementResteHorsServiceSiUnAutreDeSesIncidentsEstEnCours() {
        incidentEnregistre(incidentNonAffecte());
        autreIncidentEnCoursSurLEquipement(true);

        incidentService.resoudreIncident(1L, null);

        assertThat(equipement.getEtat()).isEqualTo(EtatEquipement.HORS_SERVICE);
    }

    @Test
    void unCommentaireDeResolutionVideEstEnregistreCommeAbsent() {
        Incident incident = incidentEnregistre(incidentNonAffecte());

        incidentService.resoudreIncident(1L, "   ");

        assertThat(incident.getCommentaireResolution()).isNull();
    }

    @Test
    void unIncidentDejaResoluNePeutPasEtreResoluUneSecondeFois() {
        incidentEnregistre(incidentResolu());

        assertThatThrownBy(() -> incidentService.resoudreIncident(1L, null))
                .isInstanceOf(TransitionInterditeException.class);
    }
}
