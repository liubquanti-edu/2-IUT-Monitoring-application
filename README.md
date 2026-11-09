# 2-IUT-Monitoring-application
"IUT" / Réseaux et télécommunications / Application de supervision

## Structure du projet

```
src/main/java/fr/iut/netmonitor/
├── model/        Entités : NetworkDevice, Measurement, Event, Incident, User + énumérations
│                 (DeviceStatus, DeviceType, Severity, IncidentStatus, EventLevel, UserRole, Permission)
├── service/      NetworkDeviceManager (équipements), UserManager (comptes), IncidentManager (incidents),
│                 MonitoringService (seuils + heartbeat), AgentSimulator (agents simulés),
│                 DashboardStats (indicateurs), AccessControl (droits), NetMonitorContext (chargement/enregistrement)
├── persistence/  DataStore + MySqlDataStore (base MySQL, JDBC) et SerializedDataStore (fichier data/netmonitor.ser)
├── exception/    NetMonitorException, ValidationException, NotFoundException,
│                 AuthenticationException, AccessDeniedException
├── util/         Validator (nom, IPv4), PasswordHasher (SHA-256 salé)
├── cli/          Main : menu texte (première version, avec connexion)
└── gui/          Application JavaFX : connexion, tableau de bord, équipements, fiche équipement,
                  incidents, utilisateurs
```

## Lancer

### En un clic, sans droits administrateur (Windows)

Double-cliquer sur **`netmonitor.cmd`**. Ce fichier est autonome : il peut être copié seul (par exemple depuis
une clé USB) sur n'importe quel PC. Il affiche l'avancement de chaque étape (`DONE`, `IN PROCESS`, `IN QUEUE`,
`ERROR`), installe tout dans le profil utilisateur puis lance l'application :

1. **Git** : Git portable (`%USERPROFILE%\portable-git`, ajouté au PATH utilisateur) sauf si Git est déjà
   présent, et réglage de `user.name` / `user.email` ;
2. **Repo** : clonage du dépôt dans `%USERPROFILE%-IUT-Monitoring-application`, ou `git pull` s'il existe déjà ;
3. **MySQL** : serveur MySQL portable 8.4 (archive ZIP, `%USERPROFILE%\portable-mysql`), initialisé au premier
   lancement, démarré en arrière-plan sur `127.0.0.1:3307`, base `netmonitor`
   (utilisateur `netmonitor` / mot de passe `netmonitor`) ;
4. **JDK** : JDK 21 portable (`%USERPROFILE%\portable-jdk21`) ;
5. **App** : NetMonitor (JavaFX). À la fermeture de l'application, le serveur MySQL est arrêté.

Le premier lancement télécharge environ 450 Mo (MySQL ≈ 250 Mo). Les suivants démarrent directement.
MySQL a besoin du runtime Visual C++ (`vcruntime140.dll`, `msvcp140.dll`), présent sur la plupart des PC ;
sinon, copier ces DLL dans le dossier `bin` de MySQL.

### Avec Maven

Maven n'a pas besoin d'être installé : le wrapper (`mvnw` / `mvnw.cmd`) le télécharge au premier lancement.
Seul un JDK 21 ou plus récent est nécessaire.

```
.\mvnw.cmd test                  # tests unitaires        (Linux/macOS : ./mvnw test)
.\mvnw.cmd javafx:run            # interface graphique JavaFX
.\mvnw.cmd compile exec:java     # menu texte
```

Depuis un IDE, lancer la classe `fr.iut.netmonitor.gui.Launcher`.

Sans configuration, les données sont enregistrées dans `data/netmonitor.ser`. Pour utiliser MySQL, définir
les variables d'environnement (ou les propriétés système `netmonitor.db.url`, `.user`, `.password`) :

```
NETMONITOR_DB_URL=jdbc:mysql://127.0.0.1:3307/netmonitor?sslMode=DISABLED&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
NETMONITOR_DB_USER=netmonitor
NETMONITOR_DB_PASSWORD=netmonitor
```
Sous Windows, si les accents s'affichent mal dans le menu texte : `chcp 65001` avant de lancer.

### Comptes créés au premier lancement

