package edu.rutmiit.demo.medicinescontract.endpoints;

import edu.rutmiit.demo.medicinescontract.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "medications", description = "API для работы с лекарственными препаратами")
@RequestMapping("/api/medications")
public interface MedicationApi {

    @Operation(summary = "Получить лекарство по ID")
    @ApiResponse(responseCode = "200", description = "Лекарство найдено")
    @ApiResponse(responseCode = "404", description = "Лекарство не найдено",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @GetMapping("/{id}")
    EntityModel<MedicationResponse> getMedicationById(@PathVariable("id") Long id);

    @Operation(summary = "Получить список всех лекарств с фильтрацией и пагинацией")
    @ApiResponse(responseCode = "200", description = "Список лекарств")
    @GetMapping
    PagedModel<EntityModel<MedicationResponse>> getAllMedications(
            @Parameter(description = "Фильтр по ID производителя") @RequestParam(required = false) Long manufacturerId,
            @Parameter(description = "Фильтр по коду АТХ") @RequestParam(required = false) String atcCode,
            @Parameter(description = "Поиск по названию или МНН") @RequestParam(required = false) String search,
            @Parameter(description = "Номер страницы (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "Получить рецептурные лекарства")
    @ApiResponse(responseCode = "200", description = "Список рецептурных лекарств")
    @GetMapping("/prescription")
    PagedModel<EntityModel<MedicationResponse>> getPrescriptionMedications(
            @Parameter(description = "Номер страницы (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "Получить безрецептурные лекарства")
    @ApiResponse(responseCode = "200", description = "Список безрецептурных лекарств")
    @GetMapping("/over-the-counter")
    PagedModel<EntityModel<MedicationResponse>> getOverTheCounterMedications(
            @Parameter(description = "Номер страницы (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "Создать новое лекарство")
    @ApiResponse(responseCode = "201", description = "Лекарство успешно создано")
    @ApiResponse(responseCode = "400", description = "Невалидный запрос",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @ApiResponse(responseCode = "409", description = "Лекарство с таким МНН уже существует",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<MedicationResponse>> createMedication(@Valid @RequestBody MedicationRequest request);

    @Operation(summary = "Обновить лекарство по ID")
    @ApiResponse(responseCode = "200", description = "Лекарство успешно обновлено")
    @ApiResponse(responseCode = "404", description = "Лекарство не найдено",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @ApiResponse(responseCode = "409", description = "Лекарство с таким МНН уже существует",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PutMapping("/{id}")
    EntityModel<MedicationResponse> updateMedication(@PathVariable Long id, @Valid @RequestBody UpdateMedicationRequest request);

    @Operation(summary = "Удалить лекарство по ID")
    @ApiResponse(responseCode = "204", description = "Лекарство успешно удалено")
    @ApiResponse(responseCode = "404", description = "Лекарство не найдено")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMedication(@PathVariable Long id);

    @Operation(summary = "Обновить рецептурный статус лекарства")
    @ApiResponse(responseCode = "200", description = "Рецептурный статус обновлен")
    @ApiResponse(responseCode = "404", description = "Лекарство не найдено")
    @PatchMapping("/{id}/prescription-status")
    EntityModel<MedicationResponse> updatePrescriptionStatus(
            @PathVariable Long id,
            @Parameter(description = "Рецептурный статус") @RequestParam boolean prescriptionRequired);
}