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

| Composant        | Technologie                        |
|------------------|------------------------------------|
| Application      | Spring Boot 3.3.0                  |
| Build            | Maven 3.9                          |
| Conteneurisation | Docker (multi-stage build)         |
| CI/CD            | GitHub Actions                     |
| Registry         | Docker Hub                         |
| Serveur cible    | Oracle Cloud Free Tier (Ubuntu)    |
| Sécurité         | SSH ed25519 (clés privée/publique) |

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

## Étape 1 — Générer les clés SSH

Sur ta machine ou dans le Codespace, exécute :

```bash
ssh-keygen -t ed25519 -C "github-actions-afric" -f ~/.ssh/afric_deploy -N ""
```

Cela génère deux fichiers :
- `~/.ssh/afric_deploy` → **clé privée** → valeur du secret `SSH_PRIVATE_KEY`
- `~/.ssh/afric_deploy.pub` → **clé publique** → à coller sur le VPS

Afficher la clé privée :
```bash
cat ~/.ssh/afric_deploy
```

Afficher la clé publique :
```bash
cat ~/.ssh/afric_deploy.pub
```

---

## Étape 2 — Créer un token Docker Hub

1. Va sur **https://hub.docker.com**
2. Connecte-toi → clique sur ton avatar → **Account Settings**
3. **Personal access tokens** → **Generate new token**
4. Token name : `github-actions-afric`
5. Permissions : **Read & Write**
6. Clique **Generate** et copie le token immédiatement

---

## Étape 3 — Configurer les Secrets GitHub

Aller dans : **GitHub repo → Settings → Secrets and variables → Actions → New repository secret**

| Nom du Secret         | Description                     | Valeur                                         |
|-----------------------|---------------------------------|------------------------------------------------|
| `DOCKER_HUB_USERNAME` | Username Docker Hub             | Ton username hub.docker.com                    |
| `DOCKER_HUB_TOKEN`    | Token Docker Hub                | Token généré à l'étape 2                       |
| `SSH_PRIVATE_KEY`     | Clé privée SSH ed25519 complète | Contenu complet de `~/.ssh/afric_deploy`       |
| `VPS_HOST`            | IP publique du VPS              | IP fournie par Oracle Cloud                    |
| `VPS_USER`            | Utilisateur SSH                 | `ubuntu` (par défaut Oracle Cloud)             |

> ⚠️ Pour `SSH_PRIVATE_KEY`, copier le contenu **entier** incluant :
> ```
> -----BEGIN OPENSSH PRIVATE KEY-----
> ...
> -----END OPENSSH PRIVATE KEY-----
> ```

---

## Étape 4 — Préparer le serveur VPS (Oracle Cloud)

Se connecter au VPS via SSH, puis exécuter ces commandes :

### 4.1 Installer Docker
```bash
curl -fsSL https://get.docker.com | sh
```

### 4.2 Ajouter l'utilisateur ubuntu au groupe docker
```bash
usermod -aG docker ubuntu
```

### 4.3 Autoriser la clé SSH de GitHub Actions
```bash
mkdir -p /home/ubuntu/.ssh
chmod 700 /home/ubuntu/.ssh
echo "COLLER_ICI_LA_CLE_PUBLIQUE" >> /home/ubuntu/.ssh/authorized_keys
chmod 600 /home/ubuntu/.ssh/authorized_keys
chown -R ubuntu:ubuntu /home/ubuntu/.ssh
```

> Remplacer `COLLER_ICI_LA_CLE_PUBLIQUE` par le contenu de `~/.ssh/afric_deploy.pub`

### 4.4 Ouvrir les ports nécessaires
```bash
ufw allow OpenSSH
ufw allow 8080
ufw enable
```

### 4.5 Vérifier que Docker fonctionne
```bash
docker --version
docker run hello-world
```

### 4.6 Tester la connexion SSH depuis le Codespace
```bash
ssh -i ~/.ssh/afric_deploy ubuntu@IP_DU_VPS
```

---

## Étape 5 — Déclencher et suivre le pipeline

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

# Supprimer les anciennes images
docker image prune -f
```
