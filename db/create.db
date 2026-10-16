-- =====================================================================
-- Downlyzer : création de la base et des données de démonstration
-- SGBD : MariaDB 10.6+
-- Lancement : mariadb -u root -p < db/create.sql
--
-- Les dates sont calculées par rapport à NOW() au moment de l'exécution,
-- pour que les cas limites de RG3 (47 h, 48 h, 72 h) soient justes.
-- Relancer le script juste avant une démonstration pour les recaler.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS downlyzer
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE downlyzer;

DROP TABLE IF EXISTS evaluation;
DROP TABLE IF EXISTS incident;
DROP TABLE IF EXISTS prestataire_specialite;
DROP TABLE IF EXISTS equipement;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS prestataire;
DROP TABLE IF EXISTS type_equipement;
DROP TABLE IF EXISTS station;

-- ---------------------------------------------------------------------
-- Schéma
-- ---------------------------------------------------------------------

CREATE TABLE station (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    nom       VARCHAR(100) NOT NULL,
    code      VARCHAR(10)  NOT NULL,
    latitude  DOUBLE       NOT NULL,
    longitude DOUBLE       NOT NULL,
    CONSTRAINT pk_station PRIMARY KEY (id),
    CONSTRAINT uk_station_nom UNIQUE (nom),
    CONSTRAINT uk_station_code UNIQUE (code)
) ENGINE = InnoDB;

CREATE TABLE type_equipement (
    id      BIGINT      NOT NULL AUTO_INCREMENT,
    libelle VARCHAR(50) NOT NULL,
    CONSTRAINT pk_type_equipement PRIMARY KEY (id),
    CONSTRAINT uk_type_equipement_libelle UNIQUE (libelle)
) ENGINE = InnoDB;

CREATE TABLE equipement (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    reference          VARCHAR(20)  NOT NULL,
    libelle            VARCHAR(150) NOT NULL,
    etat               VARCHAR(20)  NOT NULL,
    station_id         BIGINT       NOT NULL,
    type_equipement_id BIGINT       NOT NULL,
    CONSTRAINT pk_equipement PRIMARY KEY (id),
    -- RG9 : la référence d'un équipement est unique
    CONSTRAINT uk_equipement_reference UNIQUE (reference),
    CONSTRAINT ck_equipement_etat CHECK (etat IN ('EN_SERVICE', 'DEGRADE', 'HORS_SERVICE')),
    CONSTRAINT fk_equipement_station FOREIGN KEY (station_id) REFERENCES station (id),
    CONSTRAINT fk_equipement_type FOREIGN KEY (type_equipement_id) REFERENCES type_equipement (id)
) ENGINE = InnoDB;

CREATE TABLE prestataire (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    nom       VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL,
    telephone VARCHAR(20)  NOT NULL,
    actif     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_prestataire PRIMARY KEY (id),
    CONSTRAINT uk_prestataire_nom UNIQUE (nom)
) ENGINE = InnoDB;

-- Types d'équipement sur lesquels un prestataire sait intervenir
-- (sert aux statistiques et, plus tard, à l'aide à l'affectation)
CREATE TABLE prestataire_specialite (
    prestataire_id     BIGINT NOT NULL,
    type_equipement_id BIGINT NOT NULL,
    CONSTRAINT pk_prestataire_specialite PRIMARY KEY (prestataire_id, type_equipement_id),
    CONSTRAINT fk_specialite_prestataire FOREIGN KEY (prestataire_id) REFERENCES prestataire (id),
    CONSTRAINT fk_specialite_type FOREIGN KEY (type_equipement_id) REFERENCES type_equipement (id)
) ENGINE = InnoDB;

CREATE TABLE utilisateur (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    login             VARCHAR(50) NOT NULL,
    mot_de_passe_hash VARCHAR(60) NOT NULL,
    prenom            VARCHAR(50) NOT NULL,
    nom               VARCHAR(50) NOT NULL,
    profil            VARCHAR(20) NOT NULL,
    actif             BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_utilisateur PRIMARY KEY (id),
    CONSTRAINT uk_utilisateur_login UNIQUE (login),
    CONSTRAINT ck_utilisateur_profil CHECK (profil IN ('RESPONSABLE', 'AGENT'))
) ENGINE = InnoDB;

