# Journal — Downlyzer (ALTN72, 2026-27)

> Supervision d'équipements et suivi d'incidents sur un réseau de transport.

**Groupe :** Ibrahim GHELLAB, Prénom NOM

---

## README

### Prérequis
- Java 21
- Maven 3.9+ (ou le wrapper `./mvnw` fourni)
- MariaDB : développé et testé avec MariaDB 10.4 (XAMPP) ; version 10.6 ou plus recommandée (voir Limites connues)
- Spring Boot 4.1.1 (même pile que le TP fil rouge)

### Initialiser la base de données
1. Créer la base, son schéma et les données de démonstration (en `root`) :
   ```bash
   mariadb -u root -p < db/create.sql
   ```
   Le script supprime puis recrée toutes les tables : il peut être relancé autant de fois que nécessaire et remet la base dans son état de démonstration. Il peut aussi être importé depuis phpMyAdmin (onglet Importer).

2. Créer un compte dédié à l'application, limité aux données de cette base :
   ```sql
   CREATE USER 'downlyzer_app'@'localhost' IDENTIFIED BY 'un_mot_de_passe';
   GRANT SELECT, INSERT, UPDATE, DELETE ON downlyzer.* TO 'downlyzer_app'@'localhost';
   ```
   L'application n'a ni droit global, ni droit de modifier la structure des tables : seul le script, lancé en `root`, les crée.

Les dates des incidents sont calculées par rapport à l'instant d'exécution du script (`NOW() - INTERVAL 47 HOUR`, etc.) : **relancer le script juste avant une démonstration** pour que les cas limites (47 h, 48 h, 72 h) restent exacts.

### Configuration
Aucun secret n'est versionné. Variables d'environnement à définir avant le lancement (dans IntelliJ : *Run → Edit Configurations → Modify options → Environment variables*, sans cocher « Store as project file ») :

| Variable | Rôle | Obligatoire |
|---|---|---|
| `DB_USERNAME` | utilisateur MariaDB (ex. `downlyzer_app`) | oui |
| `DB_PASSWORD` | mot de passe de cet utilisateur | oui |
| `DB_URL` | URL JDBC ; par défaut `jdbc:mariadb://localhost:3306/downlyzer` | non |
| `DB_DIALECT` | dialecte Hibernate ; par défaut `org.hibernate.dialect.MariaDBDialect`, `org.hibernate.dialect.MySQLDialect` pour la base déployée sur MySQL | non |
| `DOWNLYZER_API_KEY` | clé exigée des systèmes externes sur l'API REST (en-tête `X-API-KEY`) ; une longue valeur aléatoire, par exemple obtenue avec `openssl rand -hex 24` | pour utiliser l'API |

Sans `DB_USERNAME` ou `DB_PASSWORD`, l'application refuse de démarrer (`Could not resolve placeholder`). Sans `DOWNLYZER_API_KEY`, elle démarre, mais l'API refuse toutes les requêtes (401) et un avertissement l'indique au démarrage.

### Lancer l'application
```bash
mvn spring-boot:run
```
Puis ouvrir http://localhost:8080. Documentation de l'API : http://localhost:8080/swagger-ui.html (bouton « Authorize » pour saisir la clé d'API avant d'essayer une requête).

### Essayer l'API et le simulateur de capteurs (piste 8)
- `requetes.http`, à la racine : toutes les requêtes de l'API (succès et erreurs), à lancer depuis IntelliJ. La clé est lue dans un fichier `http-client.private.env.json` non versionné (son contenu est indiqué en tête de `requetes.http`).
- Le simulateur envoie des signalements de panne à intervalles réguliers, comme des capteurs installés sur les équipements :
  ```bash
  java simulateur/SimulateurCapteurs.java 5
  ```
  (5 signalements ; la variable `DOWNLYZER_API_KEY` doit être définie dans le terminal). Les incidents créés apparaissent aussitôt dans le tableau de bord.

