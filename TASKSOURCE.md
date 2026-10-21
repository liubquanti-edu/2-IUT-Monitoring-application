1. Contexte général
Les infrastructures informatiques modernes sont constituées d'un grand nombre d'équipements interconnectés : routeurs, commutateurs, points d'accès Wi-Fi, serveurs, ordinateurs, objets connectés, imprimantes réseau, équipements industriels, etc. Dans une entreprise, ces équipements peuvent être répartis sur plusieurs bâtiments ou plusieurs sites géographiques. Leur bon fonctionnement est indispensable au fonctionnement des services numériques de l'entreprise.

Lorsqu'un équipement devient indisponible, lorsqu'un serveur manque de mémoire ou lorsqu'un point d'accès Wi-Fi ne répond plus, il est important que les techniciens puissent détecter le problème rapidement. Il n'est évidemment pas envisageable qu'un technicien vérifie manuellement et en permanence chacun des équipements. Des outils de supervision réseau sont donc utilisés.

Dans cette SAÉ, vous allez développer une version simplifiée d'un tel outil. L'objectif n'est pas de reproduire un produit industriel existant, mais de comprendre les principaux mécanismes informatiques permettant de construire une application de supervision :

modélisation des équipements ;
gestion des utilisateurs ;
gestion des droits ;
conservation des données ;
gestion d'événements ;
détection d'anomalies ;
communication entre plusieurs applications ;
échanges réseau ;
gestion simultanée de plusieurs clients ;
interface graphique.
L'application développée sera appelée NetMonitor.

2. Introduction à la supervision réseau
Qu'est-ce que la supervision réseau ?
La supervision réseau consiste à observer automatiquement et régulièrement l'état d'une infrastructure informatique. L'objectif est notamment de répondre à des questions telles que :

un équipement est-il actuellement joignable ?
depuis combien de temps fonctionne-t-il ?
quand a-t-il été vu pour la dernière fois ?
son processeur est-il fortement utilisé ?
manque-t-il de mémoire ?
est-il en fonctionnement normal ?
une anomalie a-t-elle été détectée ?
un technicien a-t-il pris en charge cette anomalie ?
Une plateforme de supervision centralise ces informations pour permettre aux administrateurs et techniciens d'avoir une vision globale de l'état de leur infrastructure.

Exemple d'infrastructure
Une petite entreprise peut disposer de l'infrastructure suivante :

                     Internet
                        |
                     Routeur
                        |
                 +------+------+
                 |             |
              Switch A      Switch B
                 |             |
          +------+------+      +---------+
          |             |                |
      Serveur        AP Wi-Fi         Serveur
       Web                              Fichiers
Une plateforme de supervision doit par exemple pouvoir afficher :

RTR-01      Routeur          192.168.1.1     UP
SW-01       Switch           192.168.1.10    UP
AP-01       Wi-Fi            192.168.1.20    WARNING
SRV-WEB     Serveur          192.168.1.30    UP
SRV-FILES   Serveur          192.168.1.31    DOWN
L'objectif n'est donc pas uniquement de stocker une liste d'équipements. La plateforme doit également suivre leur état au cours du temps.

État d'un équipement
Dans NetMonitor, chaque équipement possède un état. On retiendra au minimum les trois états suivants :

- UP: L'équipement fonctionne normalement.

- WARNING: L'équipement fonctionne mais une anomalie a été détectée.

Exemples :

utilisation CPU importante ;
mémoire presque saturée ;
latence importante ;
perte de certains messages.
- DOWN: L'équipement est considéré comme indisponible.

Exemples :

aucune information reçue depuis un certain temps ;
équipement arrêté ;
perte de connexion.
D'autres états pourront éventuellement être proposés.

Mesures et indicateurs
Un outil de supervision peut recevoir différents indicateurs. Dans NetMonitor, on pourra par exemple utiliser :

utilisation CPU ;
utilisation mémoire ;
latence ;
disponibilité ;
date du dernier contact.
Exemple :

Équipement : SRV-WEB

Adresse IP : 192.168.1.30
État       : UP
CPU        : 34 %
RAM        : 62 %
Latence    : 12 ms
Dernier contact : 14:32:17
Dans cette SAÉ, ces informations pourront être dans un premier temps simulées.

Événements, alertes et incidents
Il est important de distinguer plusieurs notions.

- Événement: Un événement correspond à quelque chose qui vient de se produire.

Exemple :

14:32:17 - SRV-WEB a envoyé son état.
- Alerte: Une alerte signale une situation anormale.

Exemple :