CREATE TABLE incident (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    equipement_id          BIGINT        NOT NULL,
    declarant_id           BIGINT        NULL,
    source                 VARCHAR(20)   NOT NULL,
    description            VARCHAR(1000) NOT NULL,
    date_ouverture         DATETIME      NOT NULL,
    statut                 VARCHAR(20)   NOT NULL,
    prestataire_id         BIGINT        NULL,
    date_affectation       DATETIME      NULL,
    date_resolution        DATETIME      NULL,
    commentaire_resolution VARCHAR(1000) NULL,
    version                INT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_incident PRIMARY KEY (id),
    CONSTRAINT fk_incident_equipement FOREIGN KEY (equipement_id) REFERENCES equipement (id),
    CONSTRAINT fk_incident_declarant FOREIGN KEY (declarant_id) REFERENCES utilisateur (id),
    CONSTRAINT fk_incident_prestataire FOREIGN KEY (prestataire_id) REFERENCES prestataire (id),
    CONSTRAINT ck_incident_source CHECK (source IN ('MANUELLE', 'CAPTEUR')),
    -- Un incident saisi à la main a forcément un déclarant ; un capteur n'en a pas
    CONSTRAINT ck_incident_declarant CHECK (source = 'CAPTEUR' OR declarant_id IS NOT NULL),
    -- RG2 : non affecté, affecté ou résolu, avec des colonnes cohérentes avec le statut
    CONSTRAINT ck_incident_statut CHECK (
        (statut = 'NON_AFFECTE' AND prestataire_id IS NULL AND date_resolution IS NULL)
        OR (statut = 'AFFECTE' AND prestataire_id IS NOT NULL AND date_affectation IS NOT NULL
            AND date_resolution IS NULL)
        OR (statut = 'RESOLU' AND date_resolution IS NOT NULL)
    ),
    CONSTRAINT ck_incident_date_affectation CHECK (date_affectation IS NULL OR date_affectation >= date_ouverture),
    -- RG5 : la résolution est postérieure ou égale à l'ouverture
    CONSTRAINT ck_incident_date_resolution CHECK (date_resolution IS NULL OR date_resolution >= date_ouverture)
) ENGINE = InnoDB;

-- Le tableau de bord filtre sur le statut, l'historique trie sur la date de résolution
CREATE INDEX idx_incident_statut ON incident (statut);
CREATE INDEX idx_incident_date_resolution ON incident (date_resolution);

