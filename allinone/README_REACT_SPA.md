# FormationCloud — Frontend React (SPA)

## Prérequis
- Node.js 18+ (recommandé)
- Backend Spring Boot lancé sur http://localhost:8080

## Lancer en DEV (le plus simple)
```bash
cd frontend-react
npm install
npm run dev
```
Ouvrir: http://localhost:5173/#/login

Le proxy Vite forward /api vers le backend.

## Build PROD pour servir via Spring Boot
```bash
cd frontend-react
npm install
npm run build
```

Puis copier le contenu de `frontend-react/dist/` vers:
`<ton-projet-spring>/src/main/resources/static/app/`

Ensuite ouvrir:
http://localhost:8080/app/#/login

## Notes
- Auth: JWT stocké en sessionStorage (logout automatique à la fermeture de l'onglet)
- Front destiné à l'ADMIN (on étendra aux autres rôles ensuite)
