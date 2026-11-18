# AppForge Cloud pour LifeOS

Le parcours LifeOS V3 est désormais **sans compte client** : une licence active et éligible autorise le Worker de licences à émettre un jeton Cloud court (10 minutes), lié à l’appareil. Le Worker Cloud vérifie à chaque lecture/écriture l’état courant de la licence et l’activation de l’appareil, puis isole le snapshot dans D1 par identifiant de licence.

## Worker canonique

Le Worker à déployer pour LifeOS est [appforge-cloud-sync/worker](../appforge-cloud-sync/worker/README.md). Ce dossier historique contient les anciennes routes comptes / organisations. Elles restent présentes et leurs tables/migrations ne sont pas supprimées afin de préserver les données; le client LifeOS ne les appelle plus. Ne pas les présenter comme le parcours V3.

## Sources de règles

- [Standard AppForge V3](https://github.com/RMLala/android-template/blob/main/APPFORGE-STANDARD-V3.md)
- [PR de référence no 7](https://github.com/RMLala/android-template/pull/7)
- [Guide de déploiement Cloud et licences](../appforge-cloud-sync/README.md)

Les anciens snapshots par organisation ne sont pas automatiquement migrés vers les nouvelles lignes tenant par licence : une migration de données nécessite une décision et une association explicite à une licence. Aucun ancien compte, snapshot ou table n’est supprimé par ces changements.