14:35:08 - CPU du serveur SRV-WEB supérieur à 90 %.
- Incident: Un incident correspond à un problème devant potentiellement être traité par un technicien.

Exemple :

Incident #42

Équipement : SRV-WEB
Gravité    : CRITICAL
Problème   : serveur inaccessible
État       : OPEN
Un incident pourra ensuite passer par plusieurs états :

   OPEN
    |
    v
IN_PROGRESS
    |
    v
  CLOSED
Le principe du heartbeat
Une technique très courante consiste à demander aux équipements supervisés d'envoyer régulièrement un message indiquant : « Je suis toujours actif. » On appelle généralement ce message un heartbeat (battement de coeur).

Exemple :

AP-01 ---- HEARTBEAT ----> Serveur

AP-01 ---- HEARTBEAT ----> Serveur

AP-01 ---- HEARTBEAT ----> Serveur

AP-01 ---- X
Si aucun heartbeat n'est reçu pendant une durée définie, le serveur peut considérer que l'équipement est devenu indisponible.

Exemple :

Dernier heartbeat : 14:31:04
Heure actuelle    : 14:31:40

Aucun heartbeat depuis 36 secondes.

=> AP-01 est déclaré DOWN.
NetMonitor utilisera ultérieurement un mécanisme similaire.

3. Objectif global du projet
Objectifs
Vous devez développer une application Java permettant de superviser un ensemble d'équipements informatiques.

L'application devra progressivement permettre :

d'enregistrer des équipements ;
de les consulter ;
de modifier certaines informations ;
de conserver les données ;
d'authentifier des utilisateurs ;
de gérer plusieurs types d'utilisateurs ;
de visualiser l'état des équipements ;
de recevoir des informations provenant d'autres applications ;
de gérer plusieurs connexions ;
de détecter certains problèmes ;
de générer des alertes ou incidents ;
de permettre aux techniciens de suivre les incidents.
Le développement sera volontairement réalisé par étapes.

Une partie importante du travail consistera à faire évoluer une première application locale vers une véritable architecture client/serveur.

Architecture cible
À la fin de la SAÉ, l'application devra se rapprocher de l'architecture suivante :

                     +----------------------+
                     |   Client NetMonitor  |
                     |       JavaFX         |
                     +----------+-----------+
                                |
                                |
                         Communication
                             réseau
                                |
                                |
                     +----------v-----------+
                     |                      |
                     |   NetMonitor Server  |
                     |                      |
                     | - utilisateurs       |
                     | - équipements        |
                     | - incidents          |
                     | - supervision        |
                     |                      |
                     +----------+-----------+
                                |
                               JDBC
                                |
                     +----------v-----------+
                     | Base de données      |
                     +----------------------+

                                ^
                                |
                                |
                  +-------------+-------------+
                  |             |             |
               Agent 1       Agent 2       Agent 3
               AP-01         SW-01         SRV-01
Les agents représentent des équipements supervisés. Ils seront également développés en Java. Ils n'ont pas besoin de contrôler de véritables équipements : ils pourront simuler leurs données.

Technologies
Le projet sera développé principalement avec :

Java ;
programmation orientée objet ;
JavaFX pour l'interface graphique ;
JDBC ;
une base de données ;
sérialisation des données ;
sockets Java dans les volets réseau ;
threads ou mécanismes équivalents pour les traitements concurrents.
Selon l'avancement du projet, certaines bibliothèques complémentaires pourront être utilisées après validation par l'enseignant.

4. Cahier des charges
Rédigez un cahier des charges qui détaille: 

la gestion des équipements
la gestion des utilisateurs
la gestion des incidents
l'interface graphique
la persistance
la robustesse

5. Modélisation du domaine
Objectif
Construire le cœur métier de NetMonitor en programmation orientée objet. À ce stade : aucune communication réseau n'est demandée. L'application fonctionne uniquement localement.

Travail demandé
Identifier et modéliser les principales entités du système. On pourra notamment retrouver les concepts suivants :

User
NetworkDevice
Incident
Event
Measurement
Vous devrez réfléchir à leurs relations.

Exemple :

NetworkDevice 1 -------- * Incident
       |
       |
       * 
 Measurement
Un diagramme de classes UML simplifié devra être produit.

Fonctionnalités minimales
L'application doit permettre :

création d'un équipement ;
suppression d'un équipement ;
modification d'un équipement ;
recherche d'un équipement ;
affichage de la liste des équipements.
Les données peuvent dans un premier temps être stockées en mémoire.

Exemple

