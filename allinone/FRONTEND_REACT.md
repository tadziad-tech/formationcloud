# Frontend React (seul)

Le projet ne contient **plus** l'ancien front HTML (supprimé). L'UI officielle est React.

## Lancer le projet (jury / démo)
1) Lance Spring Boot depuis Eclipse: **Run As > Spring Boot App**
2) Ouvre: `http://localhost:8080/app/`

React est déjà buildé dans: `src/main/resources/static/app/`

## Développement React
Dans `frontend-react/`:
```bash
npm install
npm run dev
```
UI: `http://localhost:5173/` (proxy vers Spring Boot pour `/api`).

## Rebuild React vers Spring Boot
Dans `frontend-react/`:
```bash
npm run build
```
Le build sort directement dans `src/main/resources/static/app/`.
