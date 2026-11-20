package ru.teamname.sportclub.booking.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** HTTP-клиент catalog-service. */
@FeignClient(name = "catalog-service", contextId = "catalogClient")
public interface CatalogClient {

    @GetMapping("/api/classes/{id}")
    ClassView getClass(@PathVariable("id") Long id);
}
