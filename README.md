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
├── persistence/  DataStore + SerializedDataStore (fichier data/netmonitor.ser)
├── exception/    NetMonitorException, ValidationException, NotFoundException,
│                 AuthenticationException, AccessDeniedException
├── util/         Validator (nom, IPv4), PasswordHasher (SHA-256 salé)
├── cli/          Main : menu texte (première version, avec connexion)
└── gui/          Application JavaFX : connexion, tableau de bord, équipements, fiche équipement,
                  incidents, utilisateurs
```

## Lancer

Maven n'a pas besoin d'être installé : le wrapper (`mvnw` / `mvnw.cmd`) le télécharge au premier lancement.
Seul un JDK 21 ou plus récent est nécessaire.

```
.\mvnw.cmd test                  # tests unitaires        (Linux/macOS : ./mvnw test)
.\mvnw.cmd javafx:run            # interface graphique JavaFX
.\mvnw.cmd compile exec:java     # menu texte
```

Depuis un IDE, lancer la classe `fr.iut.netmonitor.gui.Launcher`.
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
- **Quel format ?** La sérialisation Java (`ObjectOutputStream`) d'un objet `AppData` : simple, sans
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
