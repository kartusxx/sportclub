package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.teamname.sportclub.booking.client.ClassDirectory;
import ru.teamname.sportclub.booking.client.ClassInfo;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;

/** Реализация порта {@link ClassDirectory} поверх Feign-клиента. */
@Component
@RequiredArgsConstructor
public class FeignClassDirectory implements ClassDirectory {

    private final CatalogClient catalogClient;

    @Override
    public ClassInfo getClassInfo(Long classId) {
        try {
            ClassView view = catalogClient.getClass(classId);
            return new ClassInfo(view.id(), view.title(), view.startsAt(), view.capacity());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Занятие", classId);
        } catch (FeignException e) {
            throw FeignErrors.unavailable("catalog-service", e);
        }
    }
}