-- RG8 : une seule évaluation par incident, note entière de 1 à 5.
-- « Incident résolu et affecté » est vérifié dans la couche service (règle sur une autre table).
CREATE TABLE evaluation (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    incident_id     BIGINT        NOT NULL,
    auteur_id       BIGINT        NOT NULL,
    note            INT           NOT NULL,
    commentaire     VARCHAR(1000) NULL,
    date_evaluation DATETIME      NOT NULL,
    CONSTRAINT pk_evaluation PRIMARY KEY (id),
    CONSTRAINT uk_evaluation_incident UNIQUE (incident_id),
    CONSTRAINT ck_evaluation_note CHECK (note BETWEEN 1 AND 5),
    CONSTRAINT fk_evaluation_incident FOREIGN KEY (incident_id) REFERENCES incident (id),
    CONSTRAINT fk_evaluation_auteur FOREIGN KEY (auteur_id) REFERENCES utilisateur (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Données de démonstration
-- ---------------------------------------------------------------------

INSERT INTO station (id, nom, code, latitude, longitude) VALUES
    (1, 'Gare Centrale',          'GCE', 48.8443, 2.3740),
    (2, 'Hôtel de Ville',         'HDV', 48.8566, 2.3522),
    (3, 'Les Tanneurs',           'TAN', 48.8722, 2.3488),
    (4, 'Université',             'UNI', 48.8462, 2.3442),
    (5, 'Parc des Expositions',   'PEX', 48.8323, 2.2875),
    (6, 'Port Fluvial',           'PFL', 48.8310, 2.3780);

INSERT INTO type_equipement (id, libelle) VALUES
    (1, 'Ascenseur'),
    (2, 'Escalier mécanique'),
    (3, 'Portique de validation'),
    (4, 'Distributeur de titres'),
    (5, 'Écran d''information');

INSERT INTO equipement (id, reference, libelle, etat, station_id, type_equipement_id) VALUES
    (1,  'ASC-001', 'Ascenseur quai A vers hall',              'HORS_SERVICE', 1, 1),
    (2,  'ESC-001', 'Escalier mécanique sortie Nord',          'HORS_SERVICE', 1, 2),
    (3,  'POR-001', 'Ligne de portiques entrée principale',    'DEGRADE',      1, 3),
    (4,  'DIS-001', 'Distributeur hall principal',             'EN_SERVICE',   1, 4),
    (5,  'ASC-002', 'Ascenseur parvis',                        'HORS_SERVICE', 2, 1),
    (6,  'ECR-001', 'Écran des départs, hall',                 'HORS_SERVICE', 2, 5),
    (7,  'DIS-002', 'Distributeur sortie Ouest', 'HORS_SERVICE', 2, 4),
    (8,  'ESC-002', 'Escalier mécanique quai B',               'HORS_SERVICE', 3, 2),
    (9,  'POR-002', 'Portiques sortie Est',                    'DEGRADE',      3, 3),
    (10, 'ECR-002', 'Écran quai direction Port Fluvial',       'EN_SERVICE',   3, 5),
    (11, 'ASC-003', 'Ascenseur accès PMR',                     'HORS_SERVICE', 4, 1),
    (12, 'DIS-003', 'Distributeur entrée campus',              'HORS_SERVICE', 4, 4),
    (13, 'ESC-003', 'Escalier mécanique vers les halls',       'HORS_SERVICE', 5, 2),
    (14, 'ECR-003', 'Écran d''accueil visiteurs',              'EN_SERVICE',   5, 5),
    (15, 'ASC-004', 'Ascenseur quai unique',                   'EN_SERVICE',   6, 1),
    (16, 'POR-003', 'Portiques entrée unique',                 'EN_SERVICE',   6, 3);

INSERT INTO prestataire (id, nom, email, telephone, actif) VALUES
    (1, 'Ascenso Maintenance',  'contact@ascenso-maintenance.example',  '01 45 10 20 30', TRUE),
    (2, 'MécaFlux Services',    'interventions@mecaflux.example',       '01 46 22 33 44', TRUE),
    (3, 'ValiTech Contrôle',    'support@valitech.example',             '01 47 55 66 77', TRUE),
    (4, 'InfoAffichage Pro',    'sav@infoaffichage.example',            '01 48 88 99 00', TRUE),
    (5, 'Rapid''Intervention',  'astreinte@rapid-intervention.example', '06 12 34 56 78', TRUE);

INSERT INTO prestataire_specialite (prestataire_id, type_equipement_id) VALUES
    (1, 1), (1, 2),
    (2, 2), (2, 1),
    (3, 3), (3, 4),
    (4, 5), (4, 4),
    (5, 1), (5, 2), (5, 3), (5, 4), (5, 5);

-- Mots de passe hachés en BCrypt (coût 10). Valeurs en clair : voir journal.md, section README.
INSERT INTO utilisateur (id, login, mot_de_passe_hash, prenom, nom, profil, actif) VALUES
    (1, 'claire.martin', '$2a$10$hjrlH0OPEVPjv0IEih8RfOkT0/wfNv97IIX0X8jybW4Zs5r0X/6m2', 'Claire', 'Martin', 'RESPONSABLE', TRUE),
    (2, 'karim.dupont',  '$2a$10$P/JN2yUPsYtBA1/jbo4oZuyewG5TJlUhp.yWqmERc9B0w/Fekuw/6', 'Karim',  'Dupont', 'AGENT',       TRUE);

-- Incidents actifs non affectés : un par cas limite de RG3
INSERT INTO incident (id, equipement_id, declarant_id, source, description, date_ouverture, statut) VALUES
    (1, 6,  2,    'MANUELLE', 'Écran éteint, aucun affichage des départs.',
        NOW() - INTERVAL 2 HOUR,                       'NON_AFFECTE'),  -- vert
    (2, 7,  NULL, 'CAPTEUR',  'Monnayeur bloqué, paiement en espèces impossible.',
        NOW() - INTERVAL 47 HOUR,                      'NON_AFFECTE'),  -- vert (47 h)
    (3, 3,  2,    'MANUELLE', 'Deux portiques sur cinq refusent les titres valides.',
        NOW() - INTERVAL 2879 MINUTE,                  'NON_AFFECTE'),  -- vert (47 h 59)
    (4, 12, 2,    'MANUELLE', 'Écran tactile du distributeur ne réagit plus.',
        NOW() - INTERVAL 48 HOUR,                      'NON_AFFECTE'),  -- orange (48 h pile)
    (5, 9,  NULL, 'CAPTEUR',  'Portique bloqué en position fermée.',
        NOW() - INTERVAL 60 HOUR,                      'NON_AFFECTE'),  -- orange
    (6, 13, 2,    'MANUELLE', 'Escalier arrêté, bruit anormal avant l''arrêt.',
        NOW() - INTERVAL 4319 MINUTE,                  'NON_AFFECTE'),  -- orange (71 h 59)
    (7, 8,  1,    'MANUELLE', 'Marche cassée en haut de l''escalier, escalier coupé.',
        NOW() - INTERVAL 72 HOUR,                      'NON_AFFECTE'),  -- rouge (72 h pile)
    (8, 11, 2,    'MANUELLE', 'Ascenseur PMR bloqué au niveau -1.',
        NOW() - INTERVAL 5 DAY,                        'NON_AFFECTE');  -- rouge

-- Incidents actifs affectés : jamais vert/orange/rouge (RG4), même ouverts depuis longtemps
INSERT INTO incident (id, equipement_id, declarant_id, source, description, date_ouverture, statut,
                      prestataire_id, date_affectation) VALUES
    (9,  1, 2,    'MANUELLE', 'Ascenseur à l''arrêt entre deux niveaux, personne bloquée dégagée.',
         NOW() - INTERVAL 80 HOUR, 'AFFECTE', 1, NOW() - INTERVAL 70 HOUR),
    (10, 2, NULL, 'CAPTEUR',  'Arrêt d''urgence déclenché, redémarrage impossible.',
         NOW() - INTERVAL 10 HOUR, 'AFFECTE', 2, NOW() - INTERVAL 3 HOUR),
    (11, 5, 2,    'MANUELLE', 'Porte palière qui ne se referme pas.',
         NOW() - INTERVAL 50 HOUR, 'AFFECTE', 5, NOW() - INTERVAL 20 HOUR);

-- Incidents résolus : historique varié pour l'historique (F9) et les graphiques
INSERT INTO incident (id, equipement_id, declarant_id, source, description, date_ouverture, statut,
                      prestataire_id, date_affectation, date_resolution, commentaire_resolution) VALUES
    (12, 4,  2,    'MANUELLE', 'Bourrage de billets dans le distributeur.',
         NOW() - INTERVAL 28 DAY, 'RESOLU', 3, NOW() - INTERVAL 28 DAY + INTERVAL 2 HOUR,
         NOW() - INTERVAL 27 DAY, 'Mécanisme de distribution nettoyé.'),
    (13, 15, NULL, 'CAPTEUR',  'Capteur de porte en défaut.',
         NOW() - INTERVAL 25 DAY, 'RESOLU', 1, NOW() - INTERVAL 25 DAY + INTERVAL 1 HOUR,
         NOW() - INTERVAL 22 DAY, 'Capteur remplacé.'),
    (14, 10, 2,    'MANUELLE', 'Affichage figé sur une ancienne information.',
         NOW() - INTERVAL 20 DAY, 'RESOLU', 4, NOW() - INTERVAL 20 DAY + INTERVAL 5 HOUR,
         NOW() - INTERVAL 19 DAY, 'Mise à jour logicielle de l''écran.'),
    (15, 13, 1,    'MANUELLE', 'Main courante désynchronisée.',
         NOW() - INTERVAL 16 DAY, 'RESOLU', 2, NOW() - INTERVAL 15 DAY,
         NOW() - INTERVAL 12 DAY, 'Courroie de main courante changée.'),
    (16, 16, NULL, 'CAPTEUR',  'Lecteur de titres hors service sur un portique.',
         NOW() - INTERVAL 12 DAY, 'RESOLU', 3, NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR,
         NOW() - INTERVAL 11 DAY, 'Lecteur reconfiguré.'),
    (17, 14, 2,    'MANUELLE', 'Écran qui clignote par intermittence.',
         NOW() - INTERVAL 9 DAY,  'RESOLU', 5, NOW() - INTERVAL 8 DAY,
         NOW() - INTERVAL 6 DAY,  'Alimentation remplacée.'),
    (18, 15, 2,    'MANUELLE', 'Bouton d''appel du niveau quai inopérant.',
         NOW() - INTERVAL 6 DAY,  'RESOLU', 5, NOW() - INTERVAL 6 DAY + INTERVAL 4 HOUR,
         NOW() - INTERVAL 4 DAY,  'Bouton d''appel remplacé.'),
    (19, 4,  2,    'MANUELLE', 'Distributeur affiché hors service par erreur.',
         NOW() - INTERVAL 3 DAY,  'RESOLU', NULL, NULL,
         NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR, 'Fausse alerte : redémarrage du terminal, aucune intervention.'),
    (20, 16, NULL, 'CAPTEUR',  'Portique resté ouvert en permanence.',
         NOW() - INTERVAL 2 DAY,  'RESOLU', 3, NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR,
         NOW() - INTERVAL 1 DAY,  'Moteur du vantail recalé.');

-- Évaluations : volontairement absentes sur les incidents 18 et 20 pour pouvoir démontrer F10.
-- L'incident 19, résolu sans prestataire, n'est pas évaluable (RG8).
INSERT INTO evaluation (id, incident_id, auteur_id, note, commentaire, date_evaluation) VALUES
    (1, 12, 1, 5, 'Intervention rapide et propre.',             NOW() - INTERVAL 27 DAY),
    (2, 13, 1, 3, 'Délai un peu long pour un simple capteur.',  NOW() - INTERVAL 22 DAY),
    (3, 14, 1, 4, NULL,                                          NOW() - INTERVAL 19 DAY),
    (4, 15, 1, 2, 'Trois jours d''arrêt, peu de communication.', NOW() - INTERVAL 12 DAY),
    (5, 16, 1, 5, NULL,                                          NOW() - INTERVAL 11 DAY),
    (6, 17, 1, 4, 'Bon diagnostic.',                             NOW() - INTERVAL 6 DAY);
