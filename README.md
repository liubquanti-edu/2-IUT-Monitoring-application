# 2-IUT-Monitoring-application
"IUT" / Réseaux et télécommunications / Application de supervision

## Structure du projet (étape 1 : modélisation du domaine)

```
src/main/java/fr/iut/netmonitor/
├── model/        Entités : NetworkDevice, Measurement, Event, Incident, User + énumérations
├── service/      NetworkDeviceManager : ajout, modification, suppression, recherche
├── persistence/  DeviceStore (interface) + SerializedDeviceStore (fichier data/devices.ser)
├── exception/    NetMonitorException, ValidationException, NotFoundException
├── util/         Validator (nom, IPv4), PasswordHasher (SHA-256 salé)
└── cli/          Main : menu texte
```

## Lancer

```
mvn test                     # tests unitaires
mvn compile exec:java        # menu texte
```

Sous Windows, si les accents s'affichent mal : `chcp 65001` avant de lancer.
