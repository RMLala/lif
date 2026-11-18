# LifeOS — licences AppForge V3

Le Worker valide la licence, le paiement confirmé, la date d’expiration et l’installation autorisée. La route `POST /v1/cloud-token` renouvelle une autorisation Cloud de dix minutes seulement quand `cloud_enabled=1`; un compte AppForge Cloud n’est pas requis.

## Déploiement

Cette PR prépare le Worker et les migrations. Elle ne déploie pas de Worker/D1 et ne certifie pas MVola en production. Il faut des droits Cloudflare du propriétaire du service.

1. Créer D1 `lifeos-licenses`; renseigner l’ID dans `worker/wrangler.toml`.
2. Appliquer les migrations jusqu’à `0006_cloud_entitlements.sql`. C’est une migration additive : elle ne supprime ni les licences ni les activations.
3. Garder les secrets Worker uniquement dans Wrangler : `LICENSE_SIGNING_PRIVATE_JWK`, `ADMIN_API_TOKEN`, `CODE_HASH_PEPPER` et `CLOUD_TOKEN_HMAC_SECRET`. La dernière valeur doit être identique sur le Worker Cloud LifeOS. Ne pas mettre cette valeur dans l’APK, le HTML, le fichier de configuration ou Git.
4. Déployer ce Worker et vérifier `GET /health`, `GET /v1/config`, activation après paiement confirmé, statut, renouvellement de jeton et refus après expiration/révocation/appareil retiré.
5. Déployer ensuite [le Worker Cloud V3](../appforge-cloud-sync/README.md) avec une liaison `LICENSES` vers ce même D1.

L’activation MVola reste fondée sur la validation de paiement existante côté administration : le Worker refuse une licence si `paid_confirmed` n’est pas vrai. La vérification d’une réponse d’API ne remplace pas un test sur une licence et un appareil réels.
