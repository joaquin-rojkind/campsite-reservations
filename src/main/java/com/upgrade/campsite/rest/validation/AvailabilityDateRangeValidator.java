package com.upgrade.campsite.rest.validation;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.BeanWrapperImpl;

import com.upgrade.campsite.calendar.Calendar;


public class AvailabilityDateRangeValidator implements ConstraintValidator<AvailabilityDateRange, Object> {

	private List<BiPredicate<LocalDate, LocalDate>> validationRules;
	private String startDateName;
	private String endDateName;

	@Override
	public void initialize(final AvailabilityDateRange constraintAnnotation) {

		validationRules = Arrays.asList(
			(startDate, endDate) -> startDate != null && endDate != null,
			(startDate, endDate) -> startDate.isBefore(endDate) || startDate.isEqual(endDate),
			(startDate, endDate) -> startDate.isAfter(LocalDate.now()) && endDate.isBefore(LocalDate.now().plusDays(Calendar.TIME_SPAN + 1))
		);
		startDateName = constraintAnnotation.startDate();
		endDateName = constraintAnnotation.endDate();
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {

		LocalDate startDate = (LocalDate) new BeanWrapperImpl(value)
				.getPropertyValue(startDateName);
		LocalDate endDate = (LocalDate) new BeanWrapperImpl(value)
				.getPropertyValue(endDateName);

		if (startDate == null && endDate == null) {
			return true;
		}
		return validationRules.stream()
				.allMatch(rule -> rule.test(startDate, endDate));
	}
}
