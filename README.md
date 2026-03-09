# Projet Architecture Logicielle - Microservices Auth & API Gateway

**Auteurs :** Elies LOUNIS, Adel EL MOUSSAOUI, Amir BENYAHIA et Sofiane KHOURTA

Ce projet implémente une architecture orientée microservices sécurisée par une API Gateway NGINX. Il répond aux exigences du projet d'introduction à l'architecture logicielle : authentification décentralisée, proxy inverse, et communication asynchrone type événementielle.

## Architecture du Projet

L'écosystème est composé de 5 conteneurs applicatifs et 3 conteneurs d'infrastructure :

1. **NGINX (API Gateway)** : Point d'entrée unique (`localhost:80`). Route les requêtes vers les services et vérifie les jetons JWT en interrogeant l'Auth Service via le module `auth_request`.
2. **Auth Service** : Gère l'inscription (`/register`), la vérification (`/verify`), la génération de token JWT (`/login`) et la validation de tokens (`/validate`). Connecté à MySQL et RabbitMQ.
3. **Notification Service** : Écoute les événements `UserRegistered` émis par l'Auth Service via RabbitMQ et envoie un e-mail de confirmation à l'utilisateur via MailHog.
4. **Service A** : Un pseudo-service protégé qui renvoie `hello A` sur `/hello`.
5. **Service B** : Un pseudo-service protégé qui renvoie `hello B` sur `/hello`.
6. **Infrastructure** : MySQL 8.0 (Base de données), RabbitMQ (Message Broker) et MailHog (Serveur SMTP local).

## Prérequis

- **Docker** et **Docker Compose** doivent être installés et fonctionnels sur votre machine.
- *Note : Grâce aux builds multi-étapes (multi-stage builds) dans les Dockerfiles, **Java et Maven ne sont même pas requis** sur la machine hôte. Docker se charge de télécharger Maven, compiler le code source, et lancer les applications.*

## 🛠️ Lancer le projet

Une seule commande est nécessaire pour compiler l'intégralité du code source Java et monter toute l'infrastructure (Base de données, Message Broker, SMTP, NGINX et les 4 microservices Spring Boot) :

```bash
docker compose up -d --build
```

*(Attendez environ 30 à 60 secondes le temps que les conteneurs Spring Boot démarrent complètement, surtout au premier lancement où Maven télécharge les dépendances).*

Pour arrêter les conteneurs :
```bash
docker compose down
```

## Scénario de Test (Manuel)

Voici les commandes `curl` pour tester de A à Z la robustesse de l'API Gateway et des services.

### 1. Tenter d'accéder aux services sans être authentifié
Accès refusé par l'API Gateway (HTTP 403 Forbidden) :
```bash
curl -i http://localhost/a/hello
curl -i http://localhost/b/hello
```

### 2. Créer un compte utilisateur
L'Auth Service va hasher le mot de passe en base de données et publier un événement dans RabbitMQ.
```bash
curl -X POST http://localhost/register \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com", "password":"password123"}'
```

### 3. Vérifier la réception de l'e-mail (Notification Service)
Le Notification Service a consommé l'événement RabbitMQ et généré un e-mail.
Rendez-vous sur l'interface de MailHog dans votre navigateur : [http://localhost:8025](http://localhost:8025)
Vous y trouverez un e-mail contenant un lien de vérification avec un token unique.

### 4. Valider le compte
Copiez l'UUID (le token) reçu dans l'e-mail sur MailHog et remplacez `<VOTRE_TOKEN>` ci-dessous :
```bash
curl -i "http://localhost/verify?token=<VOTRE_TOKEN>"
```

### 5. Se connecter et récupérer le JWT
Une fois vérifié, connectez-vous pour obtenir votre Token JWT :
```bash
curl -X POST http://localhost/login \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com", "password":"password123"}'
```
Copiez la valeur du `"token"` retourné dans la réponse.

### 6. Accéder aux services protégés via l'API Gateway
Maintenant que vous avez le JWT, injectez-le dans le header `Authorization: Bearer <JWT>`. NGINX va faire une subrequest transparente à l'Auth Service (`/validate`), obtenir un 200 OK, et vous laisser passer vers les vrais services !

Remplacez `<VOTRE_JWT>` par le token généré à l'étape précédente :
```bash
curl -i -H "Authorization: Bearer <VOTRE_JWT>" http://localhost/a/hello

curl -i -H "Authorization: Bearer <VOTRE_JWT>" http://localhost/b/hello
```
*Succès ! Vous devriez voir s'afficher `hello A` et `hello B`.*

## Accès aux interfaces d'administration locales
- **MailHog UI** : [http://localhost:8025](http://localhost:8025)
- **RabbitMQ UI** : [http://localhost:15672](http://localhost:15672) (Identifiants : `guest` / `guest`)
- **API Gateway (NGINX)** : [http://localhost:80](http://localhost:80)
- **Base de données MySQL** : `localhost:3306` (User: `user` / Password: `password` / DB: `auth_db`)
