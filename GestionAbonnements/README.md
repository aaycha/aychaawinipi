# Gestion des Abonnements et Participations - Version Professionnelle

## 📋 Description

Ce projet est une application JavaFX professionnelle pour la gestion avancée des abonnements et participations avec intégration d'intelligence artificielle pour les recommandations personnalisées.

### 🎯 Fonctionnalités Principales

- **Gestion des Abonnements** : CRUD avancé avec auto-renew, points accumulés, et gestion du churn
- **Gestion des Participations** : Inscriptions aux événements avec hébergements, contextes sociaux, et badges
- **Recommandations IA** : Algorithmes de ML (Collaborative Filtering, NLP, TensorFlow) pour suggestions personnalisées
- **Analytics** : Tableaux de bord, rapports, et métriques de performance
- **Interface JavaFX** : Moderne, responsive, avec recherche, tri, et filtrage avancé

### 🤖 Intelligence Artificielle Intégrée

- **Collaborative Filtering** : Recommandations basées sur la similarité entre utilisateurs
- **NLP (Natural Language Processing)** : Analyse des descriptions pour détecter le contexte social
- **TensorFlow/ML** : Prédiction de conversion et détection de churn
- **Web Scraping** : Récupération automatique des prix et disponibilités

### 🏗️ Architecture

```
src/main/java/com/gestion/
├── MainApplication.java              # Point d'entrée JavaFX
├── controllers/
│   └── MainController.java          # Contrôleur principal de l'interface
├── entities/
│   ├── Abonnement.java             # Entité Abonnement
│   ├── Participation.java          # Entité Participation
│   └── Recommandation.java       # Entité Recommandation IA
├── interfaces/
│   ├── AbonnementService.java       # Interface service abonnements
│   ├── ParticipationService.java    # Interface service participations
│   └── RecommandationService.java # Interface service IA
├── services/
│   ├── AbonnementServiceImpl.java   # Implémentation service abonnements
│   ├── ParticipationServiceImpl.java # Implémentation service participations
│   └── RecommandationServiceImpl.java # Implémentation service IA
└── tools/
    └── DBConnection.java          # Gestion connexion base de données

src/main/resources/
├── views/
│   └── main-view.fxml            # Interface principale FXML
├── styles/
│   └── main.css                 # Styles CSS modernes
└── images/
    └── logo.png                 # Logo application
```

## 🚀 Démarrage Rapide

### Prérequis

- Java 17 ou supérieur
- Maven 3.6+
- MySQL 8.0+
- Scene Builder (pour modifier les FXML)
- IDE IntelliJ IDEA (recommandé)

### Installation

1. **Cloner le projet**
   ```bash
   git clone <repository-url>
   cd GestionAbonnements
   ```

2. **Configurer la base de données**
   ```sql
   CREATE DATABASE lamma_db;
   -- Exécuter le script SQL fourni dans docs/database.sql
   ```

3. **Configurer la connexion**
   - Modifier `DBConnection.java` si nécessaire
   - Par défaut : MySQL sur localhost:3306/lamma_db

4. **Compiler et exécuter**
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

### Configuration IntelliJ

1. Ouvrir le projet dans IntelliJ
2. Configurer JDK 17
3. Importer les dépendances Maven
4. Créer une configuration d'exécution :
   - Main class : `com.gestion.MainApplication`
   - VM options : `--module-path /path/to/javafx/sdk/lib --add-modules javafx.controls,javafx.fxml`

## 📊 Base de Données

### Schéma Principal

```sql
-- Table des abonnements
CREATE TABLE abonnements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('MENSUEL', 'ANNUEL', 'PREMIUM') NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    prix DECIMAL(10,2) NOT NULL,
    statut ENUM('ACTIF', 'EXPIRE', 'SUSPENDU', 'EN_ATTENTE') NOT NULL,
    avantages JSON,
    auto_renew BOOLEAN DEFAULT FALSE,
    points_accumules INT DEFAULT 0,
    churn_score FLOAT DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table des participations
CREATE TABLE participations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    evenement_id BIGINT NOT NULL,
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type ENUM('SIMPLE', 'HEBERGEMENT', 'GROUPE') NOT NULL,
    statut ENUM('EN_ATTENTE', 'CONFIRME', 'ANNULE', 'EN_LISTE_ATTENTE') NOT NULL,
    hebergement_nuits INT DEFAULT 0,
    contexte_social ENUM('COUPLE', 'AMIS', 'FAMILLE', 'SOLO', 'PROFESSIONNEL'),
    badge_associe VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table des recommandations
CREATE TABLE recommandations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    evenement_suggere_id BIGINT NOT NULL,
    score FLOAT NOT NULL CHECK (score >= 0 AND score <= 1),
    raison TEXT,
    algorithme_used ENUM('COLLABORATIVE', 'CONTENT_BASED', 'NLP', 'HYBRIDE', 'ML_TENSORFLOW', 'CLUSTERING'),
    equipement_bundle JSON,
    source_scraped VARCHAR(500),
    date_generation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP NOT NULL,
    est_utilisee BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🎨 Interface Utilisateur

### Vue Principale

L'interface est divisée en 4 sections principales :

1. **Abonnements**
   - Tableau avec tous les abonnements
   - Filtres par statut et type
   - Actions : Créer, Modifier, Supprimer
   - Pagination et recherche

2. **Participations**
   - Gestion des inscriptions aux événements
   - Contextes sociaux (couple, amis, famille, etc.)
   - Badges et gamification
   - Gestion des hébergements

3. **Recommandations IA**
   - Visualisation des suggestions IA
   - Filtres par algorithme et score
   - Détails des bundles d'équipements
   - Génération manuelle et automatique

4. **Analytics**
   - Statistiques générales
   - Performance des algorithmes IA
   - Top utilisateurs
   - Rapports exportables

### Fonctionnalités de l'Interface

- **Recherche en temps réel** : Sur tous les champs
- **Tri avancé** : Par date, score, statut, etc.
- **Filtrage multiple** : Combinaison de filtres
- **Pagination** : Navigation efficace dans grands datasets
- **Export** : CSV, PDF, JSON
- **Notifications** : Toast et alerts informatives

## 🤖 Algorithmes de Recommandation

### 1. Collaborative Filtering

Basé sur la similarité entre utilisateurs :

```java
// Similarité cosinus
double similarite = calculerSimilariteCosinus(user1, user2);

