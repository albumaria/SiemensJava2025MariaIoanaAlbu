package com.siemens.internship.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String status;

    // adding email validation using the @Pattern validation of jakarta which uses regex matching
    // I've found that this is the most basic approach to validate fields of spring boot entities
    // It checks using the most general valid characters for each part of an email
    @Pattern(
            regexp = "^[A-Za-z0-9!#$%^&*{}|`'=?~._+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$"
    )
    private String email;
}