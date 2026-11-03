package ru.teamname.sportclub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.teamname.sportclub.enums.MemberStatus;

import java.time.LocalDate;

public record MemberRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+?[0-9\\- ()]{5,32}$", message = "должен быть корректным номером телефона") String phone,
        @Past LocalDate birthDate,
        MemberStatus status) {
}
