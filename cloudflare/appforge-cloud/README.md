# LifeOS AppForge Cloud

Service Workers + D1 offline-first adapté du kit AppForge Cloud de la Bibliothèque. La base Cloud est séparée de celle des licences. Le snapshot LifeOS est synchronisé après sauvegarde locale; un conflit de révision n’écrase jamais le serveur automatiquement.

## Déploiement

Prérequis : compte Cloudflare, Node.js, Wrangler et une base D1 dédiée. Aucune URL de Worker ou secret Cloudflare n’était disponible dans la Bibliothèque; ce service n’est donc pas déclaré déployé.

1. Créer D1 avec `npx wrangler d1 create lifeos-cloud`, puis remplacer l’ID placeholder dans `worker/wrangler.toml`.
2. Renseigner les origines HTTPS exactes dans `APP_ORIGINS`. Pour l’APK Capacitor, confirmer l’origine réelle du WebView avant de l’ajouter (souvent `https://localhost`). Le HTML ouvert en `file://` ne peut pas appeler le Worker.
3. Depuis `worker`, exécuter `npm install`, puis `npx wrangler d1 migrations apply lifeos-cloud --remote`.
4. Configurer `BOOTSTRAP_SECRET` par `npx wrangler secret put BOOTSTRAP_SECRET`, avec au moins 32 caractères générés localement, puis `npx wrangler deploy`.
5. Créer une seule fois l’organisation propriétaire via `POST /v1/bootstrap` avec l’en-tête `X-Bootstrap-Secret`. Retirer aussitôt le secret (`npx wrangler secret delete BOOTSTRAP_SECRET`). Ne pas enregistrer les secrets dans Git.
6. Vérifier `GET /health`, le login, l’organisation et un aller-retour snapshot sur deux appareils avant de valider le cloud.

Les comptes secondaires nécessitent une invitation par un owner/admin. Les invitations et codes de réinitialisation sont remis manuellement (pas d’e-mail sortant intégré). D1 stocke le snapshot JSON (limite 500 Ko), sans chiffrement de bout en bout. Quota configurable : 5 000 requêtes snapshot/utilisateur/jour par défaut.

## Routes et sécurité

Voir `worker/src/index.js` : bootstrap one-shot, login mot de passe PBKDF2, sessions révocables de 30 jours, rôles organisationnels, invitations, quota, snapshots avec révision optimiste et réponse 409. Le client conserve le token en mémoire de session seulement. Les données locales restent la source disponible hors ligne.
