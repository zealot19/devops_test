# @fric DevOps Pipeline — Zero-to-Deploy

Pipeline CI/CD complet : Spring Boot + Docker + GitHub Actions + VPS Oracle Cloud.

---

## Pourquoi GitHub Actions et pas Jenkins ?

- Aucun serveur Jenkins à provisionner ou maintenir
- Runners Ubuntu gratuits fournis par GitHub
- Secrets gérés nativement dans l'interface GitHub
- Déclenchement automatique sur chaque push sur `main`

---

## Architecture du pipeline

```
Push sur main
     │
     ▼
[1] Build & Test (Maven + JUnit)
     │
     ▼
[2] Docker Build & Push (Docker Hub)
     │
     ▼
[3] Deploy via SSH sur VPS Oracle Cloud
     │
     ▼
[4] Notification (résumé dans les logs)
```

---

## Stack technique

| Composant       | Technologie                        |
|-----------------|------------------------------------|
| Application     | Spring Boot 3.3.0                  |
| Build           | Maven 3.9                          |
| Conteneurisation| Docker (multi-stage build)         |
| CI/CD           | GitHub Actions                     |
| Registry        | Docker Hub                         |
| Serveur cible   | Oracle Cloud Free Tier (Ubuntu)    |
| Sécurité        | SSH ed25519 (clés privée/publique) |

---

## Structure du repo

```
afric-devops-pipeline/
├── hello-devops/
│   ├── src/
│   │   └── main/java/com/afric/hellodevops/
│   │       └── HelloDevopsApplication.java
│   ├── Dockerfile
│   └── pom.xml
├── .github/
│   └── workflows/
│       └── pipeline.yml
└── README.md
```

---

## Configuration des Secrets GitHub

Aller dans : **GitHub repo → Settings → Secrets and variables → Actions → New repository secret**

| Nom du Secret          | Description                          | Comment l'obtenir                                                   |
|------------------------|--------------------------------------|---------------------------------------------------------------------|
| `DOCKER_HUB_USERNAME`  | Username Docker Hub                  | Ton compte hub.docker.com                                           |
| `DOCKER_HUB_TOKEN`     | Token Docker Hub                     | hub.docker.com → Account Settings → Security → New Access Token     |
| `SSH_PRIVATE_KEY`      | Clé privée SSH ed25519 complète      | Générée avec ssh-keygen (voir ci-dessous)                           |
| `VPS_HOST`             | IP publique du VPS                   | Dashboard Oracle Cloud                                              |
| `VPS_USER`             | Utilisateur SSH                      | `ubuntu` (par défaut Oracle Cloud)                                  |

### Générer les clés SSH

```bash
ssh-keygen -t ed25519 -C "github-actions-afric" -f ~/.ssh/afric_deploy -N ""
```

Cela génère deux fichiers :
- `~/.ssh/afric_deploy` → **clé privée** → valeur du secret `SSH_PRIVATE_KEY`
- `~/.ssh/afric_deploy.pub` → **clé publique** → à coller sur le VPS

Afficher la clé privée (à copier dans GitHub Secrets) :
```bash
cat ~/.ssh/afric_deploy
```

Afficher la clé publique (à coller sur le VPS) :
```bash
cat ~/.ssh/afric_deploy.pub
```

---

## Prérequis sur le serveur cible (VPS Oracle Cloud)

Se connecter au VPS, puis exécuter :

```bash
# 1. Installer Docker
curl -fsSL https://get.docker.com | sh

# 2. Ajouter l'utilisateur ubuntu au groupe docker
usermod -aG docker ubuntu

# 3. Créer le dossier SSH pour ubuntu
mkdir -p /home/ubuntu/.ssh
chmod 700 /home/ubuntu/.ssh

# 4. Ajouter la clé publique
echo "COLLER_ICI_LA_CLE_PUBLIQUE" >> /home/ubuntu/.ssh/authorized_keys
chmod 600 /home/ubuntu/.ssh/authorized_keys
chown -R ubuntu:ubuntu /home/ubuntu/.ssh

# 5. Ouvrir le port 8080
ufw allow 8080
```

---

## Déclencher et suivre le pipeline

Tout push sur la branche `main` déclenche automatiquement le pipeline.

Pour suivre l'exécution :
1. Aller sur le repo GitHub
2. Cliquer sur l'onglet **Actions**
3. Cliquer sur le dernier workflow en cours

Le pipeline comporte 4 jobs dans l'ordre :

| Job | Description |
|-----|-------------|
| 🔨 **Build & Test** | Compile le code et exécute les tests unitaires |
| 🐳 **Docker Build & Push** | Construit l'image Docker et la pousse sur Docker Hub |
| 🚀 **Deploy** | Se connecte au VPS via SSH et lance le nouveau conteneur |
| 📣 **Notification** | Affiche un résumé du statut dans les logs |

---

## Accéder à l'application déployée

Une fois le pipeline terminé avec succès :

```
http://IP_VPS:8080/         →  Hello from @fric DevOps Pipeline! ✅
http://IP_VPS:8080/health   →  UP
```

---

## Commandes utiles sur le VPS

```bash
# Voir les conteneurs qui tournent
docker ps

# Voir les logs de l'application
docker logs afric-hello-devops

# Redémarrer manuellement le conteneur
docker restart afric-hello-devops

# Voir les images téléchargées
docker images
```

