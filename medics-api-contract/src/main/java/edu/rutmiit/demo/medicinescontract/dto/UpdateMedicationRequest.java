package edu.rutmiit.demo.medicinescontract.dto;

import jakarta.validation.constraints.*;

public record UpdateMedicationRequest(
        @NotBlank(message = "Название лекарства не может быть пустым")
        @Size(min = 2, max = 200, message = "Название лекарства должно содержать от 2 до 200 символов")
        String name,

        @NotBlank(message = "Международное непатентованное наименование не может быть пустым")
        @Size(min = 2, max = 100, message = "МНН должно содержать от 2 до 100 символов")
        String inn,

        @Size(min = 1, max = 50, message = "Код АТХ должен содержать 1-50 символов")
        String atcCode,

        @Size(max = 500, message = "Условия хранения не могут превышать 500 символов")
        String storageConditions,

        @Min(value = 0, message = "Срок годности не может быть отрицательным")
        @Max(value = 120, message = "Срок годности не может превышать 120 месяцев")
        Integer shelfLifeMonths
) {}