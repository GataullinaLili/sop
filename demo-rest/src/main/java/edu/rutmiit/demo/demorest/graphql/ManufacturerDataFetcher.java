package edu.rutmiit.demo.demorest.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.demo.medicinescontract.dto.ManufacturerRequest;
import edu.rutmiit.demo.medicinescontract.dto.ManufacturerResponse;
import edu.rutmiit.demo.demorest.service.ManufacturerService;

import java.util.List;
import java.util.Map;

@DgsComponent
public class ManufacturerDataFetcher {

    private final ManufacturerService manufacturerService;

    public ManufacturerDataFetcher(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    @DgsQuery
    public List<ManufacturerResponse> manufacturers() {
        return manufacturerService.findAll();
    }

    @DgsQuery
    public ManufacturerResponse manufacturerById(@InputArgument Long id) {
        return manufacturerService.findById(id);
    }

    @DgsMutation
    public ManufacturerResponse createManufacturer(@InputArgument("input") Map<String, String> input) {
        ManufacturerRequest request = new ManufacturerRequest(
                input.get("name"),
                input.get("country"),
                input.get("licenseNumber"),
                input.get("contactEmail")
        );
        return manufacturerService.create(request);
    }

    @DgsMutation
    public ManufacturerResponse updateManufacturer(@InputArgument Long id,
                                                   @InputArgument("input") Map<String, String> input) {
        ManufacturerRequest request = new ManufacturerRequest(
                input.get("name"),
                input.get("country"),
                input.get("licenseNumber"),
                input.get("contactEmail")
        );
        return manufacturerService.update(id, request);
    }

    @DgsMutation
    public Long deleteManufacturer(@InputArgument Long id) {
        manufacturerService.delete(id);
        return id;
    }
}