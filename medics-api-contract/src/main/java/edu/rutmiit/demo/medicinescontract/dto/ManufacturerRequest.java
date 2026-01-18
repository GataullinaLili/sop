package edu.rutmiit.demo.medicinescontract.dto;

import jakarta.validation.constraints.*;

public record ManufacturerRequest(
        @NotBlank(message = "Название производителя не может быть пустым")
        @Size(min = 2, max = 100, message = "Название производителя должно содержать от 2 до 100 символов")
        String name,

        @NotBlank(message = "Страна не может быть пустой")
        @Size(min = 2, max = 50, message = "Название страны должно содержать от 2 до 50 символов")
        String country,

        @NotBlank(message = "Лицензионный номер не может быть пустым")
        @Size(min = 5, max = 50, message = "Лицензионный номер должен содержать от 5 до 50 символов")
        String licenseNumber,

        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        @Size(max = 100, message = "Email не может превышать 100 символов")
        String contactEmail
) {}