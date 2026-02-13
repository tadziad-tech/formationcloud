# FormationCloud — Backend Spring Boot + Front React (Tout-en-un)

Ce projet contient :
- Backend Spring Boot (Maven) : à la racine (pom.xml)
- Front React (Vite) : dossier frontend-react/

Identifiants de test
- Admin : admin@formationcloud.com
- Mot de passe : password

---

## 1) Lancer en DEV

### Backend (Eclipse)
1. Eclipse -> File -> Import -> Maven -> Existing Maven Projects
2. Sélectionne le dossier racine du projet (celui qui contient pom.xml)
3. Finish
4. Ouvre la classe d'entrée : src/main/java/.../FormationcloudPlatformApplication.java (ou *Application.java)
5. Run As -> Spring Boot App (ou Java Application)
6. Vérifie que le backend répond : http://localhost:8080/login.html

### Front React (DEV)
1. Ouvre CMD dans le dossier frontend-react/
2. npm install
3. npm run dev
4. Ouvre : http://localhost:5173/#/login

Remarque : le front React appelle l'API via /api et Vite fait un proxy vers http://localhost:8080 (pas de CORS à gérer).

---

## 2) Mode DEMO (servir React depuis Spring Boot)

1. Va dans frontend-react/
2. Exécute build-to-spring.bat
3. Redémarre Spring Boot
4. Ouvre : http://localhost:8080/app/#/login

EOF