| Identifiant | Mot de passe | Rôle           |
|-------------|--------------|----------------|
| admin       | admin        | Administrateur |
| tech        | tech         | Technicien     |
| viewer      | viewer       | Observateur    |

Des équipements de démonstration sont aussi créés. Pensez à changer les mots de passe (bouton « Mot de passe »).

## Droits par rôle

| Fonction               | Administrateur | Technicien | Observateur |
|------------------------|:--------------:|:----------:|:-----------:|
| Consulter équipements  | Oui            | Oui        | Oui         |
| Ajouter / modifier / supprimer un équipement | Oui | Non | Non     |
| Gérer les utilisateurs | Oui            | Non        | Non         |
| Consulter incidents    | Oui            | Oui        | Oui         |
| Créer, prendre en charge, clôturer un incident | Oui | Oui | Non   |

Les boutons non autorisés ne sont pas affichés, et les services revérifient les droits (`AccessControl`).

## Supervision (simulée)

En attendant les agents réseau, `AgentSimulator` envoie toutes les 2 s un heartbeat avec des mesures
(CPU, RAM, latence) pour chaque équipement, avec des pics et des pannes aléatoires.
`MonitoringService` applique les règles du cahier des charges :

- CPU > 90 %, RAM > 90 % ou latence > 200 ms → état WARNING, alerte et incident MEDIUM ;
- aucun heartbeat depuis plus de 30 s → état DOWN, alerte et incident CRITICAL ;
- un seul incident ouvert par équipement et par problème.

## Tableau de bord

Le tableau de bord affiche, calculés à partir des données : le nombre total d'équipements, le nombre UP,
WARNING et DOWN, le nombre d'incidents ouverts, un graphique de l'état des équipements, un graphique
des incidents par gravité, les derniers incidents, et un champ de recherche (nom, adresse IP, localisation).

## Persistance : réponses aux questions

- **Quelles informations conserver ?** Les équipements (nom, IP, type, localisation, état, dernier contact),
  leur historique (mesures, événements, alertes, incidents avec technicien et dates) et les comptes
  utilisateurs (mot de passe haché salé, jamais en clair). L'historique est limité aux 500 dernières
  mesures et événements par équipement.
- **Où ?** Dans une base **MySQL** (`MySqlDataStore`) quand elle est configurée (c'est le cas avec
  `netmonitor.cmd`), sinon dans le fichier `data/netmonitor.ser`. Tables : `users`, `devices`,
  `measurements`, `events`, `incidents` (clé étrangère `device_id`, suppression en cascade), créées
  automatiquement. Au premier lancement avec une base vide, le fichier `netmonitor.ser` existant est
  importé dans MySQL. L'enregistrement remplace le contenu dans une transaction : en cas d'erreur, la base
  reste intacte. Si la base n'a pas pu être lue au démarrage, l'enregistrement est refusé pour ne pas
  l'écraser avec des données vides.
- **Quel format (fichier) ?** La sérialisation Java (`ObjectOutputStream`) d'un objet `AppData` : simple, sans
  bibliothèque externe, et elle conserve les liens entre objets. L'interface `DataStore` permettra de
  passer à JDBC sans modifier le reste du code.
- **Comment représenter un objet sous forme de données ?** Les classes du modèle implémentent
  `Serializable` avec un `serialVersionUID` fixe. Les champs ajoutés plus tard (ex. `location`) reçoivent
  une valeur par défaut à la lecture d'un ancien fichier.
- **Fichier inaccessible ?** Un fichier absent donne des données de départ. Un fichier illisible ou corrompu
  est signalé à l'utilisateur et l'application démarre avec des données vides. L'enregistrement passe par
  un fichier temporaire renommé ensuite, donc un arrêt pendant l'écriture ne corrompt pas l'ancien fichier.
  Les erreurs d'enregistrement sont affichées.
- **Éviter les doublons ?** Le nom et l'adresse IP d'un équipement sont uniques. Les incidents et événements
  ne sont stockés qu'une fois, dans leur équipement : la liste globale des incidents est calculée, pas
  recopiée. L'ancien fichier `data/devices.ser` est importé automatiquement s'il existe.
