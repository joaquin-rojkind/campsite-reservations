package com.upgrade.campsite.rest.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Annotate a dto at class level in order to validate a date range given by two separate date fields.
 * Validation corresponds to a date range used for making a reservation.
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = BookingDateRangeValidator.class)
public @interface BookingDateRange {

	public static final String INVALID_DATE_RANGE_MESSAGE = "A valid date range must be provided, it must not be longer than 3 days and it must be fully contained within a time span of 30 days counting from tomorrow";

	String message() default INVALID_DATE_RANGE_MESSAGE;

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	String arrivalDate();

	String departureDate();
}
