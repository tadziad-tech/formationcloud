package com.formationcloud.platform.dto;

import lombok.Data;

@Data
public class CategorieDTO {
    private Long id;
    private String nom;
    private String description;
    private String icone;
    private String couleur;
}
