# LifeOS Premium 6.7.0

LifeOS aide à organiser les objectifs, habitudes, projets et tâches, suivre les progrès et planifier des sessions de concentration. L’interface est disponible en français et malgache, avec thèmes clair/sombre et affichage compact.

## Fonctionnalités incluses

- Objectifs reliés aux projets, jalons, tâches et habitudes.
- Habitudes classées Matin, Journée ou Soir, avec actions rapides et suivi quotidien.
- Recherche, filtres, tri, statistiques, calendrier, guide pratique et minuteur de concentration.
- 150 icônes dédiées aux habitudes et 150 icônes dédiées aux objectifs, plus « Sans icône ».
- Sauvegarde locale, restauration avec validation et chiffrement facultatif AES-GCM par mot de passe.
- Verrouillage Android par biométrie ou code de l’appareil lorsqu’il est activé.

## Formats

- HTML autonome pour Chrome, Edge ou un autre navigateur compatible.
- Application Android Capacitor avec variantes minSdk API 23 et API 29.
- Aucune PWA n’est générée.

## Sauvegarde et transfert

Sur Android 10 ou ultérieur, l’export local est enregistré par défaut dans `Download/backup`. Sur Android 6 à 9, Android ouvre le sélecteur d’emplacement. Dans un navigateur, l’emplacement dépend du réglage de téléchargement du navigateur.

Le partage vers Google Drive se fait avec le menu de partage natif Android. La restauration utilise le sélecteur de fichiers Android. LifeOS ne se connecte pas au compte Google et ne synchronise pas automatiquement les appareils. L’export protégé peut être partagé depuis la boîte de dialogue de sauvegarde chiffrée.

Les données du HTML et de l’application Android sont stockées séparément. Conserve une copie externe avant de désinstaller ou d’effacer les données.

## Développement

Prérequis : Node.js 22, JDK 21 et Android SDK.

```bash
npm ci
npm test
npm run android:sync
```

Pour compiler dans le dossier `android` :

```bash
./gradlew :app:assembleApi23Debug :app:assembleApi29Debug
```

Le workflow GitHub exécute les tests, synchronise Capacitor et construit les deux APK Debug sur Pull Request. Sur `main`, il construit les Releases signées API 23 et API 29 si les secrets organisationnels `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD` et `KEY_PASSWORD` sont présents. Le workflow arrête la compilation Release si un secret manque.

## État AppForge V3

Cette branche apporte les variantes Android API 23/29, le partage/restauration natifs sans OAuth, l’interface À propos & Support, les clients Cloud/licences LifeOS et leurs Workers Cloudflare + migrations D1. Aucun fichier PWA n’est généré. Un audit statique vert ne signifie pas conformité complète.

À compléter ou à valider séparément :

- Déployer les deux Workers Cloudflare, créer les bases D1, configurer les origines exactes et les secrets depuis les guides sous `cloudflare/`. La Bibliothèque ne contenait ni endpoint réel ni secrets.
- Tester en ligne les parcours Cloud et licence : les fichiers et clients sont préparés, le service n’est pas déployé.
- Essais sur appareil pour le verrouillage, le partage, la restauration, la sauvegarde locale et les autorisations.
- Builds Release signés et vérification de la disponibilité effective des secrets d’organisation.

Le référentiel complet est dans [APPFORGE-STANDARD-V3.md](APPFORGE-STANDARD-V3.md).