### Lancer les tests
```bash
mvn verify
```
Les tests unitaires et les tests des pages n'ont besoin ni de base ni de variables d'environnement. Seul `DownlyzerApplicationTests` (vérification que toute l'application démarre) se connecte à la base : il faut que MariaDB soit démarrée et que `DB_USERNAME` et `DB_PASSWORD` soient définies, comme pour lancer l'application.

Tous les tests, y compris `DownlyzerApplicationTests`, sont aussi lancés par GitHub Actions, sur MariaDB et sur MySQL, à chaque push sur `master` et sur chaque pull request (onglet *Actions* du dépôt, fichier `.github/workflows/tests.yml`).

### Comptes de test

| Profil | Login | Mot de passe |
|---|---|---|
| Responsable d'exploitation | `claire.martin` | `Supervision2026!` |
| Agent de station | `karim.dupont` | `Terrain2026!` |

Les mots de passe sont stockés hachés en BCrypt dans la base.

### Données de démonstration
- 6 stations, 5 types d'équipement, 16 équipements, 5 prestataires.
- Incidents non affectés couvrant tous les cas limites de RG3 : 2 h, 47 h, 47 h 59 (vert) ; 48 h pile, 60 h, 71 h 59 (orange) ; 72 h pile, 5 jours (rouge).
- 3 incidents affectés, dont un ouvert depuis 80 h (il ne doit pas apparaître en rouge, RG4).
- 9 incidents résolus sur les 4 dernières semaines, dont un résolu sans prestataire (non évaluable) et deux résolus pas encore évalués (pour démontrer F10).
- Plusieurs incidents déclarés par un capteur (`source = CAPTEUR`, sans déclarant).
- L'état des équipements est cohérent avec leurs incidents : un équipement qui a un incident en cours est hors service ou dégradé.
- Toutes les données sont fictives : noms de stations et de prestataires inventés, adresses e-mail en `.example` (domaine réservé à la documentation, qui ne peut appartenir à personne), coordonnées choisies dans Paris pour la carte (piste 4).

### Application déployée
- URL : **https://downlyzer.onrender.com** (documentation de l'API : https://downlyzer.onrender.com/swagger-ui.html)
- Comptes : les mêmes que ci-dessus (Comptes de test).
- Hébergement : l'application sur Render (construite à partir du `Dockerfile`), la base sur Aiven (MySQL 8).
- L'offre gratuite de Render met l'application en veille après 15 minutes sans visite : **le premier chargement peut prendre environ une minute**, les suivants sont immédiats.
- Pour charger ou remettre à zéro les données de démonstration sur la base distante, depuis la racine du projet (PowerShell) :
  ```powershell
  $env:DB_ADMIN_URL = "jdbc:mariadb://<hôte>:<port>/?sslMode=verify-full&serverSslCert=db/aiven-ca.pem"
  $env:DB_ADMIN_USERNAME = "avnadmin"
  $env:DB_ADMIN_PASSWORD = "<mot de passe avnadmin, console Aiven>"
  $env:DB_PASSWORD = "<mot de passe choisi pour downlyzer_app>"
  java -cp "$HOME\.m2\repository\org\mariadb\jdbc\mariadb-java-client\3.5.10\mariadb-java-client-3.5.10.jar" db/ChargementBase.java
  ```
  Le programme exécute `db/create.sql` dans le fuseau de Paris (heure d'hiver), le même que celui de l'application : sans cela, `NOW()` serait en UTC et tous les incidents seraient décalés d'une heure. Il crée ensuite le compte `downlyzer_app`, ou remet son mot de passe s'il existe déjà. Le jar du pilote est celui que Maven a téléchargé pour l'application (`./mvnw package` le télécharge s'il manque).

### Limites connues
- Le script de données utilise `NOW()` de MariaDB et l'application l'heure de la JVM : les deux doivent être dans le **même fuseau horaire**, sinon les seuils sont décalés (un incident de 47 h pourrait apparaître orange).
- MariaDB 10.4 (fournie par XAMPP) n'est plus officiellement prise en charge par Hibernate 7, qui affiche un avertissement au démarrage (minimum 10.6). L'application fonctionne, mais le serveur de déploiement doit utiliser une version récente.
- Bootstrap, Leaflet et le fond de carte OpenStreetMap sont chargés depuis Internet : sans connexion, les pages fonctionnent mais s'affichent sans mise en forme, et l'onglet Carte reste vide (l'onglet Liste donne les mêmes informations).
- L'état « Dégradé » d'un équipement n'est jamais attribué automatiquement (voir CHOIX TECHNIQUES, F7-F8) : il n'existe que dans les données de démonstration.
- Les équipements d'une station (F2) ne sont pas triés : la base les renvoie dans l'ordre de son choix.
- *À compléter au fil du projet.*

---

## ARCHITECTURE

- Dépôt : https://github.com/ibrahimghellab/Downlyzer

### Architecture applicative

```mermaid
flowchart LR
    Navigateur["Navigateur<br/>(agent, responsable)"] -->|pages HTML| Web["controleur.web<br/>@Controller + Thymeleaf"]
    Externe["Système externe<br/>(simulateur de capteurs)"] -->|JSON + clé d'API| Api["controleur.api<br/>@RestController /api/**"]
    Web --> Service["service<br/>règles métier RG1 à RG11"]
    Api --> Service
    Service --> Repository["repositories<br/>Spring Data JPA"]
    Repository --> BDD[("MariaDB")]
```

Le même service est appelé par les deux types de contrôleurs : la logique métier n'est écrite qu'une fois.

Organisation des packages (`fr.efrei.downlyzer`) :

| Package | Contenu |
|---|---|
| `modele` | énumérations communes (`StatutIncident`, `EtatEquipement`, `Profil`, `SourceIncident`, `NiveauAttention`) |
| `modele.<entité>` (`station`, `equipement`, `incident`…) | une entité JPA et son repository Spring Data |
| `exception` | exceptions métier (`ExceptionMetier` et ses sous-classes) |
| `service` | règles de gestion (`IncidentService`, `EvaluationService`, `TableauDeBordService`, `StationService`, `EquipementService`, `PolitiqueUrgence`) et les objets qu'ils renvoient aux pages (`SituationStation`, `LigneTableauDeBord`, `IndicateursTableauDeBord`) |
| `securite` | configuration Spring Security et chargement de l'utilisateur connecté |
| `configuration` | beans techniques (l'horloge `Clock`) |
| `controleur.web` | pages Thymeleaf, gestionnaire d'erreurs des pages, contrôleur d'erreur |
| `controleur.web.formulaire` | objets qui reçoivent les saisies des formulaires |
| `controleur.api` | API REST (`IncidentApiControleur`) et son gestionnaire d'erreurs JSON |
| `controleur.api.dto` | objets échangés avec les systèmes externes (`IncidentDto`, `DeclarationCapteurDto`, `ResolutionDto`) |

- **`controleur.web`** : les pages de l'application, utilisées par les deux profils. Après une modification, redirection (Post/Redirect/Get) ; en cas d'erreur, une page lisible avec le bon code HTTP.
- **`controleur.api`** : l'API REST destinée au monde extérieur (F12 et piste 8). Elle expose une partie seulement de la logique métier, renvoie du JSON (DTO, jamais les entités) et des codes HTTP adaptés (200, 201, 204, 400, 404, 409).
- **Chaque repository est rangé à côté de son entité** (`modele.incident.Incident` et `modele.incident.IncidentRepository`) plutôt que dans un package `repository` commun : on trouve au même endroit une donnée et la façon d'y accéder. La séparation des couches reste la même : les contrôleurs n'utilisent jamais un repository, ils passent toujours par un service.

| Endpoint | Usage | Codes |
|---|---|---|
| `GET /api/incidents` | consulter les incidents, du plus récent au plus ancien ; `?statut=RESOLU` pour filtrer | 200, 400 (statut inconnu), 401 |
| `GET /api/incidents/{id}` | consulter un incident | 200, 400 (identifiant non numérique), 401, 404 |
| `POST /api/incidents` | un capteur déclare une panne, en désignant l'équipement par sa référence | 201 + `Location`, 400, 401, 404 (référence inconnue), 409 (incident déjà en cours) |
| `PATCH /api/incidents/{id}/resolution` | signaler le retour en service (corps et commentaire facultatifs) | 204, 401, 404, 409 (déjà résolu) |

Toutes les erreurs de l'API sont renvoyées en JSON au format `ProblemDetail` (RFC 9457) : `status`, `title`, `detail`, et pour un 409 de déclaration, `incidentExistant`.

### Pages de l'application

| URL | Fonctionnalité | Profil |
|---|---|---|
| `GET /connexion` | F1 : connexion (le `POST` est traité par Spring Security) | tous |
| `GET /` | page d'accueil : redirige le responsable vers `/tableau-de-bord`, l'agent vers `/stations` | connecté |
| `GET /tableau-de-bord` | F11 : incidents en cours, filtres (`stationId`, `typeEquipementId`, `niveau`, `ordre`) et indicateurs | responsable |
| `GET /stations` | liste des stations, triée par nom (onglet « Liste ») ; le niveau d'attention n'est affiché qu'au responsable | connecté |
| `GET /stations?vue=carte` | piste 4 : carte des stations et de leur situation (onglet « Carte ») | responsable |
| `GET /stations/{id}` | F2 : équipements de la station | connecté |
| `GET /equipements/{id}` | F3 : fiche de l'équipement et ses incidents | connecté |
| `GET /equipements/{id}/incidents/nouveau`, `POST /equipements/{id}/incidents` | F4 : déclarer un incident sur cet équipement | connecté |
| `GET /incidents/{id}` | F5 : détail d'un incident | connecté |
| `GET`, `POST /incidents/{id}/description` | F6 : modifier la description | selon l'état de l'incident (voir droits) |
| `POST /incidents/{id}/affectation` | F7 : affecter ou réaffecter un prestataire | responsable |
| `POST /incidents/{id}/resolution` | F8 : déclarer l'incident résolu | responsable |
| `GET /historique` | F9 : historique des incidents résolus | connecté (lecture seule) |
| `POST /incidents/{id}/evaluation` | F10 : évaluer l'intervention du prestataire | responsable |
| `POST /deconnexion` | F1 : déconnexion | connecté |

La déclaration d'un incident est rangée sous l'équipement (`/equipements/{id}/incidents`) parce qu'un incident se déclare toujours depuis un équipement donné (F4) ; une fois créé, l'incident a sa propre adresse (`/incidents/{id}`), indépendante de l'équipement.

### Modèle de données

```mermaid
erDiagram
    STATION ||--o{ EQUIPEMENT : contient
    TYPE_EQUIPEMENT ||--o{ EQUIPEMENT : categorise
    EQUIPEMENT ||--o{ INCIDENT : concerne
    PRESTATAIRE |o--o{ INCIDENT : "prend en charge"
    UTILISATEUR |o--o{ INCIDENT : declare
    INCIDENT ||--o| EVALUATION : "est evalue par"
    UTILISATEUR ||--o{ EVALUATION : redige
    PRESTATAIRE }o--o{ TYPE_EQUIPEMENT : "specialise en"

    STATION {
        bigint id PK
        varchar nom UK
        varchar code UK
        double latitude
        double longitude
    }
    TYPE_EQUIPEMENT {
        bigint id PK
        varchar libelle UK
    }
    EQUIPEMENT {
        bigint id PK
        varchar reference UK
        varchar libelle
        varchar etat "EN_SERVICE, DEGRADE, HORS_SERVICE"
        bigint station_id FK
        bigint type_equipement_id FK
    }
    PRESTATAIRE {
        bigint id PK
        varchar nom UK
        varchar email
        varchar telephone
        boolean actif
    }
    UTILISATEUR {
        bigint id PK
        varchar login UK
        varchar mot_de_passe_hash
        varchar prenom
        varchar nom
        varchar profil "RESPONSABLE, AGENT"
        boolean actif
    }
    INCIDENT {
        bigint id PK
        bigint equipement_id FK
        bigint declarant_id FK "null si capteur"
        varchar source "MANUELLE, CAPTEUR"
        varchar description
        datetime date_ouverture
        varchar statut "NON_AFFECTE, AFFECTE, RESOLU"
        bigint prestataire_id FK "nullable"
        datetime date_affectation "nullable"
        datetime date_resolution "nullable"
        varchar commentaire_resolution "nullable"
        int version
    }
    EVALUATION {
        bigint id PK
        bigint incident_id FK,UK
        bigint auteur_id FK
        int note "1 a 5"
        varchar commentaire "nullable"
        datetime date_evaluation
    }
```

Choix de modélisation (le sujet laisse le groupe enrichir le modèle minimal du §4) :
- **L'indicateur vert / orange / rouge n'est pas stocké** : il est calculé à partir de l'heure courante, comme la durée d'ouverture et la note moyenne d'un prestataire, elles aussi calculées.
- **Le statut est stocké** (enum) pour la lisibilité des requêtes, et sa cohérence est garantie à deux niveaux : il n'est modifié que par des méthodes métier (`affecter()`, `resoudre()`), et une contrainte `CHECK` en base refuse toute combinaison incohérente (RG2).
- **`source` et `declarant_id` nullable** : un incident peut être déclaré par un capteur via l'API (piste 8), qui n'est pas un utilisateur humain. Une contrainte `CHECK` impose quand même un déclarant à tout incident saisi à la main.
- **`date_affectation`** : permet d'afficher depuis quand un incident est pris en charge et de mesurer les délais.
- **`commentaire_resolution`** (facultatif) : garde une trace de ce qui a été fait, utile dans l'historique (F9) et pour évaluer le prestataire (F10).
- **`version`** : verrouillage optimiste (`@Version`) contre les modifications concurrentes d'un même incident.
- **`prestataire_specialite`** : relie les prestataires aux types d'équipement sur lesquels ils interviennent. Elle sert dès F7 (seuls les prestataires spécialisés sont proposés), puis aux statistiques.
- **`type_equipement` est une table** plutôt qu'un texte libre dans `equipement` : les types sont écrits une seule fois, sans faute de frappe possible, et peuvent être reliés aux spécialités des prestataires et regroupés dans les graphiques (piste 2).
- **L'état d'un équipement a trois valeurs** (`EN_SERVICE`, `DEGRADE`, `HORS_SERVICE`) : « dégradé » distingue un équipement qui fonctionne partiellement (deux portiques sur cinq bloqués) d'un équipement à l'arrêt.
- **Station : un `code` court** (`GCE`, `HDV`…) en plus du nom, unique, pour un affichage compact dans les listes ; **coordonnées** nécessaires à la cartographie (piste 4).
- **Prestataire : e-mail et téléphone séparés** (le sujet demande « de quoi le contacter ») et un booléen **`actif`** : un prestataire avec lequel on ne travaille plus n'est plus proposé à l'affectation, sans supprimer son historique.
- **Utilisateur : un `nom`** en plus du prénom (affiché pour le déclarant d'un incident) et un booléen **`actif`** : un compte désactivé ne peut plus se connecter, sans perdre la trace de ses déclarations.
- **Évaluation : un `auteur`**, pour savoir quel responsable a noté l'intervention.

Choix techniques du script `db/create.sql` :
- **Énumérations en `VARCHAR` + `CHECK`** plutôt qu'en type `ENUM` natif : c'est du SQL standard, utilisable avec d'autres bases relationnelles (§7.1), et les valeurs restent lisibles dans phpMyAdmin. Nous avions ajouté `@JdbcTypeCode(SqlTypes.VARCHAR)` sur les champs enum par précaution ; un essai sans cette annotation a montré qu'Hibernate acceptait déjà ces colonnes, elle a donc été retirée.
- **Identifiants `BIGINT AUTO_INCREMENT`**, mappés en `Long` avec `GenerationType.IDENTITY` : c'est la base qui attribue les identifiants.
- **Moteur InnoDB et encodage `utf8mb4`** : InnoDB est le seul moteur de MariaDB qui gère les transactions et les clés étrangères ; `utf8mb4` accepte tous les caractères (accents, apostrophes typographiques).
- **Contraintes nommées** (`uk_equipement_reference`, `ck_incident_statut`…) : un message d'erreur de la base dit immédiatement quelle règle a été violée.
- **Index sur `incident.statut` et `incident.date_resolution`** : le tableau de bord filtre sur le statut, l'historique trie sur la date de résolution.
- **`DROP TABLE IF EXISTS` puis `CREATE TABLE`** : le script remet la base dans un état connu à chaque exécution (§7.6).

Contraintes en base :

| Contrainte | Règle |
|---|---|
| `UNIQUE (equipement.reference)` | RG9 |
| `UNIQUE (evaluation.incident_id)`, `CHECK (note BETWEEN 1 AND 5)` | RG8 |
| `CHECK (date_resolution >= date_ouverture)` | RG5 |
| `NOT NULL` sur `equipement_id`, `date_ouverture`, `statut` | RG1 |
| `CHECK` de cohérence statut / prestataire / dates | RG2 |
| `UNIQUE (utilisateur.login)` | F1 |

Ces contraintes ne remplacent pas les contrôles de la couche service : elles évitent seulement qu'une erreur de programmation laisse la base dans un état incohérent.

---

## TESTS

- **Stratégie** : les règles métier sont testées par des tests unitaires du service, sans base de données : les repositories sont simulés avec Mockito, et l'horloge est figée (`Clock.fixed`) pour que les durées ne dépendent pas du moment où les tests s'exécutent.
  - Les simulations sont créées avec `mock(...)` dans une méthode `@BeforeEach`, et le service est construit à la main avec `new IncidentService(...)` : le test ne démarre pas Spring, il s'exécute en une fraction de seconde, et on voit dans le test exactement de quoi le service dépend.
  - Les entités utilisées dans les tests sont de vrais objets (`Incident.declarerManuellement(...)`, `affecter(...)`) et non des simulations : les règles d'`Incident` (RG1, RG2, RG5) sont donc testées en même temps que celles du service.
- **Les pages sont testées avec `@WebMvcTest` et MockMvc**, et non avec `@SpringBootTest` :
  - `@WebMvcTest` ne démarre que la couche web (les contrôleurs choisis, les `@ControllerAdvice` et la configuration de sécurité, importée avec `@Import(SecuriteConfiguration.class)`), sans serveur ni base. Un `@SpringBootTest` démarrerait toute l'application et aurait besoin de MariaDB : les tests seraient plus lents et échoueraient sur une machine sans base.
  - Le service est remplacé par une simulation (`@MockitoBean`), car ses règles sont déjà couvertes par ses propres tests. On ne vérifie que ce qui relève du contrôleur : code HTTP, page affichée, redirection, validation du formulaire, droits d'accès et CSRF.
  - L'utilisateur connecté est simulé avec `spring-security-test` (`user(new UtilisateurConnecte(...))`), ce qui évite de passer par le formulaire de connexion dans chaque test ; `csrf()` ajoute le jeton exigé par Spring Security sur les formulaires.
  - Le contenu de la page est vérifié par quelques textes attendus (`containsString`), pour s'assurer que le template s'affiche réellement sans erreur.
- **Noms des tests** : chaque méthode est nommée comme une phrase qui décrit le comportement attendu (`unAgentNePeutPlusModifierLaDescriptionDUnIncidentAffecte`), conformément à la fiche *Nommage* : un test qui échoue dit directement quelle règle est cassée.
- Commande : `mvn verify` (voir README pour `DownlyzerApplicationTests`, qui a besoin de la base).
- **Intégration continue avec GitHub Actions** (outil cité par la piste 7 du lot alpha, qui n'est pas l'une de nos deux pistes) : à chaque push sur `master` et sur chaque pull request, GitHub lance `./mvnw -B verify` sur une machine neuve, avec MariaDB puis avec MySQL. Un oubli (fichier non commité, test cassé, dépendance propre à notre poste) se voit tout de suite, et la croix rouge ou la coche verte reste attachée à chaque commit dans l'historique.
  - **Une vraie base dans le job** (conteneur `services`) plutôt que de sauter `DownlyzerApplicationTests` : c'est le seul test qui démarre toute l'application, et avec `ddl-auto=validate` il vérifie que les entités correspondent exactement au schéma de `db/create.sql`. C'est précisément l'erreur qu'un commit risque d'introduire sans qu'aucun test unitaire ne la voie.
  - **La base est créée par le même `db/create.sql` et le même compte `downlyzer_app`** (mêmes droits) qu'en local : la CI teste la procédure du README, pas une configuration à part. Le script est envoyé au client du conteneur (`mariadb` ou `mysql`, avec `docker exec`), pour ne pas dépendre du client installé sur la machine de GitHub.
  - **Deux jobs en parallèle, un sur MariaDB 11.4 et un sur MySQL 8.4** (`matrix`) : MariaDB comme en local, MySQL comme la base déployée (voir CHOIX TECHNIQUES, Déploiement). Le même script, le même compte et les mêmes tests doivent passer sur les deux, ce qu'on ne pouvait pas vérifier sur notre poste. MariaDB 11.4 et MySQL 8.4 sont les versions maintenues à long terme, au-dessus du minimum d'Hibernate 7. `fail-fast: false` : si un job échoue, l'autre va quand même au bout, pour savoir si le problème vient d'une seule base ou des deux. Côté MySQL, l'URL ajoute `sslMode=trust`, car MySQL 8 n'envoie le mot de passe que sur une connexion chiffrée ; vérifier le certificat n'aurait pas de sens pour un conteneur créé pour le job.
  - **Mots de passe écrits en clair dans le fichier du workflow** : la base n'existe que le temps du job, sur une machine jetable, et n'est joignable de nulle part ailleurs. Ce ne sont pas des secrets ; les vrais (base de production, clé d'API) ne sont jamais dans le dépôt. `DOWNLYZER_API_KEY` n'est pas définie : l'application démarre quand même, l'API refuse simplement tout (testé à part par MockMvc).
  - **`cache: maven`** dans `setup-java` : les dépendances sont gardées d'un job à l'autre tant que `pom.xml` ne change pas, ce qui évite de tout retélécharger à chaque push.
  - **Droit d'exécution ajouté à `mvnw` dans Git** (`git update-index --chmod=+x`) : le fichier avait été enregistré depuis Windows sans ce droit, et `./mvnw` échouait sous Linux.
- Couvert :
  - service des incidents (`IncidentServiceTest`, 22 tests) : déclaration par un capteur (source `CAPTEUR`, sans déclarant, équipement hors service), référence inconnue, pas de doublon quand l'équipement a déjà un incident en cours, liste de l'API avec ou sans statut ;
    - F4 : un incident déclaré démarre non affecté, à l'heure de l'horloge, et l'équipement passe hors service ;
    - F5-F6 : droits de modification de la description (agent ou responsable, incident non affecté, affecté ou résolu), incident inexistant, durée d'ouverture (incident en cours ou résolu) ;
    - F7 : affectation d'un prestataire spécialisé, réaffectation (l'ancien prestataire n'est plus proposé), refus d'un prestataire non proposé, refus d'affecter un incident résolu (RG5), seul le responsable peut affecter et résoudre ;
    - F8 : date de résolution et commentaire enregistrés, équipement remis en service s'il n'a plus d'incident en cours, ou laissé hors service sinon, commentaire vide enregistré comme absent, refus de résoudre deux fois ;
    - F9 : l'historique demande les incidents résolus triés par date de résolution décroissante.
  - tableau de bord (`TableauDeBordServiceTest`, 13 tests) : regroupement en une colonne par niveau dans l'ordre d'urgence en gardant le tri choisi, colonne vide présente ; tri par défaut du plus urgent au moins urgent (et du plus ancien au plus récent à niveau égal), un incident pris en charge passe après les incidents en attente même s'il est plus ancien (RG4), tri par station puis par urgence, tri du plus récent au plus ancien, filtre par niveau, filtres par station et par type transmis à la requête, absence de filtre, indicateurs (incidents en cours, en attente, rouges, pris en charge, équipements hors service, délai moyen converti en durée), délai moyen absent quand rien n'a été résolu, répartition par niveau (nombres et pourcentages dans l'ordre d'urgence), répartition sans division par zéro quand il n'y a aucun incident.
  - politique d'urgence (`PolitiqueUrgenceTest`, 5 tests) : exactement les cas du §7.8 — 2 h, 47 h et 47 h 59 min 59 s vert ; 48 h pile et 71 h 59 min 59 s orange ; 72 h pile et 5 jours rouge ; un incident affecté depuis 1 h ou ouvert depuis 80 h est « pris en charge » (RG4) ; un incident résolu n'a pas de niveau (RG6). Les tests construisent la vraie classe avec `new`, sans Spring ni simulation : elle ne dépend de rien d'autre que de l'incident et de l'heure.
  - situation des stations (`StationServiceTest`, 4 tests) : une station prend le niveau de son incident le plus urgent, un incident en attente passe avant un incident pris en charge, une station dont tous les incidents sont affectés est « prise en charge », une station sans incident n'a pas de niveau.
  - service des évaluations (`EvaluationServiceTest`, 8 tests) : évaluation enregistrée avec l'auteur connecté et la date de l'horloge, refus pour un incident non résolu ou résolu sans prestataire, une seule évaluation par incident, note de 1 à 5 (0 et 6 refusés, 1 et 5 acceptés), commentaire vide enregistré comme absent, seul le responsable peut évaluer, notes moyennes indexées par prestataire (RG8, F10).
  - API (`IncidentApiControleurTest`, 16 tests) : 401 en JSON sans clé ou avec une clé fausse, 403 pour un utilisateur des pages, liste des incidents en JSON sans champ interne, filtre par statut, statut inconnu (400), incident inexistant (404 en JSON), identifiant non numérique (400), panne signalée par un capteur (201 et `Location`), déclaration invalide (400 avec chaque champ), corps qui n'est pas du JSON (400), référence inconnue (404), panne déjà signalée (409 avec l'incident existant), retour en service avec ou sans corps (204), incident déjà résolu (409).
  - pages (`IncidentControleurTest`, 25 tests) :
    - accès refusé sans connexion, détail d'un incident (F5) avec ou sans lien de modification, enregistrement d'une description valide, formulaire réaffiché si la description est trop courte, refus d'un envoi sans jeton CSRF ;
    - F7-F8 : formulaires visibles pour le responsable et absents pour l'agent, affectation et résolution qui ramènent au détail avec un message, affectation sans prestataire réaffichée avec l'erreur, prestataire non proposé (400), agent qui tente d'affecter ou de résoudre (403) ;
    - F9-F10 : historique affiché avec ses incidents et le badge « À évaluer », formulaire d'évaluation visible pour le responsable, évaluation enregistrée avec un message, note invalide réaffichée avec l'erreur, agent qui tente d'évaluer (403), seconde évaluation (409) ;
    - tableau de bord (`TableauDeBordControleurTest`, 9 tests) : par défaut, les incidents sont rangés en colonnes sous la phrase de situation, sans tableau ; une colonne de plus de 6 incidents renvoie vers la liste filtrée sur son niveau ; la vue en liste affiche le tableau et les badges de niveau ; en vue par niveau, le filtre par niveau n'est pas appliqué ; la barre de répartition n'affiche que les niveaux présents, avec leur infobulle, et la légende les donne tous ; le responsable voit les incidents en cours, leur niveau, leur durée et les indicateurs ; un agent reçoit un 403 ; les filtres choisis sont transmis au service ; un filtre invalide dans l'URL est ignoré au lieu de provoquer une erreur ; après la connexion, le responsable arrive sur le tableau de bord et l'agent sur les stations ;
    - stations (`StationControleurTest`, 5 tests) : le responsable voit la situation de chaque station et l'onglet Carte ; l'agent voit les stations et leur nombre d'incidents, sans niveau d'attention ni onglet Carte ; l'onglet carte charge Leaflet et transmet la position et le niveau de chaque station ; un agent qui demande la carte reçoit un 403 ; une valeur d'onglet inconnue affiche la liste ;
    - pages d'erreur : 404 (incident inexistant, identifiant non numérique, URL inconnue), 403, 409, et erreur imprévue affichée sans détail technique.
- Non couvert :
  - la fiche équipement et les équipements d'une station (F2-F3) : ce sont de simples lectures, sans règle ;
  - le JavaScript de la carte (placement des points, couleurs, bulles) : vérifié à la main dans le navigateur ;
  - la connexion réelle (`UtilisateurDetailsService` et le contrôle du mot de passe BCrypt) : vérifiée à la main avec les deux comptes de test ;
  - les repositories, les requêtes JPQL et la requête SQL native : aucun test n'utilise une vraie base (`@DataJpaTest` demanderait une base de test). La requête native a été vérifiée à la main dans MariaDB ; RG6 (un incident résolu n'apparaît plus parmi les incidents en cours) repose sur la requête `trouverEnCours`, testée seulement dans l'application. En particulier, l'ordre de l'historique (RG7) n'est vérifié que par le choix de la requête (`findByStatutOrderByDateResolutionDesc`), et la moyenne des notes (`AVG`) n'est vérifiée qu'à la main dans l'application ;
  - l'atomicité réelle des opérations `@Transactional` (RG10) : les tests unitaires utilisent des simulations, ils vérifient les règles mais pas l'annulation en base en cas d'erreur.

---

## CHOIX TECHNIQUES

### Profils et droits

Deux profils :
- **Responsable d'exploitation** : gère le réseau : décide des affectations, valide les résolutions, évalue les prestataires et supervise la situation.
- **Agent de station** : sur le terrain : consulte les équipements et signale les pannes.

| N° | Fonctionnalité | Responsable d'exploitation | Agent de station |
|---|---|---|---|
| — | Liste des stations | Accès complet, avec le niveau d'attention de chaque station | Lecture seule : nombre d'incidents en cours, sans niveau d'attention |
| F2–F3 | Consulter équipements et fiches | Accès complet | Accès complet |
| F4 | Déclarer un incident | Accès complet | Accès complet |
| F5–F6 | Consulter / modifier un incident | Accès complet | Consultation ; modification de la description tant que l'incident n'est pas affecté |
| F7 | Affecter ou réaffecter un prestataire | Accès complet | Interdit |
| F8 | Déclarer un incident résolu | Accès complet | Interdit |
| F9 | Consulter l'historique | Accès complet | Lecture seule |
| F10 | Évaluer un prestataire | Accès complet | Interdit |
| F11 | Tableau de bord | Accès complet | Interdit |
| Alpha 2 | Graphiques de supervision | Accès complet | Interdit |
| Alpha 4 | Carte des stations (onglet de la page Stations) | Accès complet | Interdit |

Justification :
- La résolution est réservée au responsable, car c'est elle qui ouvre l'évaluation du prestataire (RG8) : elle revient à celui qui gère les prestataires.
- **Les niveaux d'attention (vert, orange, rouge, pris en charge) sont réservés au responsable**, sur la liste des stations comme sur la carte et le tableau de bord. Ils mesurent depuis combien de temps un incident attend un prestataire : c'est un outil de pilotage, et seul le responsable peut agir dessus en affectant un prestataire. Les montrer à l'agent sur la page Stations lui aurait donné, par une autre porte, l'information du tableau de bord qui lui est interdit (F11).
- **L'agent voit en revanche le nombre d'incidents en cours de chaque station** : en arrivant dans une station, il sait si des pannes y sont déjà signalées, ce qui évite les déclarations en double. Nous avions d'abord ouvert la carte et les niveaux aux deux profils ; nous sommes revenus sur ce choix pour garder le tableau des droits cohérent.
- La règle « peut superviser » est écrite une seule fois, dans `Profil.peutSuperviser()`, et utilisée par la page Stations (affichage de la colonne et de l'onglet) ; la route de la carte est en plus bloquée par Spring Security.
- L'agent peut préciser sa description tant que personne n'a pris l'incident en charge ; ensuite, le prestataire travaille sur une description stable.
- **F6 : seule la description est modifiable** (par le responsable jusqu'à la résolution, par l'agent tant que l'incident n'est pas affecté). Les autres informations ont chacune une raison de ne pas l'être :
  - le statut, le prestataire et les dates d'affectation et de résolution changent uniquement par F7 et F8, qui garantissent RG2 et RG5 : les modifier librement permettrait de contourner ces règles ;
  - la date d'ouverture sert au calcul de l'indicateur vert / orange / rouge (RG3) et des délais : la modifier permettrait de fausser la supervision ;
  - la source et le déclarant sont des faits (qui a signalé la panne, et comment), pas des informations à corriger ;
  - l'équipement n'est pas modifiable : nous avons envisagé de permettre de corriger un mauvais équipement, mais un incident déjà affecté changerait alors de nature sous les yeux du prestataire ; il vaut mieux résoudre l'incident erroné et en déclarer un nouveau ;
  - un incident n'a pas de titre : le sujet ne prévoit qu'une description, et un titre court aurait demandé un champ de plus dans la base, les formulaires et les règles de F6 pour un gain surtout visuel ;
  - un incident résolu n'est plus modifiable : il fait partie de l'historique (F9) et peut avoir été évalué (F10).
- **Le droit de modifier est vérifié dès l'affichage du formulaire**, et pas seulement à l'envoi : une adresse saisie à la main (`/incidents/9/description` pour un incident affecté) affiche directement le refus, au lieu d'un formulaire dont l'envoi serait rejeté. Le lien « Modifier la description » n'est affiché que si la modification est permise, mais ce n'est qu'un confort : la règle est appliquée par le service.
- Les systèmes externes (capteurs) ne sont pas un profil de l'application : ils accèdent uniquement à `/api/**`, avec une clé d'API.
- Nous n'avons pas créé de profil « administrateur » chargé de créer les comptes : le sujet impose deux profils, et aucune fonctionnalité ne demande de gérer les utilisateurs ; les comptes sont créés par le script SQL.
- Toutes les restrictions sont appliquées côté serveur (RG11), à deux niveaux :
  - **par Spring Security pour les routes réservées à un profil** (`POST /incidents/*/affectation`, `/incidents/*/resolution` et `/incidents/*/evaluation`, `/tableau-de-bord` et `/stations?vue=carte` exigent le rôle `RESPONSABLE`) : un agent qui forge la requête reçoit un 403 avant même d'atteindre le contrôleur ;
  - **dans la couche service pour les règles qui dépendent de l'état de l'incident** (un agent ne modifie plus un incident affecté) : Spring Security ne connaît que l'URL, pas l'incident.

### Deux types de contrôleurs
Les pages de l'application et l'API REST sont séparées en deux packages (`controleur.web` et `controleur.api`) :
- elles ne servent pas les mêmes clients (un humain dans un navigateur / un programme) ;
- elles ne répondent pas de la même manière (vue et redirection / JSON et codes HTTP) ;
- elles ne sont pas sécurisées de la même manière (formulaire de connexion et session / clé d'API sans session, réponse 401 en JSON) ;
- chacune a son propre gestionnaire d'exceptions, ciblé par package.

Une création depuis un formulaire renvoie une redirection (302, Post/Redirect/Get) et non un 201 : un navigateur ne suit pas l'en-tête `Location` d'un 201, et un rafraîchissement de la page renverrait le formulaire. Le 201 est réservé à l'API.

Les transitions d'état passent par des requêtes POST vers des URL d'action (`/incidents/{id}/affectation`, `/incidents/{id}/resolution`) plutôt que par PUT/DELETE simulés avec un champ caché : chaque action métier a sa propre adresse, et le `HiddenHttpMethodFilter` du TP n'est plus nécessaire.

### Configuration et accès à la base
- **Aucun secret dans le dépôt** (§7.7) : les identifiants de connexion sont lus dans des variables d'environnement (`${DB_USERNAME}`, `${DB_PASSWORD}`). Seule l'URL a une valeur par défaut, car elle n'est pas secrète. Les variables sont définies dans la configuration de lancement d'IntelliJ, sans « Store as project file », et `.idea/` est ignoré par Git : elles ne peuvent pas être versionnées par ce chemin non plus.
- **Compte dédié à privilèges minimaux** : l'application se connecte avec `downlyzer_app`, qui n'a que `SELECT`, `INSERT`, `UPDATE` et `DELETE` sur `downlyzer`. Une faille dans l'application ne permettrait ni de supprimer des tables, ni de lire les autres bases du serveur. Le compte n'accepte que les connexions locales (`'downlyzer_app'@'localhost'`), puisque l'application tourne sur la même machine que la base.
- **`spring.jpa.hibernate.ddl-auto=validate`** : le schéma est créé uniquement par `db/create.sql`. Au démarrage, Hibernate vérifie que les entités correspondent aux tables, sans jamais modifier la base ; une erreur de mapping bloque le démarrage au lieu de passer inaperçue.
- **`spring.web.error.include-message` retiré** : utile pendant le TP, ce réglage pourrait afficher à l'écran le message d'une exception technique. Les messages destinés à l'utilisateur passent par le gestionnaire d'erreurs.
- **`spring.jpa.open-in-view` laissé à sa valeur par défaut (actif)** : la connexion à la base reste disponible pendant l'affichage de la page, ce qui permet aux templates de lire une relation non encore chargée (les spécialités d'un prestataire, par exemple). Spring affiche un avertissement au démarrage à ce sujet. Le désactiver obligerait chaque service à charger explicitement tout ce dont la vue a besoin ; avec nos relations en `EAGER` (voir plus bas), le gain serait faible.
- **Fichiers `.properties` en UTF-8** : ils étaient enregistrés en ISO-8859-1, ce qui cassait les accents et faisait échouer la compilation Maven (`MalformedInputException`).
- **Libellés des énumérations dans `messages.properties`** (`etat.HORS_SERVICE=Hors service`) plutôt que dans les énumérations elles-mêmes : le texte affiché est une affaire de présentation, l'énumération reste une simple donnée, et tous les textes de l'interface sont au même endroit. Les templates les lisent avec `#{etat.__${equipement.etat}__}`.
- **Horloge injectée** : `HorlogeConfiguration` déclare un bean `Clock` (`Clock.systemDefaultZone()`), injecté dans les services. Personne n'appelle `LocalDateTime.now()` sans horloge : les tests peuvent figer l'heure (§7.8). Le fuseau par défaut de la machine est utilisé, comme celui de `NOW()` dans le script SQL (voir Limites connues).

### Modèle de données en Java
- **Les entités simples** (`Station`, `TypeEquipement`, `Equipement`, `Prestataire`, `Utilisateur`, `Evaluation`) suivent le modèle JavaBean du TP : annotations JPA explicites (`@Table`, `@Column` avec `nullable`, `length` et `unique` recopiés du script SQL, pour lire la structure de la table dans la classe), getters et setters. Exception : **aucun `setId()`**, car l'identifiant est attribué par la base ; le modifier pourrait faire écraser une autre ligne.
- **`Incident` n'expose pas de setters pour son état.** C'est la seule entité dont l'état obéit à des règles de gestion. Il est créé par `declarerManuellement(...)` ou `declarerParCapteur(...)`, qui garantissent RG1 (équipement et date d'ouverture obligatoires, statut `NON_AFFECTE`, aucun prestataire). Il évolue uniquement par `affecter()`, `resoudre()` et `modifierDescription()`, qui vérifient RG2 et RG5 et lèvent une `TransitionInterditeException` en cas de transition interdite. Ces règles sont donc écrites une seule fois, quelle que soit la porte d'entrée (pages, API, capteurs), et le statut, le prestataire et les dates changent toujours ensemble.
  - **Deux méthodes de création nommées plutôt qu'un constructeur public** : chaque cas a ses règles (une saisie manuelle exige un déclarant, un capteur n'en a pas), et le nom dit l'intention (`declarerParCapteur(...)` se lit mieux que `new Incident(equipement, null, CAPTEUR, ...)`). Le constructeur complet est privé.
  - **Constructeur sans argument `protected`** : JPA l'exige pour recréer un incident lu en base, mais le reste de l'application ne peut pas créer un incident vide. Il ne peut pas être `private`, car Hibernate crée des sous-classes des entités.
  - `Objects.requireNonNull` refuse un incident sans équipement ou sans date d'ouverture dès sa création, avec un message explicite.
  - **La durée d'ouverture est calculée par `Incident.dureeOuverture(maintenant)`**, jamais stockée : jusqu'à maintenant pour un incident en cours, jusqu'à la résolution sinon. Les petites questions sur l'état (`estResolu()`, `estAffecte()`, `estEvaluable()`) sont aussi posées à l'incident, à côté des données qu'elles utilisent.
- **Les règles qui dépendent du profil connecté restent dans le service** : `Incident` interdit toute modification de la description une fois résolu, mais la règle « l'agent ne la modifie que tant que l'incident n'est pas affecté » est vérifiée par le service, qui connaît l'utilisateur.
- **Un setter qui contiendrait une règle a été écarté** : son nom annoncerait un simple rangement de valeur alors qu'il refuserait ou modifierait d'autres champs, et l'ordre d'appel des setters deviendrait une règle cachée.
- **L'heure courante est passée en paramètre** des méthodes d'`Incident` : elle est fournie par le service à partir de l'horloge injectée, ce qui rend les règles testables.
- **Les énumérations sont stockées en texte** (`@Enumerated(EnumType.STRING)`) : ajouter une valeur à une énumération ne décale pas les données déjà enregistrées.
- **Relations à sens unique** : `Equipement` connaît sa `Station`, mais `Station` n'a pas de liste d'équipements ; les listes sont obtenues par des requêtes du repository. On évite ainsi les chargements inutiles et les boucles lors de la conversion en JSON.
- **Relations « vers un » en chargement immédiat (`EAGER`, valeur par défaut de `@ManyToOne`)** : charger un incident charge aussi son équipement, sa station, son prestataire et son déclarant. Les templates peuvent donc lire `incident.equipement.station.nom` sans risque de `LazyInitializationException`, et le code reste simple. La contrepartie est que des requêtes supplémentaires sont lancées pour chaque relation, même quand la page n'en a pas besoin ; avec le volume de données de l'application (quelques dizaines d'incidents), l'effet n'est pas mesurable. Le chargement paresseux (`LAZY`) avec des requêtes `JOIN FETCH` serait le choix à faire avec beaucoup plus de données.
- **Les spécialités d'un prestataire (`@ManyToMany`) restent en chargement paresseux** (valeur par défaut) : elles ne servent qu'à F7, et elles sont lues en base par une requête JPQL plutôt qu'en parcourant la collection.
- **Requêtes des repositories** : une requête dérivée du nom de la méthode quand la recherche est simple (un ou deux critères et un tri : `findByLogin`, `findByEquipementIdOrderByDateOuvertureDesc`, `existsByEquipementIdAndIdNotAndStatutNot`), du JPQL dès qu'il faut une jointure ou que le nom deviendrait illisible (`PrestataireRepository.trouverActifsSpecialisesEn`, qui parcourt la table des spécialités). Une méthode n'est ajoutée que lorsqu'un service en a besoin, pour ne pas laisser de requête inutilisée.

### Authentification (F1)
- **Spring Security avec un formulaire de connexion** : la vérification du mot de passe, la session, la protection de chaque page et la déconnexion sont confiées à une bibliothèque éprouvée, plutôt que réécrites avec `HttpSession`.
- **Page de connexion personnalisée** (`/connexion`, `connexion.html`) plutôt que la page générée par Spring Security : elle est en français, avec la mise en forme de l'application. Les champs gardent les noms attendus par défaut (`username`, `password`), ce qui évite une configuration de plus.
- **Message d'erreur volontairement générique** (« Identifiant ou mot de passe incorrect ») : préciser que le login n'existe pas permettrait de deviner les comptes existants.
- **Mots de passe hachés en BCrypt** (coût 10, valeur par défaut de `BCryptPasswordEncoder`) : un sel différent par compte et un calcul volontairement lent, comme le recommande l'OWASP (annexe C du sujet).
- **`UtilisateurConnecte`** est l'objet gardé en session : il implémente `UserDetails` et contient le login, l'empreinte, le profil, le prénom (pour l'en-tête) et l'état actif. Il copie ces valeurs au lieu de garder l'entité `Utilisateur`, qui serait détachée de la base pendant toute la session. Le profil devient un rôle préfixé `ROLE_` (convention de Spring Security, qui permet d'écrire `hasRole("RESPONSABLE")`), et un compte inactif ne peut pas se connecter (`isEnabled()`).
- **Après la connexion**, l'utilisateur revient sur la page qu'il avait demandée ; sinon il arrive sur l'adresse `/`, qui dépend de son profil : **le responsable commence par le tableau de bord** (c'est lui qui doit avoir « une vision claire de la situation », §1), l'agent par la liste des stations. Le logo Downlyzer de l'en-tête mène à cette même page d'accueil.
- **La déconnexion est un `POST` vers `/deconnexion`** (un bouton dans un formulaire) et non un simple lien : Spring Security exige le jeton CSRF, pour qu'un lien piégé sur un autre site ne puisse pas déconnecter l'utilisateur. Elle ramène sur `/connexion?logout`, qui affiche « Vous êtes déconnecté ».
- **`/error` et `/favicon.ico` sont en accès libre** : sinon, une ressource introuvable demandée par la page de connexion (l'icône de l'onglet) était bloquée, mémorisée par Spring Security comme « page demandée », et l'utilisateur y était renvoyé juste après s'être connecté (`/error?continue`).
- **L'en-tête « Bonjour [Prénom] ([profil]) | Déconnectez-vous » est un fragment Thymeleaf** (`fragments/entete.html`) inclus par chaque page : le texte n'est écrit qu'une fois. L'utilisateur connecté est fourni à toutes les pages par `UtilisateurConnecteModele`, un `@ControllerAdvice` avec une méthode `@ModelAttribute`, plutôt qu'en l'ajoutant dans chaque méthode de chaque contrôleur.

### Formulaires et saisie (F4, F6, F7, F8)
- **Un objet formulaire par saisie** (`DescriptionIncidentFormulaire`, `AffectationFormulaire`, `ResolutionFormulaire`) plutôt que l'entité, comme le `@ModelAttribute Programmeur` du TP : Spring remplit tous les champs dont le nom apparaît dans la requête, et un utilisateur pourrait ajouter `statut=RESOLU` ou `dateOuverture=...` avec les outils du navigateur. Le formulaire ne contient que ce que l'utilisateur a le droit de saisir ; le reste (équipement, date, statut, déclarant) est fixé par le serveur. `Incident` n'a d'ailleurs pas de setters que Spring pourrait appeler.
- **`DescriptionIncidentFormulaire` sert à F4 et à F6** : la déclaration et la modification portent sur la même donnée, avec la même règle de validation, écrite une seule fois.
- **Description : obligatoire, de 10 à 1 000 caractères.** 1 000 correspond à la taille de la colonne en base (une description plus longue serait refusée par MariaDB avec une erreur incompréhensible) ; 10 évite les déclarations inexploitables (« panne », « ok »), comme dans l'exemple de la fiche *Gestion des exceptions*.
- **Validation à deux niveaux** : dans le navigateur (`required`, `minlength`, `maxlength`) pour un retour immédiat, et sur le serveur (`@Valid` et `BindingResult`), qui est la vraie protection puisque la validation du navigateur se contourne facilement. En cas d'erreur, le formulaire est réaffiché avec le texte saisi et le message sous le champ.
- **Le déclarant est l'utilisateur connecté** (`@AuthenticationPrincipal`), jamais une donnée du formulaire : on ne peut pas déclarer un incident au nom de quelqu'un d'autre.
- **Message de confirmation après chaque action** (« L'incident a bien été déclaré. »), transmis par `RedirectAttributes.addFlashAttribute` : il survit à la redirection et ne s'affiche qu'une fois.
- **Les dates sont affichées au format `dd/MM/yyyy HH:mm`** (`#temporals.format`) et les durées en jours, heures et minutes.

### Affectation et résolution (F7, F8)
- **F7 : seuls les prestataires actifs et spécialisés dans le type de l'équipement sont proposés** (requête JPQL sur `prestataire_specialite`). Proposer un prestataire d'écrans pour un ascenseur n'aurait pas de sens, et la table des spécialités existait déjà. L'ordre alphabétique rend la liste facile à parcourir.
- **La réaffectation passe par le même formulaire** que l'affectation (le bouton devient « Réaffecter ») ; le prestataire déjà chargé de l'incident n'est plus proposé, et réaffecter au même prestataire est refusé. La date d'affectation devient celle de la dernière affectation.
- **Un prestataire qui ne figure pas dans la liste proposée est refusé par le service** (`DonneeInvalideException`, 400), même si la requête est forgée : la vérification réutilise exactement la liste proposée, la règle n'est donc écrite qu'une fois.
- **F8 : la date et l'heure de résolution sont celles de l'horloge**, au moment du clic, et ne sont pas saisies : elles ne peuvent donc pas précéder l'ouverture (RG5) ni être antidatées.
- **Le commentaire de résolution est facultatif** (au plus 1 000 caractères) : le sujet n'exige que la date. Un commentaire laissé vide est enregistré comme absent (`NULL`) plutôt que comme une chaîne vide.
- **Un incident peut être résolu sans avoir été affecté** (fausse alerte, redémarrage sur place) : il n'est alors pas évaluable (RG8), ce que la page de détail indique.
- **Les formulaires d'affectation et de résolution sont sur la page de détail de l'incident** plutôt que sur des pages séparées : le responsable voit l'incident et agit sans changer de page. Ils ne sont affichés qu'au responsable et tant que l'incident n'est pas résolu (`IncidentService.peutAffecterEtResoudre`).
- **L'état de l'équipement suit ses incidents** :
  - déclarer un incident fait passer l'équipement **hors service** ;
  - résoudre un incident le remet **en service**, seulement s'il n'a plus d'autre incident en cours (`existsByEquipementIdAndIdNotAndStatutNot`).
  - Sans cela, la fiche d'un équipement pouvait afficher « En service » avec une panne ouverte. Limite : l'application ne sait pas qu'une panne est seulement partielle ; l'état « dégradé » n'est jamais attribué automatiquement.
- **Pas de table d'historique des affectations** : la réaffectation remplace le prestataire de l'incident. Une table `affectation` (prestataire, date de début, date de fin) garderait la trace des réaffectations, mais aucune fonctionnalité du socle ne l'utilise.

### Historique et évaluation (F9, F10)
- **L'historique est une page à part** (`/historique`), accessible depuis l'en-tête, plutôt qu'un filtre sur une autre liste : c'est une consultation fréquente, commune aux deux profils. Il est trié par date de résolution décroissante (RG7) par une requête dérivée (`findByStatutOrderByDateResolutionDesc`) : le tri est fait par la base, pas en Java.
- **Colonnes de l'historique** : date de résolution, incident (lien vers son détail), équipement (lien vers sa fiche), station, prestataire et note. Un incident évaluable mais pas encore évalué porte un badge « À évaluer », pour que le responsable retrouve vite ce qui reste à faire ; un incident résolu sans prestataire affiche « — ».
- **Pas de filtre ni de pagination** dans l'historique : le sujet ne les demande pas, et le volume de données reste faible. Ce sera à revoir si l'historique grossit.
- **Les évaluations de l'historique sont chargées en une seule requête** (`findAll`, rangées dans une `Map` par numéro d'incident), plutôt qu'une requête par ligne du tableau.
- **Le formulaire d'évaluation est sur la page de détail de l'incident**, dans la carte « Évaluation du prestataire », comme l'affectation et la résolution : le responsable voit ce qu'il évalue (description, prestataire, durée, commentaire de résolution). Il n'apparaît qu'au responsable, pour un incident résolu par un prestataire et pas encore évalué (`EvaluationService.peutEvaluer`).
- **La note se choisit en cliquant sur des étoiles** (1 à 5), le geste habituel pour noter un service, plutôt qu'une liste déroulante ou un champ libre : les cinq valeurs sont visibles d'un coup d'œil, et aucune autre valeur ne peut être saisie depuis la page. Ce sont de vrais boutons radio dessinés en étoiles, uniquement en CSS (sans JavaScript) : le formulaire reste utilisable au clavier, et chaque étoile porte un texte caché « 3 sur 5 » pour les lecteurs d'écran. Les boutons sont écrits de 5 à 1 et affichés à l'envers, pour que la règle CSS « bouton coché ~ étoiles suivantes » colore l'étoile choisie et toutes celles à sa gauche.
- **Une note enregistrée est affichée en étoiles pleines et vides** (fragment `etoiles` de `fragments/affichage.html`, utilisé sur le détail d'un incident et dans l'historique), avec sa valeur en texte au survol et pour les lecteurs d'écran. Dans la liste de choix d'un prestataire, une balise `<option>` ne peut contenir que du texte : la moyenne y est écrite « ★ 4,0 / 5 ».
- **La note est vérifiée trois fois** : par le formulaire (`@NotNull`, `@Min(1)`, `@Max(5)`) pour réafficher un message sous le champ, par `EvaluationService` (constantes `NOTE_MINIMALE` et `NOTE_MAXIMALE`) parce que RG8 est une règle métier qui doit valoir quelle que soit la porte d'entrée (pages, future API), et par la contrainte `CHECK` de la base en dernier recours.
- **Le commentaire est facultatif** (au plus 1 000 caractères, comme la colonne) et un commentaire vide est enregistré comme absent, comme pour la résolution.
- **L'auteur est le responsable connecté et la date est celle de l'horloge** : ni l'un ni l'autre ne sont saisis.
- **Une évaluation n'est plus modifiable** une fois enregistrée : RG8 n'en autorise qu'une par incident, et la modifier changerait après coup la note moyenne déjà consultée pour choisir des prestataires.
- **Où consulter la note d'un prestataire** (F10) : sur le détail de chaque incident évalué, dans l'historique, et surtout **dans la liste de choix du prestataire au moment d'affecter un incident** (« Ascenso Maintenance — 4,0 / 5 (2 évaluation(s)) ») : c'est là que la note sert à décider. Un prestataire jamais évalué est indiqué « pas encore noté ».
- **La note moyenne est calculée par la base** (requête JPQL avec `AVG` et `GROUP BY`), jamais stockée : elle est toujours à jour. La requête construit directement un `record` `NoteMoyennePrestataire` (`SELECT new ...`) plutôt qu'un tableau d'objets, pour que le code qui l'utilise reste lisible et typé.

### Situation des stations et carte (RG3-RG4, piste 4)
- **`PolitiqueUrgence` est le seul endroit où vivent les seuils** (`SEUIL_ORANGE` = 48 h, `SEUIL_ROUGE` = 72 h, constantes nommées, §7.3). Elle reçoit l'incident et l'heure et rend un `NiveauAttention` (`ROUGE`, `ORANGE`, `VERT`, `PRIS_EN_CHARGE`) : le niveau est toujours calculé, jamais enregistré. Elle est annotée `@Component` plutôt que `@Service` : ce n'est pas un cas d'usage appelé par un contrôleur, mais une règle utilisée par les services (la carte aujourd'hui, le tableau de bord F11 ensuite).
- **La comparaison se fait sur une `Duration`**, pas sur un nombre d'heures arrondi : 47 h 59 min 59 s reste vert, 48 h pile devient orange, exactement comme l'énonce RG3.
- **Un incident résolu lève une `IllegalArgumentException`** plutôt qu'une exception métier : il n'apparaît jamais parmi les incidents en cours (RG6), donc demander son niveau est une erreur de programmation, pas une situation que l'utilisateur peut provoquer.
- **Une station prend le niveau de son incident en cours le plus urgent.** L'ordre d'urgence est l'ordre de déclaration de `NiveauAttention` (rouge, orange, vert, puis pris en charge) : un incident encore en attente d'affectation, même vert, demande une action, alors qu'un incident pris en charge est déjà traité. Une station sans incident en cours est affichée « Aucun incident ».
- **Les incidents en cours sont lus en une seule requête** (`findByStatutNot(RESOLU)`) puis regroupés par station en Java, plutôt qu'une requête par station.
- **La route de la carte est protégée par une règle Spring Security sur le paramètre `vue`** (`requestMatchers` avec une condition sur la requête), et non par un test dans le contrôleur : toutes les restrictions de route par profil restent au même endroit, dans `SecuriteConfiguration`.
- **`SituationStation` est un `record`** (identifiant, nom, code, coordonnées, niveau, nombre d'incidents en cours) et non l'entité `Station` : c'est exactement ce que la liste et la carte affichent, sans relation JPA.
- **Liste et carte sont deux onglets de la même page** (`/stations` et `/stations?vue=carte`) plutôt que deux pages : ce sont deux façons de regarder les mêmes stations. L'agent, qui n'a pas accès à la carte, ne voit pas d'onglets, seulement la liste. L'onglet est choisi par un paramètre d'URL plutôt qu'en JavaScript : chaque onglet a son adresse (on peut la mettre en favori ou la partager), le bouton « précédent » du navigateur fonctionne, le JavaScript de Bootstrap n'est pas nécessaire, et la carte n'est créée que lorsque son onglet est affiché (Leaflet calcule mal sa taille dans un onglet masqué). Une valeur inconnue affiche la liste.
- **Leaflet 1.9.4 avec le fond de carte OpenStreetMap**, chargés depuis un CDN : bibliothèque libre, légère, sans clé d'API ni compte à créer. L'attribution d'OpenStreetMap, exigée par sa licence, est affichée sur la carte. Limite : comme Bootstrap, la carte a besoin d'un accès à Internet.
- **Le JavaScript lit les stations dans la liste HTML affichée sous la carte** (attributs `data-latitude`, `data-niveau`…), qui sert aussi de légende et de version texte de la carte. Le même contenu sert donc à l'affichage et à la carte, et on ne dépend pas de la conversion d'objets Java en JavaScript par Thymeleaf. Le contenu des bulles est construit élément par élément (`textContent`) : le nom d'une station n'est jamais interprété comme du HTML.
- **Un point de couleur par station** (`circleMarker`) plutôt qu'une épingle : la couleur est l'information principale. Survoler un point affiche le nom de la station ; cliquer ouvre une bulle avec sa situation, son nombre d'incidents en cours et un lien vers ses équipements (F2), d'où l'on rejoint la fiche d'un équipement puis le détail d'un incident.
- **Les couleurs des niveaux sont définies une seule fois**, en variables CSS dans `style.css` (`--niveau-ROUGE`…) : les badges les utilisent, et le JavaScript de la carte les lit avec `getComputedStyle`. Changer une couleur ne demande de toucher qu'une ligne.
- **Les libellés des niveaux rappellent leur seuil** (« Rouge — 72 h et plus »), pour que l'utilisateur comprenne la couleur sans consulter de légende.

### Tableau de bord (F11)
- **Une page à part, `/tableau-de-bord`, réservée au responsable** (voir Profils et droits), et sa page d'accueil après la connexion.
- **Seuls les incidents en cours sont affichés** (RG6), par une requête JPQL (`trouverEnCours`) qui exclut les incidents résolus. Chaque ligne donne le niveau d'attention calculé à l'instant de l'affichage par `PolitiqueUrgence`, la durée d'ouverture, la station et l'équipement (liens vers leurs pages), la description (lien vers le détail de l'incident, pour y accéder en un clic) et le prestataire, ou « En attente ».
- **Un incident pris en charge est visuellement distingué** (RG4) de trois façons : son badge bleu « Pris en charge » au lieu d'une couleur d'urgence, son texte atténué, et sa place après les incidents en attente dans le tri par défaut. Chaque ligne porte aussi, à gauche, une bordure de la couleur de son niveau, pour repérer les urgences d'un coup d'œil.
- **Trois filtres et trois tris** (le sujet en demande au moins deux) :
  - **station** et **type d'équipement**, pour se concentrer sur une partie du réseau ou sur un métier (les ascenseurs, par exemple) ;
  - **niveau d'attention**, pour ne voir que les incidents rouges, ou que ceux qui sont pris en charge ;
  - **tri par urgence** (par défaut : du rouge au pris en charge, et à niveau égal, le plus ancien d'abord, puisque c'est celui qui va changer de couleur le premier), **par station**, ou **du plus récent au plus ancien**.
- **Les filtres passent dans l'URL** (formulaire en `GET`) plutôt qu'en session : une vue filtrée a une adresse qu'on peut mettre en favori, et le bouton « précédent » du navigateur revient au filtre d'avant. Un bouton « Réinitialiser » ramène au tableau complet.
- **Les filtres station et type sont appliqués par la base** (paramètres facultatifs de la requête JPQL : `:stationId IS NULL OR ...`), alors que **le filtre par niveau est appliqué en Java** : le niveau dépend de l'heure courante et n'est pas stocké (§5.5), la base ne peut donc pas filtrer dessus.
- **Une valeur de filtre invalide dans l'URL est ignorée** (`?niveau=VIOLET` affiche le tableau sans ce filtre) au lieu de provoquer une page d'erreur : les filtres sont une aide à la lecture, une valeur fausse ne mérite pas de bloquer la page.
- **Les indicateurs sont calculés sur tout le réseau, indépendamment des filtres** : ils résument la situation générale, et changeraient de sens si on filtrait. Ce sont : le nombre d'incidents en cours, ceux en attente d'affectation, ceux en rouge, ceux pris en charge, le nombre d'équipements hors service, et le délai moyen de résolution sur les 30 derniers jours (période assez longue pour contenir des résolutions, assez courte pour rester actuelle).
- **Les lignes et les indicateurs sont des `record`** (`LigneTableauDeBord`, `IndicateursTableauDeBord`, `PartNiveau`) : ils regroupent ce qui est calculé pour l'affichage, sans être stockés.
- **Trois maquettes très différentes ont été comparées avant de choisir** : une console dense avec barre latérale, un tableau de tri en colonnes par niveau d'urgence, et un plan de ligne schématique du réseau. Le plan de ligne a été écarté : il ne tient pas avec un vrai réseau (plusieurs lignes, correspondances, dizaines de stations) et demanderait de stocker l'ordre des stations sur chaque ligne ; la carte Leaflet de la page Stations joue déjà ce rôle avec de vraies coordonnées.
- **La présentation retenue range les incidents en quatre colonnes, une par niveau** (rouge, orange, vert, pris en charge), dans l'ordre d'urgence : le responsable voit d'un coup d'œil où sont les urgences et combien il y en a, et un incident pris en charge est séparé des autres par construction (RG4). Les colonnes sont le premier critère d'organisation ; la station, le type d'équipement et le tri restent disponibles en filtres (§5.5).
- **Une phrase résume d'abord la situation** (« 7 incidents attendent un prestataire depuis 72 h ou plus », ou « Tous les incidents en cours sont pris en charge »), avant les chiffres : elle dit ce qui demande une action. Elle change selon le niveau le plus urgent présent, et ses accords (1 incident attend / 7 incidents attendent) sont gérés.
- **Chaque colonne affiche au plus 6 fiches** (constante `FICHES_PAR_COLONNE`) ; au-delà, un lien « Voir les N incidents » ouvre la vue en liste filtrée sur ce niveau. Sans cette limite, une colonne de 40 incidents rouges rendrait la page interminable.
- **Une seconde vue, « Liste », reprend le tableau complet** avec la barre de répartition : plus pratique quand les incidents sont nombreux ou pour trier finement. On passe de l'une à l'autre par deux boutons accolés qui gardent les filtres en cours ; la vue est un paramètre d'URL (`vue=COLONNES` ou `vue=LISTE`), comme les filtres. En vue par niveau, le filtre par niveau n'est pas proposé ni appliqué : chaque niveau a déjà sa colonne.
- **Une fiche d'incident est un lien vers son détail** et donne l'équipement, la station, la description sur deux lignes au plus, et la durée (« En attente depuis 6 j 17 h », ou « Ouvert depuis » et le nom du prestataire pour un incident pris en charge). Un liseré gauche porte la couleur du niveau.
- **Une barre de répartition des incidents en cours par niveau**, dans la vue en liste, pour « identifier immédiatement la situation » (§5.5) : rouge, orange, vert puis pris en charge, chaque part proportionnelle à son nombre d'incidents. C'est une proportion d'un tout, d'où une **barre empilée horizontale** plutôt qu'un camembert : on compare des longueurs alignées bien plus précisément que des angles. Elle est construite en HTML et CSS, sans bibliothèque de graphiques : une seule barre ne justifie pas une dépendance de plus.
  - Les parts sont dans l'ordre d'urgence (le même que le tri du tableau), séparées par un espace de 2 px et arrondies aux extrémités ; une part vide n'est pas dessinée, mais la **légende**, toujours présente, donne les quatre niveaux avec leur nombre, y compris zéro.
  - Le nombre est écrit dans chaque part ; au survol ou au focus clavier, une infobulle donne le niveau, le nombre et le pourcentage. Chaque part est un lien qui applique le filtre de ce niveau.
  - La barre a un équivalent texte : chaque part porte une description pour les lecteurs d'écran (`aria-label`), et la légende contient les mêmes chiffres.
- **Les valeurs des indicateurs restent dans la couleur du texte** ; la couleur d'un niveau est portée par une pastille placée à côté du libellé. Un chiffre coloré en orange ou en vert clair devient difficile à lire, et la couleur ne doit jamais être la seule façon de comprendre une information.
- **La palette des niveaux a été vérifiée avec un outil de contrôle des couleurs** (séparation sous les trois formes de daltonisme, écart entre couleurs voisines, contraste) plutôt qu'à l'œil : les quatre couleurs, dans l'ordre de la barre, restent distinguables pour un daltonien (écart minimal ΔE 9,9 entre vert et orange, pour un seuil de 8) et pour une vision normale (ΔE 16,9 entre orange et rouge, seuil 15). Seul le contraste de l'orange sur fond blanc est insuffisant (2,6:1) : **le texte posé sur l'orange (badges, barre) est en encre foncée et non en blanc**, et chaque couleur est toujours accompagnée de son libellé.
- **La colonne « Ouvert depuis » utilise des chiffres à chasse fixe** (`tabular-nums`) pour que les durées s'alignent d'une ligne à l'autre ; les grands chiffres des indicateurs gardent des chiffres proportionnels, plus lisibles en gros.
- **Pas de mode sombre** : l'application suit le thème clair de Bootstrap sur toutes ses pages ; un mode sombre demanderait de choisir et vérifier une seconde série de couleurs pour toute l'interface.

### API REST (F12) et capteurs (piste 8)
- **`ResponseEntity` dans chaque méthode de l'API, comme dans la partie REST du TP fil rouge** : `ok(...)` (200), `created(adresse).body(...)` (201, avec l'en-tête `Location` construit par `ServletUriComponentsBuilder`) et `noContent()` (204). Le code de succès se lit donc directement dans la méthode. Les erreurs, elles, ne sont pas décidées dans le contrôleur : ce sont les exceptions métier du service, traduites en JSON par `GestionnaireExceptionsApi` (voir Exceptions), ce qui évite de répéter les mêmes `notFound()` dans chaque méthode.
- **L'API renvoie des DTO (`record`), jamais les entités** : pas de relation JPA à sérialiser ni de risque de boucle, et aucun champ interne exposé (version, données du déclarant). Le statut et la source sont envoyés sous forme de texte (`NON_AFFECTE`, `CAPTEUR`).
- **Un capteur désigne l'équipement par sa référence** (`ASC-004`), unique d'après RG9, et non par son identifiant en base, qu'il ne connaît pas.
- **Un seul incident en cours par équipement** (point métier tranché) : un capteur qui répète son signal ne crée pas de doublon ; il reçoit un **409** qui donne l'identifiant de l'incident déjà ouvert (`incidentExistant`), pour pouvoir le suivre. Créer un doublon fausserait le tableau de bord ; renvoyer simplement l'incident existant avec un 200 aurait caché au capteur que son signalement n'a rien créé.
- **La déclaration par un capteur suit les mêmes règles qu'une déclaration manuelle** : `Incident.declarerParCapteur` (RG1, pas de déclarant), même validation de la description (10 à 1 000 caractères), et l'équipement passe hors service, dans une transaction.
- **Le retour en service est un `PATCH`** sur `/api/incidents/{id}/resolution` : seule une partie de l'incident change (son statut, sa date et son commentaire de résolution), et l'adresse nomme l'action, comme les URL d'action des pages. Le corps est facultatif.
- **Une clé d'API dans l'en-tête `X-API-KEY`** plutôt qu'un compte utilisateur : un capteur ne remplit pas de formulaire de connexion. La clé est lue dans la variable d'environnement `DOWNLYZER_API_KEY`, jamais écrite dans le code (§7.7).
  - Le filtre `CleApiFiltre` compare la clé reçue en **temps constant** (`MessageDigest.isEqual`) : le temps de réponse ne permet pas de deviner la clé caractère par caractère. Sans clé configurée, aucune n'est acceptée.
  - **Une seconde chaîne Spring Security, réservée à `/api/**`**, sans session ni formulaire ni protection CSRF (aucun navigateur n'y envoie de cookie de session) : une requête sans clé valide reçoit un **401 en JSON**, au lieu d'être redirigée vers la page de connexion. Un utilisateur connecté aux pages n'a pas accès à l'API pour autant (**403**) : ce n'est pas un système externe.
- **Swagger (springdoc 3.1.1)** génère la documentation en lisant directement les contrôleurs, sans annotation de documentation dans le code (`@Operation`, `@ApiResponse`…), pour rester au plus près du code du TP. Seule la clé d'API est déclarée (`DocumentationApiConfiguration`), pour que le bouton « Authorize » de Swagger permette d'essayer les requêtes. La documentation ne couvre que `/api/**` (`springdoc.paths-to-match`), et ses pages restent accessibles sans compte, puisque les appels qu'elles font exigent de toute façon la clé.
  - Les codes d'erreur (400, 404, 409) apparaissent dans Swagger grâce au `@ResponseStatus` placé sur chaque méthode de `GestionnaireExceptionsApi`, comme dans le gestionnaire des pages : springdoc lit cette annotation de Spring.
  - **Limite** : sans annotation de documentation, springdoc ne sait pas lire le code d'un `ResponseEntity` ; Swagger affiche donc « 200 » comme réponse réussie du `POST` (201 en réalité) et du `PATCH` (204). Les vrais codes sont donnés par le tableau des requêtes de la section ARCHITECTURE et vérifiés par les tests. Nous avons préféré cette limite à des annotations `@ApiResponse` sur chaque méthode, qui n'ont pas été vues en cours et doubleraient le code.
- **Une adresse inconnue sous `/api` reçoit une erreur en JSON** : `ErreurControleur` renvoie un `ProblemDetail` à un client qui demande du JSON, et la page `erreur.html` à un navigateur.
- **Un simulateur de capteurs** (`simulateur/SimulateurCapteurs.java`) joue le rôle du système externe : un programme Java autonome, hors de l'application, lancé avec `java` sans compilation préalable. Il envoie à intervalles réguliers des pannes sur des équipements tirés au hasard, avec le client HTTP de Java (`HttpClient`), et affiche le code reçu (201, ou 409 quand l'équipement a déjà un incident en cours).
- **`requetes.http`** remplace celui du TP : il contient une requête par cas de l'API (succès et erreurs), à lancer depuis IntelliJ ; la clé est lue dans un fichier local ignoré par Git.

### Exceptions
- Exceptions métier personnalisées, non vérifiées (`RuntimeException`, qui n'obligent pas chaque méthode à les déclarer), héritant d'une classe mère abstraite `ExceptionMetier` (fiche *Gestion des exceptions*), avec des messages en français qui indiquent l'objet concerné et la raison :
  - `RessourceIntrouvableException` (abstraite) et ses filles `StationIntrouvableException`, `EquipementIntrouvableException`, `IncidentIntrouvableException`, `PrestataireIntrouvableException`, `UtilisateurIntrouvableException` ;
  - `TransitionInterditeException` : l'état de l'incident interdit l'opération (par exemple, modifier un incident résolu) ;
  - `ActionNonAutoriseeException` : l'opération est possible, mais pas pour le profil connecté, dans l'état actuel de l'incident (par exemple, un agent qui modifie un incident affecté) ;
  - `DonneeInvalideException` : une donnée envoyée est refusée par une règle métier que la validation du formulaire ne peut pas vérifier seule (par exemple, un prestataire qui n'intervient pas sur ce type d'équipement).
- **Traduction en réponses HTTP : `@ControllerAdvice` plutôt que `ResponseStatusException`** (§7.4). Avec `ResponseStatusException`, comme dans le TP, chaque contrôleur choisit lui-même le code HTTP et répète le même `orElseThrow(...)` ; le service, lui, ne doit rien savoir de HTTP. Ici, le service lève une exception métier, et un seul gestionnaire par package de contrôleurs la traduit. Pour les pages, `GestionnaireExceptionsWeb` (limité à `controleur.web`) affiche la page `erreur.html` avec le message métier : **404** pour une ressource introuvable, **403** pour une action non autorisée, **409** pour une transition interdite ou une modification concurrente, **400** pour une donnée invalide.
  - Les contrôleurs ne contiennent ni `try/catch` ni test d'existence : ils appellent le service, et chaque exception est traduite à un seul endroit.
  - Gérer les classes mères (`RessourceIntrouvableException`) évite d'écrire un gestionnaire par entité : une nouvelle exception « introuvable » est prise en charge sans rien ajouter.
  - Un identifiant non numérique dans l'URL (`/incidents/abc`) est traité comme une ressource introuvable (**404**) plutôt que comme une requête invalide (400) : pour l'utilisateur, cette page n'existe pas.
  - Une modification concurrente (`ObjectOptimisticLockingFailureException`, levée par `@Version`) donne un **409** avec un message qui invite à recharger la page, plutôt qu'une erreur technique.
  - Une contrainte `UNIQUE` violée (`DataIntegrityViolationException`, par exemple deux responsables qui évaluent le même incident au même instant, après que le service a vérifié des deux côtés qu'il n'y avait pas encore d'évaluation) donne aussi un **409**, au lieu d'une erreur 500.
  - `ResponseEntity` n'est pas utilisé côté pages : une page renvoie le nom d'une vue, et le code HTTP est fixé par `@ResponseStatus` sur le gestionnaire. Il sera utilisé dans l'API, uniquement pour le 201 qui doit porter l'en-tête `Location`.
- Les erreurs qui n'atteignent jamais un de nos contrôleurs (URL inconnue, accès refusé par Spring Security, exception imprévue) sont reprises par `ErreurControleur`, qui remplace la page « Whitelabel » de Spring Boot et affiche la même page `erreur.html`, avec un message générique en français : le détail technique d'une exception n'est jamais montré à l'utilisateur. Il est placé dans `controleur.web` pour que l'en-tête affiche toujours l'utilisateur connecté.
  - Pour un client qui demande du JSON (un système externe), le même contrôleur renvoie un `ProblemDetail` au lieu de la page.
- **L'API a son propre gestionnaire, `GestionnaireExceptionsApi`** (`@RestControllerAdvice` limité à `controleur.api`), qui traduit les mêmes exceptions métier en JSON (`ProblemDetail`, RFC 9457) : **404** pour une ressource introuvable, **409** pour une transition interdite, une modification concurrente ou un incident déjà en cours, **400** pour des données invalides (avec chaque champ en cause et son message), un corps qui n'est pas du JSON, ou un paramètre mal formé. Contrairement aux pages, un identifiant non numérique est ici un **400** : c'est la requête du programme qui est fausse, pas une page qui n'existerait pas.

### Interface
- **Une identité propre plutôt que l'apparence Bootstrap par défaut** : une console de supervision sobre, où la couleur est réservée à l'information. La première version (barre noire, cartes ombrées, gros chiffres isolés, bleu standard) ressemblait à n'importe quel modèle ; elle a été reprise sur toute l'application :
  - un **symbole** (une double flèche vers le bas, pour *down*, dans un carré bleu) à côté du nom, dans l'en-tête et sur la page de connexion, dessiné en SVG dans un fragment pour n'être écrit qu'une fois ;
  - la police **Inter** (Google Fonts), lisible en petite taille et pour les chiffres ; à défaut de connexion, la police du système prend le relais ;
  - un **bleu de marque** (`#1f4fd1`) plus profond que celui de Bootstrap, appliqué en redéfinissant les variables CSS de Bootstrap plutôt qu'en réécrivant ses composants ;
  - une **barre d'en-tête blanche** avec un trait fin, où l'entrée de la page affichée est soulignée (le fragment d'en-tête reçoit la section en paramètre) ;
  - des **bordures fines au lieu des ombres**, des tableaux aux en-têtes discrets et aux lignes compactes, des liens de la couleur du texte qui se colorent au survol, et les longues descriptions tenues sur une ligne avec le texte complet au survol ;
  - des **badges à fond légèrement teinté, avec un point de couleur et un texte foncé**, au lieu de pastilles pleines : plus discrets, et toujours lisibles quelle que soit la couleur. Le bleu signifie « pris en charge » partout (statut « Affecté » comme niveau d'attention).
- Le texte imposé par F1, « Bonjour [Prénom] ([profil]) | Déconnectez-vous », est conservé mot pour mot en haut à droite.
- **Bootstrap 5.3.3**, chargé depuis un CDN comme dans le TP : mise en page cohérente sur toutes les pages (barre d'en-tête, cartes, tableaux, formulaires, alertes) sans écrire de CSS. Inconvénient : il faut un accès à Internet pour que les pages soient mises en forme. Une WebJar (Bootstrap en dépendance Maven) supprimerait cette limite au prix d'une dépendance de plus.
- Les deux feuilles de style sont déclarées une seule fois, dans le fragment `styles` de `fragments/entete.html`, inclus par chaque page.
- `static/css/style.css` ne contient que ce qui est propre à Downlyzer, notamment la couleur des badges d'état et de statut. La classe d'un badge reprend le nom de la constante stockée en base (`etat-HORS_SERVICE`, `statut-NON_AFFECTE`…) : la couleur est donc choisie par la feuille de style, sans condition dans les templates.
- Les erreurs de saisie sont affichées sous le champ concerné, qui est entouré en rouge (`th:errorclass="is-invalid"`).
- **Templates rangés par dossier** (`stations/`, `equipements/`, `incidents/`, `fragments/`) : le nom renvoyé par un contrôleur (`"incidents/detail"`) dit où chercher la vue, et plusieurs pages de détail peuvent coexister sans conflit de nom.
- **Navigation par liens** : stations → équipements d'une station → fiche équipement → détail d'un incident, avec un lien de retour en haut de chaque page. La barre d'en-tête contient deux liens permanents, « Stations » et « Historique », les deux points d'entrée communs aux deux profils.
- La liste des stations affiche le code et le nom, triée par nom (`findAllByOrderByNomAsc`).

### Organisation du projet
- **Nom du projet : Downlyzer**, contraction de *down* (en panne) et *analyzer* : court, facile à retenir, et il dit ce que fait l'application (§12).
- **Le dépôt est un fork du dépôt du TP fil rouge** : la pile technique (Java 21, Spring Boot 4.1.1, Maven) est identique à celle du TP, comme l'exige le §7.1. Le remote `upstream` pointe vers le dépôt de l'enseignant ; le code du TP (`Programmeur`) a été supprimé au fil de la mise en place du projet.
- **Package `fr.efrei.downlyzer`** et classe `DownlyzerApplication` à la place de `altn72.tpfilrouge` : nom de domaine inversé de l'école suivi du nom du projet, conformément aux conventions Java et à la fiche *Nommage*.
- **Identifiants en français**, issus du vocabulaire du domaine (`Incident`, `Prestataire`, `declarerIncident`, `peutModifierDescription`), dans une seule langue (§7.3). Les termes imposés par Java ou Spring (`Repository`, `get`/`set`) restent en anglais.
- **Messages de commit préfixés par la fonctionnalité** (`F4 : déclaration d'un incident`) : l'historique se lit fonctionnalité par fonctionnalité (§7.10).
- `target/` et `.idea/` sont ignorés par Git : ils sont produits par Maven et par l'IDE, et `.idea/` peut contenir les variables d'environnement (donc le mot de passe de la base).

### Déploiement (§7.9)
- **Render pour l'application et Aiven pour la base**, deux offres gratuites :
  - Railway, qui héberge les deux ensemble, a été essayé en premier, mais son offre gratuite est limitée à une période d'essai déjà terminée sur notre compte.
  - Vercel ne fait tourner que des fonctions courtes (Node, Python, Go) et ne lance ni Java ni Docker : une application Spring Boot avec une session de connexion n'y a pas sa place.
  - Supabase et la base gratuite de Render sont en PostgreSQL : il aurait fallu changer de pilote, réécrire `db/create.sql` et la requête native (`TIMESTAMPDIFF` n'existe pas en PostgreSQL).
  - Render lance directement le `Dockerfile` du dépôt et redéploie à chaque push sur `master` ; Aiven fournit gratuitement un MySQL 8 sans carte bancaire.
- **MySQL 8 en production, MariaDB en local** : aucune offre gratuite ne propose MariaDB, et MySQL 8 accepte le même SQL pour tout ce que fait le projet (types, `CHECK` appliqués depuis 8.0.16, `NOW() - INTERVAL`, `TIMESTAMPDIFF`). `db/create.sql` passe sans modification ; Aiven exige une clé primaire sur chaque table, ce qui était déjà le cas. Le pilote reste `mariadb-java-client`, qui sait se connecter à MySQL. En revanche, Hibernate choisit son dialecte d'après le pilote et prenait le dialecte MariaDB même face à MySQL (avertissement `HHH000511` au premier déploiement) : le dialecte est donc donné par `spring.jpa.database-platform=${DB_DIALECT:...}`, MariaDB par défaut comme en local, MySQL sur Render et dans le job MySQL de la CI. Pour ne pas le découvrir en production, la CI lance tous les tests sur les deux (voir TESTS).
- **Connexion chiffrée et vérifiée** : Aiven refuse les connexions sans TLS. L'URL utilise `sslMode=verify-full` avec le certificat de l'autorité d'Aiven, copié dans l'image : la connexion est chiffrée et le pilote vérifie qu'il parle bien au serveur d'Aiven, pas à un intermédiaire. Ce certificat est public (il ne sert qu'à vérifier, pas à se connecter) et peut donc être versionné ; `sslMode=trust` aurait chiffré sans rien vérifier.
- **`db/ChargementBase.java` pour charger la base distante** : le client `mysql` de XAMPP (MariaDB 10.4) ne sait pas s'authentifier auprès de MySQL 8 (module `sha256_password` absent). Plutôt que d'installer un logiciel de plus, ce petit programme exécute `db/create.sql` avec le pilote JDBC de l'application, comme le simulateur de capteurs, en un seul fichier lancé par `java`. Les mots de passe sont lus dans des variables d'environnement, jamais en argument (ils resteraient dans l'historique du terminal). Le script est découpé sur les `;` de fin de ligne, puis chaque instruction est exécutée à part, ce qui donne une erreur précise si l'une échoue. Le mot de passe de `downlyzer_app` doit être fait de lettres et de chiffres, car `CREATE USER` n'accepte pas de paramètre `?` : le programme refuse tout autre caractère, ce qui écarte toute injection.
- **`Dockerfile` en deux étapes** : une image Maven construit le jar, puis seule une image avec le JRE 21 le lance. L'image finale ne contient ni Maven, ni le code source, ni le dossier `.m2`. Le `pom.xml` est copié avant le code pour que Docker garde les dépendances en cache tant qu'il ne change pas. `.dockerignore` exclut `target/`, `.idea/`, `.git/` et le fichier de clé du client HTTP.
- **Tests sautés dans l'image (`-DskipTests`)** : `DownlyzerApplicationTests` a besoin d'une base, qui n'existe pas pendant la construction. Ils sont lancés par le pipeline GitHub Actions, qui a une base (voir TESTS).
- **`-XX:MaxRAMPercentage=75`** : l'offre gratuite de Render donne 512 Mo, et Java ne réserve par défaut qu'un quart de la mémoire du conteneur pour ses objets (128 Mo), trop juste pour Spring Boot et Hibernate. Les 25 % restants suffisent au reste de la JVM.
- **`server.port=${PORT:8080}`** : l'hébergeur choisit le port et le donne dans la variable `PORT` ; en local, rien ne change.
- **`server.forward-headers-strategy=framework`** : Render reçoit les requêtes en HTTPS et les transmet à l'application en HTTP. Ce réglage fait lire à Spring les en-têtes `X-Forwarded-*` du proxy : l'en-tête `Location` d'un 201 et les redirections (après connexion par exemple) restent en `https://`.
- **Mot de passe retiré des logs** (`logging.level.org.hibernate.orm.connections.pooling=WARN`) : au démarrage, Hibernate 7 affiche l'URL de connexion, et le pilote MariaDB y ajoute l'utilisateur et le mot de passe. Le premier déploiement a donc écrit le mot de passe de `downlyzer_app` en clair dans les logs de Render. Ce message, purement informatif, est masqué, et le mot de passe a été changé (`db/ChargementBase.java` le remet à jour) : l'ancien, resté dans l'historique des logs, ne sert plus à rien.
- **`TZ=Europe/Paris` dans l'image** : les serveurs sont en UTC. L'horloge de l'application (`Clock.systemDefaultZone()`) et le `NOW()` du script de données doivent être dans le même fuseau (voir Limites connues), d'où aussi le `SET time_zone` au chargement du script.
- **Mêmes variables d'environnement qu'en local** (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DOWNLYZER_API_KEY`), saisies dans l'onglet *Environment* de Render : aucun secret dans le dépôt, et aucune ligne de code propre au déploiement. La clé d'API de production est différente de celle utilisée en local.
- **Compte `downlyzer_app` aussi en production**, avec les mêmes droits (`SELECT`, `INSERT`, `UPDATE`, `DELETE`), plutôt que le compte administrateur `avnadmin` fourni par Aiven. Il est créé pour `'%'` et non `'localhost'`, car l'application et la base ne sont pas sur la même machine ; il est protégé par un mot de passe long et par le TLS obligatoire.
- **Limite** : l'application s'endort après 15 minutes sans visite (premier chargement d'environ une minute), et Aiven peut éteindre une base gratuite restée longtemps inutilisée. Les deux suffisent pour une démonstration et la correction, pas pour une vraie exploitation.

### SOLID
- **S — Responsabilité unique (appliqué)** :
  - **`EvaluationService` est séparé d'`IncidentService`** : l'évaluation des prestataires (RG8, notes moyennes) a ses propres règles, ses propres données et changerait pour d'autres raisons (barème, notation par critère) que le cycle de vie d'un incident. Concrètement, `IncidentService` n'a plus besoin du repository des évaluations, et chaque classe de test ne prépare que ce dont son service a besoin.
  - **`Incident` protège son propre état** (`affecter()`, `resoudre()`, `modifierDescription()`), tandis que les services orchestrent un cas d'usage (charger, vérifier les droits, enregistrer) et que les contrôleurs ne font que traduire HTTP en appels de service : chaque couche a une seule raison de changer.
  - **`PolitiqueUrgence` ne fait qu'une chose** : dire quel est le niveau d'attention d'un incident. Changer un seuil (72 h → 96 h) ne touche que cette classe ; la carte et le futur tableau de bord en profitent sans modification.
  - **`TableauDeBordService` est séparé d'`IncidentService`** : il ne modifie rien, il ne fait que présenter les incidents en cours (filtres, tris, indicateurs), et il évoluera pour d'autres raisons (nouveaux indicateurs, nouveaux filtres) que les règles du cycle de vie.
  - **Les contrôleurs sont découpés par écran** (`StationControleur`, `EquipementControleur`, `IncidentControleur`, `HistoriqueControleur`, `TableauDeBordControleur`), et la traduction des exceptions en pages d'erreur est isolée dans `GestionnaireExceptionsWeb`.
- *Autres principes : à compléter.*

### Requête SQL native
- **Où** : `IncidentRepository.calculerDelaiMoyenDeResolutionEnMinutes`, utilisée par l'indicateur « Délai moyen de résolution (30 derniers jours) » du tableau de bord (F11) :
  ```sql
  SELECT AVG(TIMESTAMPDIFF(MINUTE, date_ouverture, date_resolution))
  FROM incident
  WHERE statut = 'RESOLU' AND date_resolution >= :depuis
  ```
- **Pourquoi en SQL natif** : le calcul d'une durée entre deux dates et sa moyenne sont faits directement par la base, sur toutes les lignes, sans charger les incidents en mémoire. En JPQL, la différence entre deux dates n'a pas de fonction portable : il aurait fallu charger tous les incidents résolus et calculer la moyenne en Java.
- **Analyse critique** :
  - **Non portable** : `TIMESTAMPDIFF` est une fonction de MariaDB et MySQL. Avec PostgreSQL, il faudrait écrire `EXTRACT(EPOCH FROM date_resolution - date_ouverture) / 60` : changer de base (§7.1 l'autorise) obligerait à réécrire cette requête, alors que les requêtes JPQL et dérivées seraient traduites automatiquement par Hibernate.
  - **Liée au schéma et non au modèle** : elle utilise les noms de table et de colonnes (`incident`, `date_resolution`) et la valeur texte `'RESOLU'`, et non l'entité `Incident` et l'énumération `StatutIncident`. Renommer un champ ou une valeur d'énumération ne provoquerait aucune erreur de compilation : la requête échouerait seulement à l'exécution.
  - **Non vérifiée au démarrage** : Hibernate valide la syntaxe des requêtes JPQL au lancement de l'application, mais pas celle d'une requête native, qui n'est contrôlée par MariaDB qu'au premier appel.
  - **Résultat non typé** : la base renvoie un nombre décimal, que Spring Data convertit en `Double` ; le service le transforme ensuite en `Duration`. Elle renvoie `NULL` quand aucun incident n'a été résolu sur la période, ce que le service doit gérer explicitement.
  - **Ce qui limite les risques** : la requête est courte, isolée dans le repository derrière une méthode au nom explicite, et la date de début est passée en paramètre (`:depuis`) plutôt que concaténée, ce qui exclut l'injection SQL. Elle a été testée à la main sur les données de démonstration (13 incidents résolus, délai moyen d'environ 30 h).
  - **Conclusion** : utile ici pour un calcul d'agrégation sur des dates, mais à réserver à ce type de cas ; tout le reste du projet passe par des requêtes dérivées ou JPQL, qui restent indépendantes de la base.

### @Transactional
- **`IncidentService.declarerIncident` (F4)** : l'incident est enregistré et l'équipement passe hors service. Ce sont deux écritures sur deux tables : la transaction garantit qu'elles réussissent ensemble ou pas du tout (RG10). Sans elle, une erreur entre les deux laisserait un incident ouvert sur un équipement affiché « En service ».
- **`IncidentService.modifierDescription` (F6)** : l'incident est chargé, vérifié puis modifié dans une seule transaction. Hibernate enregistre la modification de lui-même à la fin de la transaction, sans appel à `save()`. Le verrouillage optimiste (`@Version`) est vérifié au même moment : si quelqu'un a modifié l'incident entre son chargement et l'enregistrement, l'enregistrement est refusé (409) au lieu d'écraser son travail. Le formulaire ne transporte pas la version : une modification faite pendant que l'utilisateur remplit le formulaire n'est donc pas détectée.
- **`IncidentService.affecterPrestataire` (F7)** : la vérification (le prestataire fait partie de la liste proposée, l'incident n'est pas résolu) et la modification se font dans la même transaction, avec le même verrouillage optimiste : deux responsables qui affectent le même incident en même temps ne peuvent pas s'écraser sans le savoir.
- **`IncidentService.resoudreIncident` (F8)** : l'incident passe résolu et, s'il n'a plus d'autre incident en cours, l'équipement repasse en service. Deux tables modifiées, donc une seule transaction (RG10).
- **`EvaluationService.evaluerIntervention` (F10) n'est pas annoté** : il n'écrit qu'une seule ligne (l'évaluation), et `save()` s'exécute déjà dans sa propre transaction. Le risque de deux évaluations simultanées du même incident n'est pas réglé par une transaction (les deux vérifications passeraient quand même), mais par la contrainte `UNIQUE (incident_id)`, traduite en 409.
- Les méthodes de lecture (`trouverIncident`, `listerStations`, `listerIncidentsEnCours`, `calculerIndicateurs`…) ne sont pas annotées : chaque appel au repository s'exécute déjà dans sa propre transaction en lecture, et il n'y a rien à rendre atomique.
- L'annotation utilisée est celle de Spring (`org.springframework.transaction.annotation.Transactional`), gérée nativement par Spring, et non celle de Jakarta.

---

## LOT ALPHA

Fonctionnalités retenues : pistes **8** (source de données externe) et **4** (cartographie). La piste **2** (graphiques) était prévue comme seconde fonctionnalité ; la carte, réalisée en premier, a pris sa place, et la barre de répartition du tableau de bord en reprend une partie (voir plus bas).

Ces pistes ont été choisies parce qu'elles se renforcent : les incidents envoyés par les capteurs (8) alimentent les graphiques (2) et la carte (4). Les pistes 5 (journal métier) et 7 (qualité) étaient aussi envisagées ; la piste 6 a été écartée, car elle ferait appeler par nos propres pages l'API réservée aux systèmes externes, avec un second mode d'authentification à gérer.

### Fonctionnalité transverse 1 : source de données externe (piste 8)
- **Besoin** : des capteurs installés sur les équipements déclarent automatiquement les pannes, sans intervention humaine.
- **Périmètre envisagé** : endpoint `POST /api/incidents` sécurisé par clé d'API ; l'équipement est désigné par sa référence (le capteur ne connaît pas l'identifiant technique) ; un client de test simule plusieurs capteurs qui envoient des pannes.
- **Choix et essais** (détail dans CHOIX TECHNIQUES, API REST et capteurs) :
  - la même API sert F12 (consulter les incidents, modifier leur état) et la piste 8 (déclarer une panne) : un capteur est un système externe comme un autre ;
  - une clé d'API dans un en-tête plutôt qu'un compte technique en HTTP Basic : rien à créer dans la table des utilisateurs, et l'API reste séparée des profils de l'application ;
  - un 409 avec l'incident existant quand l'équipement a déjà un incident en cours, pour qu'un capteur qui répète son signal ne crée pas de doublon ;
  - le simulateur est un programme Java autonome plutôt qu'un script `curl` : il tourne de la même façon sous Windows, macOS et Linux, et montre ce que ferait un vrai système externe.
- **Ce qui fonctionne** : déclaration d'une panne par référence (201 et `Location`), refus d'un doublon (409), d'une référence inconnue (404) et de données invalides (400) ; consultation et résolution par l'API ; 401 en JSON sans clé, 403 pour un utilisateur des pages ; documentation Swagger avec saisie de la clé ; simulateur qui alimente le tableau de bord en direct. Tout est couvert par les tests de `IncidentApiControleurTest` et `IncidentServiceTest`.
- **Difficultés** :
  - la sécurité des pages redirigeait toute requête non authentifiée vers la page de connexion, y compris celles de l'API : il a fallu une seconde chaîne Spring Security, dédiée à `/api/**`, avec ses propres réponses en JSON ;
  - un premier test montrait qu'un utilisateur des pages recevait un 403 au corps vide : la réponse de refus a été écrite en JSON, comme le 401.
- **Reste à faire** : une clé par capteur (aujourd'hui, une clé unique pour tous les systèmes externes), et l'enregistrement du capteur qui a signalé la panne.

### Fonctionnalité transverse 2 : cartographie du réseau (piste 4)
- **Besoin** : voir d'un coup d'œil où se trouvent les problèmes sur le réseau.
- **Périmètre envisagé** : carte des stations (Leaflet + OpenStreetMap) ; la couleur de chaque station est calculée par le service à partir de ses incidents actifs, avec la même logique que RG3 ; un clic sur une station mène à ses équipements (F2).
- **Choix et essais** :
  - la carte devait être une page de supervision à part ; elle est finalement devenue un onglet « Carte » de la page Stations, à côté de l'onglet « Liste ». Nous l'avions d'abord ouverte aux deux profils, puis réservée au responsable, comme les niveaux d'attention qu'elle affiche (voir CHOIX TECHNIQUES, Profils et droits) ;
  - les deux onglets affichent les mêmes données (`SituationStation`), calculées une seule fois par `StationService` avec `PolitiqueUrgence` : la logique de couleur n'existe qu'en Java, le JavaScript ne fait que placer les points ;
  - pour transmettre les stations au JavaScript, nous avons écarté l'insertion directe d'objets Java par Thymeleaf (`th:inline="javascript"`), dont le résultat dépend de la bibliothèque de conversion JSON présente, au profit des attributs `data-*` de la liste affichée, qui sert en même temps de légende.
- **Ce qui fonctionne** : les 6 stations sont placées et colorées selon leur incident le plus urgent ; la bulle de chaque station donne sa situation, son nombre d'incidents en cours et un lien vers ses équipements ; la carte se cadre automatiquement sur l'ensemble des stations ; l'onglet Liste affiche les mêmes informations sous forme de tableau.
- **Difficultés** : Leaflet ne peut pas calculer la taille d'une carte cachée dans un onglet inactif ; c'est une des raisons pour lesquelles l'onglet est choisi par l'URL et la carte créée seulement quand elle est affichée.
- **Reste à faire** : la carte ne se met pas à jour toute seule (il faut recharger la page) ; filtrer les stations affichées par niveau serait utile avec un réseau plus grand.

### Piste 2 (visualisation des données) : prévue, en partie couverte
- **Besoin** : donner au responsable d'exploitation une vue d'ensemble de l'activité.
- **Ce qui a été fait** : la barre de répartition des incidents en cours par niveau, et les indicateurs du tableau de bord (dont le délai moyen de résolution calculé par la requête SQL native). Les agrégations sont faites côté serveur ; le navigateur ne fait qu'afficher.
- **Pourquoi elle n'a pas été menée jusqu'au bout** : la carte (piste 4), réalisée en premier, a pris la place de seconde fonctionnalité transverse ; le temps restant a été consacré au socle (F11, F12) et à la qualité de l'interface. Les graphiques prévus (répartition par station et par type, évolution des délais, notes des prestataires) restent à faire.

---

## BILAN

- Contribution de chacun : *à compléter*
- Plus grande difficulté et comment elle a été surmontée : *à compléter*
- Fonctionnalités non réalisées et pourquoi : *à compléter*
- Trois points à retenir du cours et du projet : *à compléter*
