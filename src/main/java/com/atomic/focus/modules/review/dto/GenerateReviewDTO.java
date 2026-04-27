package com.atomic.focus.modules.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateReviewDTO {

    /** weekly | monthly */
    @NotBlank
    private String scope;

    private Boolean force = false;
}
