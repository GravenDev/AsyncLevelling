# Politique de Confidentialité — AsyncLevelling

**Dernière mise à jour : 15 juin 2026**

## 1. Collecte et utilisation des données

Le bot AsyncLevelling collecte et stocke les données suivantes pour assurer le suivi des niveaux et de l'expérience des membres sur le serveur Discord « Async - Community » :

### Données stockées en base de données (PostgreSQL)

| Type de donnée | Champ | Finalité |
|---|---|---|
| Identifiant Discord de l'utilisateur | `UserDao.id` | Identification unique du membre |
| Identifiant Discord du serveur | `GuildDao.id` | Association aux configurations du serveur |
| Niveau actuel du membre | `MemberDao.level` | Calcul du niveau et classement |
| Expérience totale du membre | `MemberDao.experience` | Progression vers le niveau suivant |
| Date et heure du dernier message | `MemberDao.lastMessageAt` | Application du délai de récupération (timeout) entre les gains d'XP |
| Préférences de notification du serveur | `GuildSettingsDao` | Configuration des notifications (channel, DM, activation, messages personnalisés) |
| Récompenses de rôle par niveau | `GuildRewardDao` | Attribution automatique des rôles aux paliers de niveau |

### Données temporairement traitées (non stockées)

- **Contenu des messages** : Seule la longueur (`length()`) du message est lue pour calculer l'XP gagnée. Le contenu textuel n'est **jamais** stocké ni persistant.
- **Avatars** : Téléchargés temporairement depuis le CDN Discord pour générer les images de niveau et de classement. Ils sont supprimés immédiatement après la génération de l'image.
- **États vocaux** : Vérifiés en temps réel pour l'attribution d'XP vocal (non-sourd, non-mute, nombre de participants). Aucune donnée vocale n'est persistée.

## 2. Partage des données

Aucune donnée collectée n'est partagée avec des tiers. Les données sont exclusivement utilisées par le bot AsyncLevelling au sein du serveur Discord « Async - Community ».

## 3. Conservation des données

Les données sont conservées **indéfiniment** tant que l'utilisateur est membre du serveur ou jusqu'à demande explicite de suppression. Il n'existe pas de mécanisme automatique d'expiration ou de purge périodique.

## 4. Suppression des données

Les utilisateurs peuvent demander la suppression de leurs données d'activité en contactant l'équipe du serveur :

- **GitHub (mainteneurs)** : [RedsTom](https://github.com/RedsTom) — [AntoineJT](https://github.com/AntoineJT)
- **Serveur Discord** : [discord.gg/graven](https://discord.gg/graven) (ouvrir un ticket auprès du staff)

Une fois la demande reçue, les données associées à l'utilisateur seront supprimées manuellement de la base de données dans un délai raisonnable.

## 5. Sécurité

- Les données sont stockées sur une base PostgreSQL.
- **Aucun chiffrement au repos** n'est actuellement appliqué aux données stockées (conformément à la politique développeur Discord, des mesures sont en cours d'évaluation).
- L'accès à la base de données est protégé par des identifiants définis via les variables d'environnement `DB_USER`, `DB_PASS` et `DB_URL`.
- Le token du bot est stocké via la variable d'environnement `BOT_TOKEN` et n'est jamais exposé.

## 6. Non-participation (opt-out)

Il n'existe pas de mécanisme permettant à un utilisateur individuel de se soustraire au suivi d'activité. Les administrateurs du serveur peuvent cependant **mettre en pause** l'ensemble du bot via la commande `/settings pause`.

## 7. Intelligence artificielle

Aucune donnée collectée n'est utilisée pour l'entraînement de modèles d'intelligence artificielle ou d'apprentissage automatique (ML/AI).

## 8. Contact

Pour toute question relative à cette politique de confidentialité ou pour exercer vos droits :

- **Serveur Discord** : [discord.gg/graven](https://discord.gg/graven)
- **Organisation GitHub** : [AsyncCommunityDiscord](https://github.com/AsyncCommunityDiscord)

## 9. Modifications

Cette politique de confidentialité peut être mise à jour à tout moment. Les utilisateurs seront informés des modifications substantielles via le serveur Discord.
