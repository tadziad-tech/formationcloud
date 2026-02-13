package com.formationcloud.platform.bootstrap;

import com.formationcloud.platform.model.*;
import com.formationcloud.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seed de démo (uniquement si la base est vide).
 * Objectif : permettre de visualiser les dashboards immédiatement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final CategorieRepository categorieRepository;
    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EvaluationRepository evaluationRepository;
    private final ResultatEvaluationRepository resultatEvaluationRepository;
    private final CertificatRepository certificatRepository;
    private final TacheRepository tacheRepository;
    private final NotificationRepository notificationRepository;

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // ✅ on seed UNIQUEMENT si la base est totalement vide (aucun utilisateur)
        if (utilisateurRepository.count() > 0) {
            return;
        }

        log.info("[DEMO SEED] Base vide détectée → insertion de données démo...");

        // ===== Catégories =====
        Categorie catJava = new Categorie();
        catJava.setNom("Java");
        catJava.setDescription("Programmation Java, Spring & bonnes pratiques");
        catJava.setIcone("fa-coffee");
        catJava.setCouleur("#f89820");

        Categorie catPython = new Categorie();
        catPython.setNom("Python");
        catPython.setDescription("Data, IA, automatisation et scripts");
        catPython.setIcone("fa-python");
        catPython.setCouleur("#3776ab");

        Categorie catDevOps = new Categorie();
        catDevOps.setNom("DevOps");
        catDevOps.setDescription("CI/CD, Docker, monitoring et cloud");
        catDevOps.setIcone("fa-server");
        catDevOps.setCouleur("#0db7ed");

        Categorie catUml = new Categorie();
        catUml.setNom("UML");
        catUml.setDescription("Modélisation, conception et architecture");
        catUml.setIcone("fa-project-diagram");
        catUml.setCouleur("#5c4ee5");
        categorieRepository.saveAll(List.of(catJava, catPython, catDevOps, catUml));

        // ===== Users =====
        final String pwd = passwordEncoder.encode("password");

        Utilisateur admin = new Utilisateur();
        admin.setNom("Admin");
        admin.setPrenom("Super");
        admin.setEmail("admin@formationcloud.com");
        admin.setMotDePasse(pwd);
        admin.setRole(Role.ADMIN);
        admin.setStatutValidation(true);
        admin.setActif(true);

        Utilisateur formateur1 = new Utilisateur();
        formateur1.setNom("Dupont");
        formateur1.setPrenom("Jean");
        formateur1.setEmail("formateur@formationcloud.com");
        formateur1.setMotDePasse(pwd);
        formateur1.setRole(Role.FORMATEUR);
        formateur1.setTypeFormateur(TypeFormateur.INTERNE);
        formateur1.setStatutValidation(true);
        formateur1.setActif(true);

        Utilisateur formateur2 = new Utilisateur();
        formateur2.setNom("Bernard");
        formateur2.setPrenom("Pierre");
        formateur2.setEmail("formateur2@formationcloud.com");
        formateur2.setMotDePasse(pwd);
        formateur2.setRole(Role.FORMATEUR);
        formateur2.setTypeFormateur(TypeFormateur.EXTERNE);
        formateur2.setStatutValidation(true);
        formateur2.setActif(true);

        Utilisateur stag1 = new Utilisateur();
        stag1.setNom("Martin");
        stag1.setPrenom("Marie");
        stag1.setEmail("stagiaire@formationcloud.com");
        stag1.setMotDePasse(pwd);
        stag1.setRole(Role.STAGIAIRE);
        stag1.setStatutValidation(true);
        stag1.setActif(true);

        Utilisateur stag2 = new Utilisateur();
        stag2.setNom("Dubois");
        stag2.setPrenom("Sophie");
        stag2.setEmail("stagiaire2@formationcloud.com");
        stag2.setMotDePasse(pwd);
        stag2.setRole(Role.STAGIAIRE);
        stag2.setStatutValidation(true);
        stag2.setActif(true);

        Utilisateur stag3 = new Utilisateur();
        stag3.setNom("El Amrani");
        stag3.setPrenom("Youssef");
        stag3.setEmail("stagiaire3@formationcloud.com");
        stag3.setMotDePasse(pwd);
        stag3.setRole(Role.STAGIAIRE);
        stag3.setStatutValidation(false);
        stag3.setActif(true);

        utilisateurRepository.saveAll(List.of(admin, formateur1, formateur2, stag1, stag2, stag3));

        // ===== Formations (dates récentes) =====
        LocalDate today = LocalDate.now();

        Formation f1 = new Formation();
        f1.setNom("Java Débutant");
        f1.setDescription("Démarrez Java proprement : POO, collections, exceptions, bonnes pratiques.");
        f1.setType(TypeFormation.PRESENTIELLE);
        f1.setCategorie(catJava);
        f1.setCapaciteMax(30);
        f1.setFormateur(formateur1);
        f1.setStatut(StatutFormation.ACTIVE);
        f1.setDateDebut(today.minusDays(10));
        f1.setDateFin(today.plusDays(12));
        f1.setLieu("Salle A");
        f1.setDureeHeures(40);
        f1.setPrix(new BigDecimal("500.00"));

        Formation f2 = new Formation();
        f2.setNom("Spring Boot Pro");
        f2.setDescription("API REST, sécurité, bonnes pratiques, architecture et déploiement.");
        f2.setType(TypeFormation.PRESENTIELLE);
        f2.setCategorie(catJava);
        f2.setCapaciteMax(25);
        f2.setFormateur(formateur1);
        f2.setStatut(StatutFormation.ACTIVE);
        f2.setDateDebut(today.minusDays(2));
        f2.setDateFin(today.plusDays(20));
        f2.setLieu("Salle B");
        f2.setDureeHeures(60);
        f2.setPrix(new BigDecimal("800.00"));

        Formation f3 = new Formation();
        f3.setNom("Python Data Science");
        f3.setDescription("Pandas, visualisation, préparation de données et mini-projets.");
        f3.setType(TypeFormation.A_DISTANCE);
        f3.setCategorie(catPython);
        f3.setCapaciteMax(30);
        f3.setFormateur(formateur2);
        f3.setStatut(StatutFormation.ACTIVE);
        f3.setDateDebut(today.minusDays(7));
        f3.setDateFin(today.plusDays(25));
        f3.setLieu("En ligne");
        f3.setDureeHeures(50);
        f3.setPrix(new BigDecimal("600.00"));

        Formation f4 = new Formation();
        f4.setNom("DevOps Essentials");
        f4.setDescription("Docker, CI/CD, bases cloud et bonnes pratiques d'exploitation.");
        f4.setType(TypeFormation.PRESENTIELLE);
        f4.setCategorie(catDevOps);
        f4.setCapaciteMax(20);
        f4.setFormateur(formateur2);
        f4.setStatut(StatutFormation.ACTIVE);
        f4.setDateDebut(today.plusDays(3));
        f4.setDateFin(today.plusDays(16));
        f4.setLieu("Salle C");
        f4.setDureeHeures(45);
        f4.setPrix(new BigDecimal("700.00"));

        formationRepository.saveAll(List.of(f1, f2, f3, f4));

        // ===== Inscriptions =====
        Inscription i1 = new Inscription();
        i1.setStagiaire(stag1);
        i1.setFormation(f1);
        i1.setStatut(StatutInscription.CONFIRMEE);
        i1.setDateValidation(LocalDateTime.now().minusDays(8));

        Inscription i2 = new Inscription();
        i2.setStagiaire(stag2);
        i2.setFormation(f1);
        i2.setStatut(StatutInscription.EN_COURS);
        i2.setDateValidation(LocalDateTime.now().minusDays(6));

        Inscription i3 = new Inscription();
        i3.setStagiaire(stag1);
        i3.setFormation(f3);
        i3.setStatut(StatutInscription.EN_COURS);
        i3.setDateValidation(LocalDateTime.now().minusDays(5));

        Inscription i4 = new Inscription();
        i4.setStagiaire(stag2);
        i4.setFormation(f4);
        i4.setStatut(StatutInscription.EN_ATTENTE);

        Inscription i5 = new Inscription();
        i5.setStagiaire(stag3);
        i5.setFormation(f2);
        i5.setStatut(StatutInscription.EN_ATTENTE);

        inscriptionRepository.saveAll(List.of(i1, i2, i3, i4, i5));

        // Backdate pour une courbe "vivante" (7/14/30j)
        backdateInscription(i1.getId(), LocalDateTime.now().minusDays(9));
        backdateInscription(i2.getId(), LocalDateTime.now().minusDays(7));
        backdateInscription(i3.getId(), LocalDateTime.now().minusDays(4));
        backdateInscription(i4.getId(), LocalDateTime.now().minusDays(2));
        backdateInscription(i5.getId(), LocalDateTime.now().minusDays(1));

        // ===== Evaluations =====
        Evaluation e1 = new Evaluation();
        e1.setFormation(f1);
        e1.setTitre("Quiz Java - Bases");
        e1.setDescription("Évaluation des fondamentaux Java (POO + collections)");
        e1.setSeuilReussite(new BigDecimal("10.00"));
        e1.setDateEvaluation(today.minusDays(3));
        e1.setDureeMinutes(60);

        Evaluation e2 = new Evaluation();
        e2.setFormation(f3);
        e2.setTitre("Mini-projet Data Science");
        e2.setDescription("Analyse et visualisation sur un dataset réel");
        e2.setSeuilReussite(new BigDecimal("12.00"));
        e2.setDateEvaluation(today.plusDays(5));
        e2.setDureeMinutes(120);

        evaluationRepository.saveAll(List.of(e1, e2));

        // ===== Résultats =====
        ResultatEvaluation r1 = new ResultatEvaluation();
        r1.setEvaluation(e1);
        r1.setStagiaire(stag1);
        r1.setNote(java.math.BigDecimal.valueOf(15.5));
        r1.setReussi(true);
        r1.setCommentaire("Très bien 👍");

        ResultatEvaluation r2 = new ResultatEvaluation();
        r2.setEvaluation(e1);
        r2.setStagiaire(stag2);
        r2.setNote(java.math.BigDecimal.valueOf(13.75));
        r2.setReussi(true);
        r2.setCommentaire("Bon niveau, continue !");

        resultatEvaluationRepository.saveAll(List.of(r1, r2));

        // ===== Certificats =====
        Certificat c1 = Certificat.builder()
                .stagiaire(stag1)
                .formation(f1)
                .dateObtention(today.minusDays(2))
                .noteFinale(new BigDecimal("15.50"))
                .build();

        Certificat c2 = Certificat.builder()
                .stagiaire(stag2)
                .formation(f1)
                .dateObtention(today.minusDays(2))
                .noteFinale(new BigDecimal("13.75"))
                .build();

        certificatRepository.saveAll(List.of(c1, c2));

        // ===== Tâches =====
        Tache t1 = new Tache();
        t1.setTitre("TP Java - POO");
        t1.setDescription("Créer une mini app avec classes, héritage et interfaces");
        t1.setStagiaire(stag1);
        t1.setFormation(f1);
        t1.setPourcentageAccomplissement(100);
        t1.setStatut(StatutTache.TERMINEE);
        t1.setDateDebut(today.minusDays(9));
        t1.setDateFin(today.minusDays(5));

        Tache t2 = new Tache();
        t2.setTitre("Projet Python - Nettoyage dataset");
        t2.setDescription("Nettoyage + EDA sur un dataset Kaggle");
        t2.setStagiaire(stag1);
        t2.setFormation(f3);
        t2.setPourcentageAccomplissement(60);
        t2.setStatut(StatutTache.EN_COURS);
        t2.setDateDebut(today.minusDays(5));
        t2.setDateFin(today.plusDays(3));

        Tache t3 = new Tache();
        t3.setTitre("Pipeline CI/CD - Docker" );
        t3.setDescription("Build + test + deploy (demo)" );
        t3.setStagiaire(stag2);
        t3.setFormation(f4);
        t3.setPourcentageAccomplissement(20);
        t3.setStatut(StatutTache.ASSIGNEE);
        t3.setDateDebut(today.minusDays(2));
        t3.setDateFin(today.plusDays(6));

        tacheRepository.saveAll(List.of(t1, t2, t3));

        // ===== Notifications ===== (liens avec formationId pour deep-link admin)
        notificationRepository.saveAll(List.of(
                new Notification(null, stag1, TypeNotification.CERTIFICAT_OBTENU,
                        "Félicitations ! Certificat obtenu pour Java Débutant 🎉", false, null, null, "/formations?formationId=" + f1.getId()),
                new Notification(null, stag2, TypeNotification.TACHE_ASSIGNEE,
                        "Nouvelle tâche assignée : Pipeline CI/CD - Docker", false, null, null, "/formations?formationId=" + f4.getId()),
                new Notification(null, formateur1, TypeNotification.NOUVELLE_INSCRIPTION,
                        "Nouvelle inscription en attente : " + stag3.getPrenom() + " " + stag3.getNom(), false, null, null, "/formations?formationId=" + f3.getId()),
                // Notifications pour admin (toujours formationId pour ouvrir la bonne formation)
                new Notification(null, admin, TypeNotification.INSCRIPTION_EN_ATTENTE_ADMIN,
                        "Nouvelle demande d'inscription: " + stag3.getPrenom() + " " + stag3.getNom() + " pour la formation 'Python Data Science'", false, null, null, "/formations?formationId=" + f3.getId()),
                new Notification(null, admin, TypeNotification.PRESENCE_A_COMPLETER,
                        "Présence à compléter pour la séance 'Introduction Python' (Formation: Python Data Science)", false, null, null, "/formations?formationId=" + f3.getId() + "&tab=seances"),
                new Notification(null, admin, TypeNotification.TP_DEADLINE_PROCHE,
                        "TP 'Projet Data Science' : deadline dans moins de 72h (Formation: Python Data Science)", false, null, null, "/formations?formationId=" + f3.getId() + "&tab=ressources")
        ));

        log.info("[DEMO SEED] OK ✅ Données démo insérées.");
    }

    private void backdateInscription(Long inscriptionId, LocalDateTime dateInscription) {
        if (inscriptionId == null || dateInscription == null) return;
        jdbcTemplate.update(
                "UPDATE inscription SET date_inscription = ? WHERE id = ?",
                Timestamp.valueOf(dateInscription),
                inscriptionId
        );
    }
}