// Score de recommandation
double score = similarite * popularite_evenement;
```

### 2. NLP (Natural Language Processing)

Analyse des descriptions utilisateur :

```java
// Détection du contexte social
String contexte = analyserContexteSocial("weekend romantique avec ma copine");
// Résultat : "COUPLE"

// Extraction de mots-clés
List<String> motsCles = extraireMotsCles("camping montagne barbecue");
// Résultat : ["camping", "montagne", "barbecue"]
```

### 3. Content-Based Filtering

Basé sur l'historique et les préférences :

```java
// Similarité de contenu
double score = calculerSimilariteContenu(evenement, historique_user);
```

### 4. Hybrid Approach

Combinaison pondérée des algorithmes :

```java
double scoreFinal = 
    collaborative * 0.4 + 
    contentBased * 0.3 + 
    nlp * 0.3;
```

## 🎯 Contextes Sociaux et Équipements

### Pour Couple

- **Tente** : 2 places avec matelas queen-size
- **Extras** : Ambiance romantique, guirlandes LED
- **Loisirs** : Pique-nique romantique, spa portable

### Pour Amis

- **Tente** : 5+ personnes spacieuse
- **Extras** : Barbecue professionnel, glacière grande
- **Loisirs** : Jeux de société, sélection de vins

### Pour Famille

- **Tente** : Familiale 6+ personnes
- **Extras** : Activités enfants, sécurité
- **Loisirs** : Jeux familiaux, matelas épais

## 📈 Analytics et Métriques

### KPIs Principaux

- **Taux de conversion** : Participations / recommandations
- **Score moyen par algorithme** : Performance IA
- **Taux de rétention** : Abonnements renouvelés
- **Churn prediction** : Risque d'abandon

### Rapports

- **Mensuels** : Évolution des abonnements
- **Performance IA** : Comparaison algorithmes
- **Utilisateurs** : Top par points et participations
- **Financiers** : Revenus et prédictions

## 🔧 Développement

### Standards de Code

- **Java 17** : Features modernes (records, switch expressions)
- **JavaFX 17** : Interface moderne et responsive
- **Maven** : Gestion des dépendances
- **MySQL** : Base de données relationnelle
- **Logback** : Logging structuré

### Patterns Utilisés

- **Singleton** : DBConnection
- **Service Layer** : Séparation logique/métier
- **Repository Pattern** : Accès données
- **Observer Pattern** : Notifications UI
- **Strategy Pattern** : Algorithmes IA

### Tests

```bash
# Exécuter les tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Couverture de code
mvn jacoco:report
```

## 🚀 Déploiement

### Build de Production

```bash
# Nettoyage et compilation
mvn clean package

# Création du JAR exécutable
mvn javafx:jlink
```

### Configuration Production

1. **Base de données** : Configurer les accès production
2. **Logging** : Niveau INFO ou WARN
3. **Mémoire** : JVM args optimisés
4. **Sécurité** : HTTPS et authentification

## 🔍 Maintenance

### Monitoring

- **Logs** : Surveillance des erreurs
- **Performance** : Temps de réponse
- **Base de données** : Connexions et requêtes
- **Mémoire** : Usage JVM

### Sauvegarde

```bash
# Base de données
mysqldump -u root -p lamma_db > backup.sql

# Logs
tar -czf logs_backup.tar.gz logs/
```

## 🐛 Dépannage

### Problèmes Communs

1. **Connexion DB échoue**
   - Vérifier MySQL démarré
   - Configurer `DBConnection.java`

2. **JavaFX ne démarre pas**
   - Vérifier les modules JavaFX
   - Configurer le module-path

3. **Maven dependencies**
   - `mvn clean install`
   - Vérifier settings.xml

### Logs

```bash
# Logs application
tail -f logs/application.log

