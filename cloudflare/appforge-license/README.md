# LifeOS licences AppForge

Worker de licences issu du kit AppForge Cloudflare de la Bibliothèque, adapté avec le produit `LIFEOS-V1`. La migration 0005 ajoute LifeOS sans altérer les produits historiques du kit.

## Déploiement

Aucun endpoint Worker ni secret de production n’a été trouvé dans la Bibliothèque. Le worker n’est pas déployé par cette PR.

1. Créer une base dédiée : `npx wrangler d1 create lifeos-licenses`; remplacer l’ID placeholder dans `worker/wrangler.toml`.
2. Configurer `APP_ORIGINS` avec les origines HTTPS exactes. Laisser vide jusqu’à validation de l’origine du WebView.
3. Dans `worker`, exécuter `npm install`, puis `npx wrangler d1 migrations apply lifeos-licenses --remote`.
4. Générer la clé P-256 : `node scripts/generate-signing-key.mjs`. Garder le JWK privé hors dépôt. Ajouter dans Wrangler les secrets `LICENSE_SIGNING_PRIVATE_JWK`, `ADMIN_API_TOKEN` et `CODE_HASH_PEPPER`.
5. Déployer, contrôler `/health` et `/v1/config`, puis créer les licences via routes `/admin/*`. L’essai est serveur 72 h, mensuel 30 jours, annuel 365 jours, à vie, propriétaire, et test interne 1 jour.
6. Paiement MVola confirmé manuellement avant l’émission d’un code client. La preuve signée locale permet l’usage hors ligne; la révocation est appliquée à la prochaine vérification en ligne.

Ne jamais placer les clés privées, le token admin, pepper ou une clé de signature dans le HTML, l’APK ou Git. L’URL seule ne prouve pas le déploiement. Le build LifeOS montre « non configuré » tant qu’une URL réelle n’est pas ajoutée dans l’app.
