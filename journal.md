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
| `DOWNLYZER_API_KEY` | clé d'accès à l'API REST pour les systèmes externes (piste 8) | à venir |

Sans `DB_USERNAME` ou `DB_PASSWORD`, l'application refuse de démarrer (`Could not resolve placeholder`).

### Lancer l'application
```bash
mvn spring-boot:run
```
Puis ouvrir http://localhost:8080. Documentation de l'API : http://localhost:8080/swagger-ui.html

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

### Application déployée
- URL : *à compléter*

### Limites connues
- Le script de données utilise `NOW()` de MariaDB et l'application l'heure de la JVM : les deux doivent être dans le **même fuseau horaire**, sinon les seuils sont décalés (un incident de 47 h pourrait apparaître orange).
- MariaDB 10.4 (fournie par XAMPP) n'est plus officiellement prise en charge par Hibernate 7, qui affiche un avertissement au démarrage (minimum 10.6). L'application fonctionne, mais le serveur de déploiement doit utiliser une version récente.
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
    Service --> Repository["repository<br/>Spring Data JPA"]
    Repository --> BDD[("MariaDB")]
```

Le même service est appelé par les deux types de contrôleurs : la logique métier n'est écrite qu'une fois.

Organisation des packages (`fr.efrei.downlyzer`) :

| Package | Contenu |
|---|---|
| `modele` | entités JPA et énumérations |
| `repository` | interfaces Spring Data JPA |
| `exception` | exceptions métier (`ExceptionMetier` et ses sous-classes) |
| `service` | règles de gestion RG1 à RG11 *(à venir)* |
| `controleur.web` | pages Thymeleaf et leur gestionnaire d'erreurs *(à venir)* |
| `controleur.api` | API REST et son gestionnaire d'erreurs *(à venir)* |

- **`controleur.web`** : les pages de l'application, utilisées par les deux profils. Après une modification, redirection (Post/Redirect/Get) ; en cas d'erreur, une page lisible avec le bon code HTTP.
- **`controleur.api`** : l'API REST destinée au monde extérieur (F12 et piste 8). Elle expose une partie seulement de la logique métier, renvoie du JSON (DTO, jamais les entités) et des codes HTTP adaptés (200, 201, 204, 400, 404, 409).

| Endpoint | Usage | Codes |
|---|---|---|
| `POST /api/incidents` | un capteur déclare une panne (par référence d'équipement) | 201, 400, 404, 409 |
| `GET /api/incidents` | consulter les incidents | 200 |
| `GET /api/incidents/{id}` | consulter un incident | 200, 404 |
| `PATCH /api/incidents/{id}/resolution` | signaler le retour en service | 204, 404, 409 |

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

Choix de modélisation :
- **L'indicateur vert / orange / rouge n'est pas stocké** : il est calculé à partir de l'heure courante comme la durée d'ouverture et la note moyenne d'un prestataire sont elles aussi calculées.
- **Le statut est stocké** (enum) pour la lisibilité des requêtes, et sa cohérence est garantie à deux niveaux : il n'est modifié que par des méthodes métier (`affecter()`, `resoudre()`), et une contrainte `CHECK` en base refuse toute combinaison incohérente //RG10.
- **`source` et `declarant_id` nullable** : un incident peut être déclaré par un capteur via l'API (piste 8), qui n'est pas un utilisateur humain.
- **`date_affectation`** : permet d'afficher depuis quand un incident est pris en charge et de mesurer les délais.
- **`version`** : verrouillage optimiste (`@Version`) contre les modifications concurrentes d'un même incident.
- **`prestataire_specialite`** : relie les prestataires aux types d'équipement sur lesquels ils interviennent (statistiques, et aide à l'affectation si le temps le permet).
- **Coordonnées des stations** : nécessaires à la cartographie (piste 4).

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

- Stratégie : les règles métier sont testées par des tests unitaires du service, sans base de données : les repositories sont simulés avec Mockito, et l'horloge est figée (`Clock.fixed`) pour que les durées ne dépendent pas du moment où les tests s'exécutent.
- Les pages sont testées avec `@WebMvcTest` et MockMvc : seuls les contrôleurs, les `@ControllerAdvice` et la configuration de sécurité sont démarrés, sans serveur ni base ; le service est simulé, car ses règles sont déjà couvertes par ses propres tests. On y vérifie ce qui relève du contrôleur : code HTTP, page affichée, redirection, validation du formulaire, accès et CSRF. L'utilisateur connecté est simulé avec `spring-security-test`.
- Commande : `mvn verify`
- Couvert :
  - service (`IncidentServiceTest`) : droits de modification de F6 (agent ou responsable, incident non affecté, affecté ou résolu), incident inexistant, durée d'ouverture (incident en cours ou résolu) ;
  - pages (`IncidentControleurTest`) : accès refusé sans connexion, détail d'un incident (F5) avec ou sans lien de modification, enregistrement d'une description valide, formulaire réaffiché si la description est trop courte, refus d'un envoi sans jeton CSRF ;
  - pages d'erreur : 404 (incident inexistant, identifiant non numérique, URL inconnue), 403, 409, et erreur imprévue affichée sans détail technique.

  *À compléter.*
- Non couvert : *à compléter*

---

## CHOIX TECHNIQUES

### Profils et droits

Deux profils :
- **Responsable d'exploitation** : gere le reseau : décide des affectations, valide les résolutions, évalue les prestataires et supervise la situation.
- **Agent de station** : sur le terrain : consulte les équipements et signale les pannes.

| N° | Fonctionnalité | Responsable d'exploitation | Agent de station |
|---|---|---|---|
| F2–F3 | Consulter équipements et fiches | Accès complet | Accès complet |
| F4 | Déclarer un incident | Accès complet | Accès complet |
| F5–F6 | Consulter / modifier un incident | Accès complet | Consultation ; modification de la description tant que l'incident n'est pas affecté |
| F7 | Affecter ou réaffecter un prestataire | Accès complet | Interdit |
| F8 | Déclarer un incident résolu | Accès complet | Interdit |
| F9 | Consulter l'historique | Accès complet | Lecture seule |
| F10 | Évaluer un prestataire | Accès complet | Interdit |
| F11 | Tableau de bord | Accès complet | Interdit |
| Alpha 2 et 4 | Page de supervision (graphiques, carte) | Accès complet | Interdit |

Justification :
- La résolution est réservée au responsable, car c'est elle qui ouvre l'évaluation du prestataire (RG8) : elle revient à celui qui gère les prestataires.
- L'agent peut préciser sa description tant que personne n'a pris l'incident en charge ; ensuite, le prestataire travaille sur une description stable.
- **F6 : seule la description est modifiable** (par le responsable jusqu'à la résolution, par l'agent tant que l'incident n'est pas affecté). Les autres informations ont chacune une raison de ne pas l'être :
  - le statut, le prestataire et les dates d'affectation et de résolution changent uniquement par F7 et F8, qui garantissent RG2 et RG5 : les modifier librement permettrait de contourner ces règles ;
  - la date d'ouverture sert au calcul de l'indicateur vert / orange / rouge (RG3) et des délais : la modifier permettrait de fausser la supervision ;
  - la source et le déclarant sont des faits (qui a signalé la panne, et comment), pas des informations à corriger ;
  - un incident résolu n'est plus modifiable : il fait partie de l'historique (F9) et peut avoir été évalué (F10).
- Les systèmes externes (capteurs) ne sont pas un profil de l'application : ils accèdent uniquement à `/api/**`, avec une clé d'API.
- Toutes les restrictions sont appliquées côté serveur (RG11) : par Spring Security pour l'accès aux routes, et dans la couche service pour les règles dépendant de l'état de l'incident.

### Deux types de contrôleurs
Les pages de l'application et l'API REST sont séparées en deux packages (`controleur.web` et `controleur.api`) :
- elles ne servent pas les mêmes clients (un humain dans un navigateur / un programme) ;
- elles ne répondent pas de la même manière (vue et redirection / JSON et codes HTTP) ;
- elles ne sont pas sécurisées de la même manière (formulaire de connexion et session / clé d'API sans session, réponse 401 en JSON) ;
- chacune a son propre gestionnaire d'exceptions, ciblé par package.

Une création depuis un formulaire renvoie une redirection (302, Post/Redirect/Get) et non un 201 : un navigateur ne suit pas l'en-tête `Location` d'un 201, et un rafraîchissement de la page renverrait le formulaire. Le 201 est réservé à l'API.

Les transitions d'état passent par des requêtes POST vers des URL d'action (`/incidents/{id}/affectation`, `/incidents/{id}/resolution`) plutôt que par PUT/DELETE simulés avec un champ caché : chaque action métier a sa propre adresse, et le `HiddenHttpMethodFilter` du TP n'est plus nécessaire.

### Configuration et accès à la base
- **Aucun secret dans le dépôt** (§7.7) : les identifiants de connexion sont lus dans des variables d'environnement (`${DB_USERNAME}`, `${DB_PASSWORD}`). Seule l'URL a une valeur par défaut, car elle n'est pas secrète.
- **Compte dédié à privilèges minimaux** : l'application se connecte avec `downlyzer_app`, qui n'a que `SELECT`, `INSERT`, `UPDATE` et `DELETE` sur `downlyzer`. Une faille dans l'application ne permettrait ni de supprimer des tables, ni de lire les autres bases du serveur.
- **`spring.jpa.hibernate.ddl-auto=validate`** : le schéma est créé uniquement par `db/create.sql`. Au démarrage, Hibernate vérifie que les entités correspondent aux tables, sans jamais modifier la base ; une erreur de mapping bloque le démarrage au lieu de passer inaperçue.
- **`spring.web.error.include-message` retiré** : utile pendant le dev, ce réglage pourrait afficher à l'écran le message d'une exception technique. Les messages destinés à l'utilisateur passent par le gestionnaire d'erreurs.

### Modèle de données en Java
- **Les entités simples** (`Station`, `TypeEquipement`, `Equipement`, `Prestataire`, `Utilisateur`, `Evaluation`) suivent le modèle JavaBean du TP : annotations JPA explicites, getters et setters. Exception : **aucun `setId()`**, car l'identifiant est attribué par la base ; le modifier pourrait faire écraser une autre ligne.
- **`Incident` n'expose pas de setters pour son état.** C'est la seule entité dont l'état obéit à des règles de gestion. Il est créé par `declarerManuellement(...)` ou `declarerParCapteur(...)`, qui garantissent RG1 (équipement et date d'ouverture obligatoires, statut `NON_AFFECTE`, aucun prestataire). Il évolue uniquement par `affecter()`, `resoudre()` et `modifierDescription()`, qui vérifient RG2 et RG5 et lèvent une `TransitionInterditeException` en cas de transition interdite. Ces règles sont donc écrites une seule fois, quelle que soit la porte d'entrée (pages, API, capteurs), et le statut, le prestataire et les dates changent toujours ensemble.
- **Les règles qui dépendent du profil connecté restent dans le service** : `Incident` interdit toute modification de la description une fois résolu, mais la règle « l'agent ne la modifie que tant que l'incident n'est pas affecté » sera vérifiée par le service, qui connaît l'utilisateur.
- **Un setter qui contiendrait une règle a été écarté** : son nom annoncerait un simple rangement de valeur alors qu'il refuserait ou modifierait d'autres champs, et l'ordre d'appel des setters deviendrait une règle cachée.
- **L'heure courante est passée en paramètre** des méthodes d'`Incident` : elle est fournie par le service à partir d'une horloge injectée (`Clock`), ce qui rend les règles testables.
- **Les énumérations sont stockées en texte** (`@Enumerated(EnumType.STRING)`) : ajouter une valeur à une énumération ne décale pas les données déjà enregistrées.
- **Relations à sens unique et chargement paresseux** (`FetchType.LAZY`) : `Equipement` connaît sa `Station`, mais `Station` n'a pas de liste d'équipements ; les listes sont obtenues par des requêtes du repository. On évite ainsi les chargements inutiles et les boucles lors de la conversion en JSON.

### Exceptions
- Exceptions métier personnalisées, non vérifiées, héritant d'une classe mère abstraite `ExceptionMetier` (fiche *Gestion des exceptions*), avec des messages en français qui indiquent l'objet concerné et la raison :
  - `RessourceIntrouvableException` (abstraite) et ses filles `StationIntrouvableException`, `EquipementIntrouvableException`, `IncidentIntrouvableException`, `UtilisateurIntrouvableException` ;
  - `TransitionInterditeException` : l'état de l'incident interdit l'opération (par exemple, modifier un incident résolu) ;
  - `ActionNonAutoriseeException` : l'opération est possible, mais pas pour le profil connecté, dans l'état actuel de l'incident (par exemple, un agent qui modifie un incident affecté).
- Traduction en réponses HTTP : un `@ControllerAdvice` par package de contrôleurs. Pour les pages, `GestionnaireExceptionsWeb` (limité à `controleur.web`) affiche la page `erreur.html` avec le message métier : **404** pour une ressource introuvable, **403** pour une action non autorisée, **409** pour une transition interdite.
  - Les contrôleurs ne contiennent ni `try/catch` ni test d'existence : ils appellent le service, et chaque exception est traduite à un seul endroit.
  - Gérer les classes mères (`RessourceIntrouvableException`) évite d'écrire un gestionnaire par entité : une nouvelle exception « introuvable » est prise en charge sans rien ajouter.
  - Un identifiant non numérique dans l'URL (`/incidents/abc`) est traité comme une ressource introuvable (**404**) plutôt que comme une requête invalide (400) : pour l'utilisateur, cette page n'existe pas.
- Les erreurs qui n'atteignent jamais un de nos contrôleurs (URL inconnue, accès refusé par Spring Security, exception imprévue) sont reprises par `ErreurControleur`, qui remplace la page « Whitelabel » de Spring Boot et affiche la même page `erreur.html`, avec un message générique en français : le détail technique d'une exception n'est jamais montré à l'utilisateur. Il est placé dans `controleur.web` pour que l'en-tête affiche toujours l'utilisateur connecté.
  - Limite : ce contrôleur répond toujours en HTML. Une URL inconnue sous `/api` recevra donc une page HTML ; à revoir avec l'API.
  - L'API aura son propre gestionnaire, qui répondra en JSON : les deux clients n'attendent pas le même format.

### Interface
- **Bootstrap 5.3.3**, chargé depuis un CDN comme dans le TP : mise en page cohérente sur toutes les pages (barre d'en-tête, cartes, tableaux, formulaires, alertes) sans écrire de CSS. Inconvénient : il faut un accès à Internet pour que les pages soient mises en forme.
- Les deux feuilles de style sont déclarées une seule fois, dans le fragment `styles` de `fragments/entete.html`, inclus par chaque page.
- `static/css/style.css` ne contient que ce qui est propre à Downlyzer, notamment la couleur des badges d'état et de statut. La classe d'un badge reprend le nom de la constante stockée en base (`etat-HORS_SERVICE`, `statut-NON_AFFECTE`…) : la couleur est donc choisie par la feuille de style, sans condition dans les templates.
- Les erreurs de saisie sont affichées sous le champ concerné, qui est entouré en rouge (`th:errorclass="is-invalid"`).

### SOLID
*À compléter* (principe(s) appliqué(s) : où, pourquoi ; principes écartés : pourquoi).

### Requête SQL native
*À compléter* (où, analyse critique).

### @Transactional
- **`IncidentService.modifierDescription` (F6)** : l'incident est chargé, vérifié puis modifié dans une seule transaction. Hibernate enregistre la modification de lui-même à la fin de la transaction, sans appel à `save()`. Le verrouillage optimiste (`@Version`) est vérifié au même moment : si quelqu'un a modifié l'incident entre son chargement et l'enregistrement, l'enregistrement est refusé (409) au lieu d'écraser son travail. Le formulaire ne transporte pas la version : une modification faite pendant que l'utilisateur remplit le formulaire n'est donc pas détectée.
- *À compléter* avec les opérations sur plusieurs tables (RG10).

### Points métier à trancher
- Déclarer un incident fait-il passer l'équipement hors service, et la résolution le remet-elle en service ? (si oui : opération sur deux tables, donc transactionnelle).
- Un capteur qui signale une panne sur un équipement ayant déjà un incident actif : 409 Conflict, ou renvoi de l'incident existant ?
- `spring.jpa.open-in-view` : le laisser actif (les relations paresseuses se chargent pendant l'affichage des pages) ou le désactiver (chaque service charge explicitement ce dont la vue a besoin, par exemple avec `JOIN FETCH`) ?
---

## LOT ALPHA

Fonctionnalités retenues : pistes **8** et **2**. La piste **4** est une réalisation supplémentaire.

### Fonctionnalité transverse 1 : source de données externe (piste 8)
- **Besoin** : des capteurs installés sur les équipements déclarent automatiquement les pannes, sans intervention humaine.
- **Périmètre envisagé** : endpoint `POST /api/incidents` sécurisé par clé d'API ; l'équipement est désigné par sa référence (le capteur ne connaît pas l'identifiant technique) ; un client de test simule plusieurs capteurs qui envoient des pannes.
- Choix et essais : *à compléter*
- Ce qui fonctionne : *à compléter*
- Difficultés : *à compléter*
- Reste à faire : *à compléter*

### Fonctionnalité transverse 2 : visualisation des données (piste 2)
- **Besoin** : donner au responsable d'exploitation une vue d'ensemble de l'activité.
- **Périmètre envisagé** : répartition des incidents par station et par type d'équipement, évolution des délais de résolution, note moyenne des prestataires. Les agrégations sont faites côté serveur (repository et service) ; le front ne fait qu'afficher.
- Choix et essais : *à compléter*
- Ce qui fonctionne : *à compléter*
- Difficultés : *à compléter*
- Reste à faire : *à compléter*

### Réalisation supplémentaire : cartographie du réseau (piste 4)
- **Besoin** : voir d'un coup d'œil où se trouvent les problèmes sur le réseau.
- **Périmètre envisagé** : carte des stations (Leaflet + OpenStreetMap) ; la couleur de chaque station est calculée par le service à partir de ses incidents actifs, avec la même logique que RG3 ; un clic sur une station mène à ses équipements (F2).
- Choix et essais : *à compléter*
- Ce qui fonctionne : *à compléter*
- Difficultés : *à compléter*
- Reste à faire : *à compléter*

---

## BILAN

- Contribution de chacun : *à compléter*
- Plus grande difficulté et comment elle a été surmontée : *à compléter*
- Fonctionnalités non réalisées et pourquoi : *à compléter*
- Trois points à retenir du cours et du projet : *à compléter*



















Utilisation de WebMvcTest pour tester les contrôleurs web, avec des mocks pour les services et un contexte Spring minimal.















