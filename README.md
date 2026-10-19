# NEON/WAVE · Portable Audio OS (Android / Web)

Projet Android Studio avec vraie bibliothèque MediaStore et une UI rétro PC-98 / Windows 98 / cyberpunk. 100% local : pas de serveur externe, pas de publicité, pas de télémétrie. Le mini-serveur HTTP est **strictement limité à 127.0.0.1** ; il diffuse les fichiers autorisés à la WebView via des URL protégées par un jeton de session, avec support des requêtes HTTP Range pour la lecture et l'avance rapide.

## Installation de l'application Android

1. Installer **Android Studio** (JDK 17+, Android SDK Platform 35).
2. `File > Open` → sélectionner le dossier contenant `settings.gradle` (ici `NEONWAVE_RELEASER`).
3. Laisser Android Studio télécharger Gradle 8.9, le plugin Android 8.7.3 et le SDK Android 35. Connexion Internet **uniquement lors de la compilation**.
4. Connecter un téléphone Android 8.0+ en USB (débogage activé) et lancer ▶ Run, ou `Build > Build APK(s)`.
5. APK de développement : `app/build/outputs/apk/debug/app-debug.apk` (après compilation sur ton ordinateur).
6. Au premier lancement, autoriser l'accès aux fichiers audio. Le scan ne retient que les pistes d'au moins **30 secondes**.

`./gradlew assembleDebug` sur macOS/Linux ou `gradlew.bat assembleDebug` sur Windows fonctionne si Java est installé. Le wrapper inclus est un **bootstrap minimal transparent**, dont le source est dans `tools/GradleWrapperMain.java` ; il récupère uniquement une distribution officielle `services.gradle.org`. Il peut être remplacé ensuite par `gradle wrapper --gradle-version 8.9`.

## Ce qui fonctionne

- Bibliothèque locale Android (MediaStore) : MP3, M4A, FLAC, WAV, OGG, selon les formats décodés par Android WebView. Les formats DRM ne sont pas pris en charge.
- Filtre d'une durée >=30 s ; affichage des pistes, des durées, des albums/artistes/dossiers, recherche.
- Import ponctuel de fichiers audio via le sélecteur Android, ou via le navigateur dans la démonstration web.
- Play / pause / piste précédente / suivante / lecture aléatoire / répétition d'une piste ou liste / volume / progression.
- Playlists conservées via le stockage interne de la WebView. Presets personnalisés sauvegardés.
- Effets non destructifs en temps réel avec Web Audio : vitesse et pitch indépendants (décalage granulaire créatif), réverbération à convolution, bass EQ, filtre passe-bas, distortion/crunch, bitcrusher, panoramique, autopan 8D, flutter cassette.
- 8 presets, spectre audio FFT, oscilloscopes, orbites et grille numérique animée, 4 thèmes de couleurs.
- Lecture hors ligne, sans connexion ni envoi de données.
- Notification de lecture Android avec premier plan (notification informative, sans télécommande audio).

## Limites honnêtes de cette version

- **APK non compilé ici** : cette machine ne dispose pas du SDK Android ni des dépendances nécessaires. Le code HTML/JavaScript est vérifié syntaxiquement ; l'intégration sur un téléphone physique nécessite compilation et essai dans Android Studio.
- Le pitch-shifter granulaire est volontairement *lo-fi* : sur de grands changements de hauteur il peut produire des artefacts.
- Le son est traité dans le moteur **Web Audio de la WebView**, pas dans un service audio natif. La lecture dans l'écran verrouillé / en arrière-plan varie selon téléphone et peut être interrompue lorsque le système suspend WebView. Une deuxième version avec moteur C++ Oboe / Media3 serait préférable pour une publication commerciale.
- Import de fichiers du navigateur : IndexedDB conserve les fichiers quand l'espace et les autorisations du navigateur le permettent. Sur Android, le scan MediaStore retrouve les pistes à chaque lancement.
- Les playlists ne déplacent ni ne suppriment les fichiers musicaux originaux.
- Pas de streaming YouTube/Spotify, pas d'export des fichiers traités dans cette version, pas de synchronisation cloud.
- Le serveur interne est sur le port fixe `27814` afin que les playlists et paramètres restent persistants d'une session à l'autre. Si ce port est occupé par une autre application, il devra être changé (les données de stockage WebView associées à l'ancien port ne suivront pas automatiquement).

## Personnalisation

- UI `app/src/main/assets/index.html`
- Graphisme et responsive `app/src/main/assets/style.css`
- Interface/audio DSP `app/src/main/assets/app.js`
- Scanner audio, flux audio à accès limité `LocalServer.java`
- Autorisations Android, conteneur UI, sélecteur de fichiers `MainActivity.java`
- Notification premier plan `MusicService.java`
- Changer l'identité et les noms dans ces fichiers avant de publier sur Google Play.

## Tester tout de suite sans Android Studio

Ouvrir `NEONWAVE_DEMO.html` sur ordinateur dans Chrome/Edge/Firefox, importer des fichiers audio d'une durée > 30 secondes, puis tester les presets et visualiseurs. La démo HTML est autonome. Sur Android, préférer l'application native pour scanner toutes les musiques automatiquement.