=== NetMonitor ===

1. Ajouter un équipement
2. Afficher les équipements
3. Rechercher
4. Supprimer
5. Quitter
Une interface en ligne de commande peut être utilisée dans cette première étape.

Compétences mobilisées
classes ;
objets ;
constructeurs ;
encapsulation ;
collections ;
énumérations ;
associations ;
héritage éventuellement ;
polymorphisme éventuellement ;
exceptions.

6. Persistance
Objectif
Faire en sorte que les informations ne disparaissent pas lorsque l'application est arrêtée.

Travail demandé
Les étudiants devront mettre en place un mécanisme permettant :

d'enregistrer les données ;
de relire les données au démarrage ;
de gérer correctement les erreurs.
Une première étape pourra utiliser un format structuré (json).

Par exemple :

{
    "id": 12,
    "name": "AP-01",
    "ip": "192.168.1.20",
    "type": "ACCESS_POINT",
    "status": "UP"
}
On pourra ensuite faire évoluer la persistance vers une base de données avec JDBC.

Questions à traiter
quelles informations doivent être conservées ?
quel format utiliser ?
comment représenter un objet sous forme de données ?
que se passe-t-il si le fichier ou la base est inaccessible ?
comment éviter de dupliquer inutilement les données ?

7. Authentification et interface graphique
Objectif
Construire une véritable application utilisable par différents profils. À ce stade, l'application peut encore fonctionner sur une seule machine.

Authentification
L'utilisateur doit commencer par s'identifier.

Exemple :

+----------------------------+
| NetMonitor                 |
|                            |
| Login    [____________]    |
| Password [____________]    |
|                            |
|       [ Connexion ]        |
+----------------------------+
Après authentification, les fonctionnalités proposées dépendent du rôle.

Gestion des droits
Exemple :

Fonction	Administrateur	Technicien
Consulter équipements	Oui	Oui
Ajouter équipement	Oui	Non
Modifier équipement	Oui	Non
Supprimer équipement	Oui	Non
Gérer utilisateurs	Oui	Non
Consulter incidents	Oui	Oui
Prendre un incident	Oui	Oui
Clôturer un incident	Oui	Oui
Interface JavaFX
Une interface graphique devra progressivement remplacer l'interface textuelle. Elle devra au minimum proposer :

écran de connexion ;
liste des équipements ;
fiche équipement ;
liste des incidents ;
fonctions dépendant du rôle.
Programmation événementielle
Cette partie doit permettre de travailler :

événements JavaFX ;
boutons ;
listeners ;
callbacks ;
mise à jour de composants graphiques.
Exemple :

Utilisateur
     |
     | clic
     v
Bouton "Ajouter"
     |
     v
EventHandler
     |
     v
Contrôle des données
     |
     v
Création de l'objet
     |
     v
Mise à jour de l'interface

8. Tableau de bord, composants JavaFX et statistiques
Construire un tableau de bord synthétique permettant à l’utilisateur authentifié d’obtenir rapidement une vue d’ensemble de l’état du parc supervisé. A partir de cette interface, on peut ouvrir via un menu la liste des équipements, la liste des incidents etc.

Le tableau de bord apparaît après l’authentification:

+--------------------------------------------------------------+
| NetMonitor                                              Alice - Administrateur     |
+--------------------------------------------------------------+
| Dashboard  |  Équipements  |  Incidents  |  Utilisateurs | Logout |
+--------------------------------------------------------------+
|                                                                                                             |
|      Équipements            En ligne            Alertes           Incidents    |
|              24                           19                     3                       5           |
|                                                                                                             |
|                                 Derniers incidents                                             |
|    ----------------------------------------------------------     |
|  ID   Équipement      Gravité              État             Technicien         |
|  15   SRV-01            CRITICAL         OPEN                    -                  |
|  14   AP-03             WARNING   IN_PROGRESS       Alice              |
|                                                                                                             |
+--------------------------------------------------------------+

Le tableau de bord devra afficher au minimum :
nombre total d’équipements ;
nombre d’équipements UP ;
nombre d’équipements WARNING ;
nombre d’équipements DOWN ;
nombre d’incidents ouverts.
Ces informations devront être calculées à partir des données de l’application et ne devront pas être codées en dur.

Ajouter un second graphique indiquant par exemple le nombre d’incidents par niveau de gravité :

Incidents

CRITICAL.   ███████   7
WARNING   █████     5
INFO            ██        2

Ajouter un champ permettant de rechercher un équipement par :
- nom ;
- adresse IP ;
- localisation.