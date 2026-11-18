# AppForge Cloud V3 — LifeOS

## Architecture

LifeOS reste **offline-first** : l’état applicatif est enregistré localement avant les appels réseau. Le client ne saisit aucun compte, e-mail, mot de passe, organisation ou URL de Worker AppForge Cloud.

1. Le client active un essai ou un code de licence après validation serveur du paiement.
2. Le Worker de licences vérifie la licence, le produit et l’installation autorisée.
3. Si le droit Cloud est présent, `POST /v1/cloud-token` délivre un HMAC signé valable 10 minutes.
4. Le Worker Cloud revalide à chaque requête le statut, l’expiration, la suspension, le droit et l’appareil dans le D1 des licences.
5. `GET/PUT /v1/lifeos/snapshot` stocke un snapshot par `(license_id, product_id)` dans Cloud D1. Les écritures utilisent une révision optimiste et renvoient 409 en cas de conflit. Les données locales ne sont jamais effacées par une erreur distante.

Les essais utilisent un tenant stable par installation. Les licences payées utilisent leur UUID de licence. Le quota est partagé entre les appareils de la licence. Les anciennes routes comptes/organisations sont conservées dans le Worker historique et ne sont plus appelées par LifeOS.

## Déploiement Cloudflare

Le déploiement exige les droits sur les Workers et D1 Cloudflare. Aucun identifiant Cloudflare ou secret de serveur ne doit être ajouté au dépôt ou à l’application.

1. Créer deux bases D1 : `lifeos-licenses` et `lifeos-cloud`.
2. Renseigner leurs ID dans les deux fichiers `wrangler.toml`. Le binding `LICENSES` du Worker Cloud doit pointer vers **la même base** que le binding `DB` du Worker de licences. Garder `DB` Cloud séparée.
3. Depuis `cloudflare/appforge-license/worker`, appliquer les migrations distantes jusqu’à `0006_cloud_entitlements.sql`. La migration ajoute des colonnes et ne supprime aucune donnée.
4. Depuis `cloudflare/appforge-cloud-sync/worker`, appliquer les migrations distantes jusqu’à `0003_license_snapshots.sql`. Les tables de comptes historiques ne sont pas supprimées.
5. Configurer par `wrangler secret put` les secrets existants du Worker de licences (`LICENSE_SIGNING_PRIVATE_JWK`, `CODE_HASH_PEPPER`, `ADMIN_API_TOKEN`) selon son guide. Créer un secret aléatoire d’au moins 32 caractères pour `CLOUD_TOKEN_HMAC_SECRET` et configurer **exactement la même valeur** sur les deux Workers. Ne pas la transmettre dans le client ni dans Git.
6. Définir les origines CORS réellement utilisées par l’application Android dans `APP_ORIGINS`. L’origine WebView doit être vérifiée sur l’APK; `https://localhost` est la valeur habituelle Capacitor, pas une preuve d’intégration.
7. Déployer d’abord le Worker de licences, puis `lifeos-cloud-api`. Confirmer que les URL configurées dans `src/cloud-v3.js` correspondent bien aux Workers déployés. L’application ne demande pas au client de les modifier.
8. Vérifier `GET /health` sur les deux services, puis tester sur Cloudflare avec une licence de test : délivrance, renouvellement, refus après expiration/révocation, appareil autorisé/interdit, deux appareils, panne réseau/reprise, quota et séparation de deux licences.

MVola n’est pas traité comme un paiement automatiquement confirmé par l’application : le Worker n’active le code que lorsque `paid_confirmed=1`, renseigné après la validation de paiement existante côté administration.

## Quotas, conflits et limites

- La limite par défaut est de 5 000 requêtes par licence et par jour; une valeur par licence peut être définie dans `cloud_daily_limit`.
- Le snapshot est limité à 500 Ko. Une écriture ne remplace qu’une révision explicitement attendue.
- En cas de divergence, LifeOS présente le conflit et demande de choisir la copie. Il n’y a pas de fusion automatique d’enregistrements.
- D1 contient du JSON applicatif en clair au repos selon la configuration Cloudflare; HTTPS protège le transport mais il n’y a pas de chiffrement de bout en bout implémenté.
- L’ouverture du HTML en `file://` n’offre pas une origine CORS fiable. Utiliser l’APK Capacitor ou un hébergement HTTPS d’origine autorisée.
- Les jetons Android sont chiffrés en AES-GCM avec une clé Android Keystore. En mode HTML, le jeton reste en mémoire et est redemandé au Worker de licences après rechargement.

## État de validation

Les tests automatisés locaux du protocole et les workflows CI couvrent le code et des scénarios simulés. Les tests de deux véritables appareils, d’expiration/révocation sur le Worker déployé et d’intégration MVola nécessitent les secrets, bases et environnements Cloudflare de l’organisation; ils restent à exécuter avant d’annoncer une conformité d’intégration complète.
