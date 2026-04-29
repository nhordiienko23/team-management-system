package com.nba.dto;

import com.nba.model.Position;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StaffDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllFieldsCorrect_thenNoViolations() {
        StaffDto dto = new StaffDto();
        dto.type = "player";
        dto.name = "LeBron";
        dto.baseSalary = 50000;
        dto.rating = 95;
        dto.positions = List.of(Position.SF);

        Set<ConstraintViolation<StaffDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenTypeIsBlank_thenViolation() {
        StaffDto dto = new StaffDto();
        dto.type = "";
        dto.baseSalary = 1000;

        Set<ConstraintViolation<StaffDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals("Type is mandatory", violations.iterator().next().getMessage());
    }

    @Test
    void whenSalaryIsNegative_thenViolation() {
        StaffDto dto = new StaffDto();
        dto.type = "player";
        dto.baseSalary = -100;

        Set<ConstraintViolation<StaffDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("greater than zero")));
    }

    @Test
    void whenRatingIsInvalid_thenViolation() {
        StaffDto dto = new StaffDto();
        dto.type = "player";
        dto.baseSalary = 1000;
        dto.rating = 150;

        Set<ConstraintViolation<StaffDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}