# Logs erreurs
grep ERROR logs/application.log
```

## 🏔️ LAMA EXPEDITION - Management System

**LAMA EXPEDITION** is a premium, all-in-one management platform designed for scouting adventures, trekking, and nature expeditions. Built with JavaFX and powered by AI, it offers a seamless experience for both administrators and explorers.

---

## 🌟 Key Features

### 🛠️ Admin Dashboard
*   **Abonnements**: Complete management of monthly, annual, and premium subscriptions.
*   **Participations**: Track event registrations and activity participation.
*   **Événements**: Manage treks, campfire nights, and outdoor stays.
*   **Restauration**: Specialized module for menu planning, inventory (ingredients), and base camp rations.
*   **Boutique & Inventaire**: Manage trekking equipment inventory, sales, and rentals.
*   **Analytics**: Real-time statistics and performance tracking for business optimization.
*   **Infrastructure**: Automatic database schema management and table initialization.

### ⛺ User Expedition Hub (Espace Utilisateur)
*   **Mission Control**: Personal dashboard to track "Expedition Data" and "Trail Status."
*   **AI Satellite Scout**: Integrated AI (powered by Gemini) for visual analysis and smart recommendations.
*   **Boutique LAMA**: User-facing store to buy or rent trekking gear.
*   **Messagerie Chat**: Real-time communication system for community explorers.
*   **Weather Widget**: Real-time weather integration (currently tracking Tunis/Expedition base).
*   **TikTok Intel**: Integrated search for viral expedition feedback and viral scouting spots.

---

## 💻 Technology Stack

*   **Core**: Java 11
*   **UI Framework**: JavaFX 17+ (Premium Nature-inspired design)
*   **Build Tool**: Maven
*   **Database**: MySQL 8.0+
*   **AI Integration**: Google Gemini API (Vision & Recommendations)
*   **Persistence**: JDBC with custom `MyConnection` singleton
*   **External APIs**:
    *   **Stripe**: Payment processing for gear and subscriptions.
    *   **SendGrid**: Automated email notifications (Invoices, Registrations).
    *   **Google Calendar**: Event synchronization.
    *   **Weather API**: Real-time local trail conditions.

---

## 📂 Project Structure

```text
src/main/java/com/gestion/
├── api/            # Integration with external REST APIs
├── controllers/    # Shared and Admin UI controllers
├── entities/       # Data models (Abonnement, Equipement, etc.)
├── services/       # Business logic and database interactions
├── tools/          # Utilities (Connection, Session, Email)
└── ui/             # Categorized UI controllers (utilisateur, menu, etc.)

src/main/resources/
├── views/          # FXML layouts organized by module
├── styles/         # CSS themes (Nature-mood, Dashboard-premium)
└── images/         # Assets and expedition icons
```

---

## 🚀 Getting Started

### Prerequisites
*   **Java 11 JDK**
*   **Maven**
*   **MySQL Server**

### Installation

1.  **Database Configuration**:
    Create a database named `lama` in your MySQL server.
    Update the credentials in `src/main/java/com/gestion/tools/MyConnection.java`:
    ```java
    private final String URL = "jdbc:mysql://localhost:3306/lama";
    private final String LOGIN = "root";
    private final String PWD = "";
    ```

2.  **Environment Variables**:
    Ensure the following keys are configured (or updated in relevant Service classes):
    *   `SENDGRID_API_KEY`: For email automation.

3.  **Build**:
    ```bash
    mvn clean install
    ```

4.  **Run**:
    ```bash
    mvn javafx:run
    ```

---

## 🎨 Design Philosophy

LAMA uses a "Nature Glass" aesthetic, combining:
*   **Glassmorphism**: Transparent, blurred panels for a modern look.
*   **Organic Palette**: Earthy greens, mountain blues, and sky accents.
*   **Micro-animations**: Smooth transitions and hover effects for a premium feel.

---

## 👨‍💻 Contributing

1.  Create a feature branch.
2.  Commit your changes following the Nature-Scout naming convention.
3.  Push to the branch.
4.  Open a Pull Request.

---

*“LAMA EXPEDITION - Log your mountain mission data and check trail status.”*

## 📚 Documentation Complémentaire

### API Documentation

- **Javadoc** : Généré avec `mvn javadoc:javadoc`
- **OpenAPI** : Spécifications REST
- **Database** : Schéma ERD dans docs/

### Guides

- **Développeur** : `docs/DEVELOPER.md`
- **Utilisateur** : `docs/USER.md`
- **Admin** : `docs/ADMIN.md`

## 🤝 Contribution

### Workflow

1. Forker le projet
2. Créer une branche feature
3. Développer et tester
4. Pull request avec description

### Standards

- **Code style** : Google Java Style
- **Commits** : Messages clairs et structurés
- **Tests** : Couverture minimale 80%
- **Documentation** : Javadoc pour les APIs

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 👥 Contact

- **Développeur principal** : [Votre Nom]
- **Email** : [votre.email@example.com]
- **GitHub** : [votre-profile]

---

**Version** : 1.0.0  
**Dernière mise à jour** : [Date]  
**Java** : 17  
**JavaFX** : 17.0.2
