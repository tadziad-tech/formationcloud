package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

	private long totalFormations;
	private long formationsActives;
	private long totalUtilisateurs;
	private long totalAdmins;
	private long totalFormateurs;
	private long totalStagiaires;
	private long totalInscriptions;
	private long inscriptionsEnAttente;
	private long totalCertificats;
	private long totalEvaluations;
	private long notificationsNonLues;
}
