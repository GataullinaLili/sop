package edu.rutmiit.demo.medicinescontract.endpoints;

import edu.rutmiit.demo.medicinescontract.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "manufacturers", description = "API для работы с производителями лекарств")
@RequestMapping("/api/manufacturers")
public interface ManufacturerApi {

    @Operation(summary = "Получить всех производителей")
    @ApiResponse(responseCode = "200", description = "Список производителей")
    @GetMapping
    CollectionModel<EntityModel<ManufacturerResponse>> getAllManufacturers();

    @Operation(summary = "Получить производителя по ID")
    @ApiResponse(responseCode = "200", description = "Производитель найден")
    @ApiResponse(responseCode = "404", description = "Производитель не найден",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @GetMapping("/{id}")
    EntityModel<ManufacturerResponse> getManufacturerById(@PathVariable Long id);

    @Operation(summary = "Создать нового производителя")
    @ApiResponse(responseCode = "201", description = "Производитель успешно создан")
    @ApiResponse(responseCode = "400", description = "Невалидный запрос",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<ManufacturerResponse>> createManufacturer(@Valid @RequestBody ManufacturerRequest request);

    @Operation(summary = "Обновить производителя")
    @ApiResponse(responseCode = "200", description = "Производитель обновлен")
    @ApiResponse(responseCode = "404", description = "Производитель не найден",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @PutMapping("/{id}")
    EntityModel<ManufacturerResponse> updateManufacturer(@PathVariable Long id, @Valid @RequestBody UpdateManufacturerRequest request);

    @Operation(summary = "Удалить производителя")
    @ApiResponse(responseCode = "204", description = "Производитель удален")
    @ApiResponse(responseCode = "404", description = "Производитель не найден")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteManufacturer(@PathVariable Long id);
}