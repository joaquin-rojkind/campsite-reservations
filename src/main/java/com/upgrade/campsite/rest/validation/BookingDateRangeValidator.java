package com.upgrade.campsite.rest.validation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.BeanWrapperImpl;

import com.upgrade.campsite.calendar.Calendar;


public class BookingDateRangeValidator implements ConstraintValidator<BookingDateRange, Object> {

	private List<BiPredicate<LocalDate, LocalDate>> validationRules;
	private String arrivalDateName;
	private String departureDateName;

	@Override
	public void initialize(final BookingDateRange constraintAnnotation) {

		validationRules = Arrays.asList(
			(arrivalDate, departureDate) -> arrivalDate != null && departureDate != null,
			(arrivalDate, departureDate) -> arrivalDate.isBefore(departureDate) || arrivalDate.isEqual(departureDate),
			(arrivalDate, departureDate) -> arrivalDate.isAfter(LocalDate.now()) && departureDate.isBefore(LocalDate.now().plusDays(Calendar.TIME_SPAN + 2)),
			(arrivalDate, departureDate) -> ChronoUnit.DAYS.between(arrivalDate, departureDate) < 3
		);
		arrivalDateName = constraintAnnotation.arrivalDate();
		departureDateName = constraintAnnotation.departureDate();
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {

		LocalDate arrivalDate = (LocalDate) new BeanWrapperImpl(value)
				.getPropertyValue(arrivalDateName);
		LocalDate departureDate = (LocalDate) new BeanWrapperImpl(value)
				.getPropertyValue(departureDateName);

		return validationRules.stream()
				.allMatch(rule -> rule.test(arrivalDate, departureDate));
	}
}
