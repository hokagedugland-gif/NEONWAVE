# NEON/WAVE : avoir une vraie application Android (.apk) sans Android Studio

Ce dossier contient le projet de l'application, et non un fichier `.apk` déjà compilé. Tu peux néanmoins obtenir un APK installable depuis GitHub en laissant les serveurs GitHub compiler le projet (GitHub Actions). L'exécution de la compilation n'a pas été vérifiée sur ce système, faute de SDK Android et d'accès aux dépendances ; si la compilation échoue, consulte le détail de l'erreur dans Actions.

## Première compilation dans GitHub (ordinateur Windows, macOS ou Linux)

1. Connecte-toi sur https://github.com et crée un **nouveau dépôt privé** (bouton `New repository`). Ne coche pas la création automatique du README.
2. Décompresse `NEONWAVE_Android_Pret_pour_APK.zip` sur ton ordinateur. Ouvre le dossier `NEONWAVE_RELEASER` qui apparaît.
3. Sur la page de ton dépôt, clique `uploading an existing file` ou `Add file` > `Upload files`.
4. Envoie **le contenu du dossier** `NEONWAVE_RELEASER`, pas le fichier ZIP lui-même. Important : le dossier `.github/workflows/android-apk.yml` doit être présent dans le dépôt, ainsi que les dossiers `app/` et `gradle/`, et `settings.gradle` à la racine. Si le site ne permet pas de déposer un dossier, utilise GitHub Desktop pour publier le dossier au complet. GitHub peut demander de confirmer une autorisation pour ajouter un workflow.
5. Clique `Commit changes` puis ouvre l'onglet **Actions**.
6. Clique le workflow `Construire NEONWAVE APK`. S'il n'a pas démarré automatiquement, clique `Run workflow`.
7. Attends la fin de la compilation, identifiée par une coche verte. Ouvre l'exécution, puis télécharge l'artifact **NEONWAVE-Android-APK** en bas de la page.
8. Décompresse l'artifact : tu trouveras **app-debug.apk**. Ce fichier est **un vrai APK Android installable**, signé avec la clé de développement par l'outil de compilation.
9. Copie `app-debug.apk` sur ton téléphone (USB, Drive, etc.). Ouvre-le depuis `Fichiers` puis autorise, si Android le propose, les installations depuis cette source. Lance NEON/WAVE et autorise l'accès audio.

## Après la première compilation

À chaque modification du projet et nouvel envoi dans `main` ou `master`, GitHub pourra produire un nouvel APK. Tu peux aussi lancer une compilation manuellement depuis l'onglet Actions.

## Remarques importantes

- **L'APK n'est pas fourni dans ce ZIP** : il doit encore être construit sur GitHub ou sur un ordinateur équipé du SDK Android.
- Une fois installé, le lecteur est hors ligne. Les fichiers locaux restent sur le téléphone ; le serveur interne de l'application écoute uniquement sur `127.0.0.1`.
- Il s'agit d'une **version de test** : toutes les fonctions ne sont pas garanties sur tous les téléphones. En particulier, le son en arrière-plan peut être interrompu lorsque le système suspend la WebView, et le pitch granulaire peut dégrader la qualité audio.
- Un `app-debug.apk` est adapté aux essais personnels, pas directement à une publication Play Store. Pour une diffusion publique, créer une véritable version release signée avec sa propre clé et faire des tests sur appareil.
- Ne télécharge pas d'APK généré par un tiers inconnu ; récupère celui compilé depuis **ton propre dépôt** et ton workflow.
