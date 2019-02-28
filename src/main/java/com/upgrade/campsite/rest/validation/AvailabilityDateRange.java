package com.upgrade.campsite.rest.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Annotate a dto at class level in order to validate a date range given by two separate date fields.
 * Validation corresponds to a date range used for reading calendar availability.
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = AvailabilityDateRangeValidator.class)
public @interface AvailabilityDateRange {

	public static final String INVALID_DATE_RANGE_MESSAGE = "The specified date range must be fully contained within a time span of 30 days counting from tomorrow. If no dates are specified the search will default to a 30 day span";

	String message() default INVALID_DATE_RANGE_MESSAGE;

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	String startDate();

	String endDate();
}
