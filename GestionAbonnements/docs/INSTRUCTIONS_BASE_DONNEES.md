# Instructions pour la Base de Données LAMMA

## 📋 Fichier SQL Complet

Le fichier `database_complet.sql` contient :
- ✅ Toutes les tables nécessaires
- ✅ Toutes les vues corrigées
- ✅ Tous les triggers corrigés
- ✅ Données de test complètes (utilisateurs, événements, abonnements, participations, restauration)
- ✅ Procédures stockées

## 🚀 Installation de la Base de Données

### Étape 1 : Créer la Base de Données

1. Ouvrez MySQL (via phpMyAdmin, MySQL Workbench, ou ligne de commande)
2. Exécutez le fichier `database_complet.sql` complet

**Via ligne de commande :**
```bash
mysql -u root -p < database_complet.sql
```

**Via phpMyAdmin :**
1. Connectez-vous à phpMyAdmin
2. Cliquez sur l'onglet "SQL"
3. Copiez-collez tout le contenu de `database_complet.sql`
4. Cliquez sur "Exécuter"

### Étape 2 : Vérifier la Connexion

Le fichier `MyConnection.java` est configuré pour :
- **URL** : `jdbc:mysql://localhost:3306/lamma_db`
- **Utilisateur** : `root`
- **Mot de passe** : (vide par défaut)

Si votre configuration MySQL est différente, modifiez `MyConnection.java` :
```java
private static final String URL = "jdbc:mysql://localhost:3306/lamma_db";
private static final String LOGIN = "root";  // Votre utilisateur MySQL
private static final String PWD = "";        // Votre mot de passe MySQL
```

## 📊 Données de Test Incluses

Le fichier SQL inclut des données de test pour :

### Utilisateurs (5 utilisateurs)
- Admin Système
- Mohamed Ben Ali
- Sonia Trabelsi
- Karim Mejri
- Leila Khaled

### Événements (5 événements)
- Camping Ain Draham
- Randonnée Zaghouan
- Soirée Saint Valentin
- Fuego Beach Party
- Camping Djerba

### Abonnements (4 abonnements)
- Différents types : MENSUEL, ANNUEL, PREMIUM
- Différents statuts : ACTIF, SUSPENDU

### Participations (5 participations)
- Différents types : SIMPLE, HEBERGEMENT, GROUPE
- Différents contextes sociaux : COUPLE, AMIS, SOLO, FAMILLE

### Restauration
- **8 options de restauration** (SOIREE, RANDONNEE, CAMPING)
- **8 menus de proposition**
- **7 restrictions alimentaires**
- **4 besoins de restauration pour participants**
- **4 repas**
- **6 présences**

## 🔧 Résolution des Problèmes

### Problème : Les interfaces sont vides

**Causes possibles :**
1. La base de données n'existe pas ou n'a pas de données
   - **Solution** : Exécutez `database_complet.sql` pour créer la base et insérer les données

2. Le nom de la base de données ne correspond pas
   - **Solution** : Vérifiez que `MyConnection.java` utilise le même nom que dans le fichier SQL
   - Par défaut : `lamma_db`

3. La connexion MySQL échoue
   - **Solution** : Vérifiez que MySQL est démarré
   - Vérifiez les identifiants dans `MyConnection.java`

4. Les tables n'existent pas
   - **Solution** : Exécutez le fichier SQL complet

### Vérification Rapide

Pour vérifier que tout fonctionne, exécutez ces requêtes SQL :

```sql
-- Vérifier les utilisateurs
SELECT COUNT(*) FROM utilisateurs;
-- Devrait retourner 5

-- Vérifier les événements
SELECT COUNT(*) FROM evenement;
-- Devrait retourner 5

-- Vérifier les options de restauration
SELECT COUNT(*) FROM option_restauration;
-- Devrait retourner 8

-- Vérifier les menus
SELECT COUNT(*) FROM menu_proposition;
-- Devrait retourner 8
```

## 📝 Notes Importantes

1. **Nom de la base de données** : Le fichier SQL crée `lamma_db`. Si vous avez déjà une base avec un autre nom, modifiez :
   - Le fichier SQL (ligne 8)
   - Le fichier `MyConnection.java` (ligne 73)

2. **Données de test** : Les données de test utilisent `INSERT IGNORE`, donc elles ne seront pas dupliquées si vous réexécutez le script.

3. **Colonne type_evenement** : Le code gère maintenant le cas où cette colonne n'existe pas dans `option_restauration`. Si vous avez une ancienne base sans cette colonne, le code fonctionnera quand même.

## ✅ Checklist de Vérification

Avant de lancer l'application, vérifiez :

- [ ] MySQL est démarré
- [ ] Le fichier `database_complet.sql` a été exécuté
- [ ] La base de données `lamma_db` existe
- [ ] Les tables sont créées (vérifier avec `SHOW TABLES;`)
- [ ] Les données de test sont présentes
- [ ] `MyConnection.java` utilise le bon nom de base de données
- [ ] `MyConnection.java` utilise les bons identifiants MySQL

## 🎯 Prochaines Étapes

Une fois la base de données installée :

1. Lancez l'application JavaFX
2. Les interfaces devraient maintenant afficher les données
3. Testez les fonctionnalités de restauration
4. Vérifiez que les filtres fonctionnent correctement
