package ru.teamname.sportclub.booking.client.feign;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Что делает {@link CatalogClient}, когда catalog-service недоступен или цепь разомкнута. */
@Component
public class CatalogClientFallbackFactory implements FallbackFactory<CatalogClient> {

    private static final String SERVICE = "catalog-service";

    @Override
    public CatalogClient create(Throwable cause) {
        return new CatalogClient() {
            @Override
            public ClassView getClass(Long id) {
                throw FeignErrors.fallback(SERVICE, cause);
            }
        };
    }
}
