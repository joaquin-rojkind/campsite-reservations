package com.upgrade.campsite.calendar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

/*
 * This class emulates a calendar structure meant to cache booking availability within a predefined time window.
 * It provides basic operations only which the consumer can combine at its convenience, hence it's the
 * consumer's responsibility to ensure thread-safety access to the calendar resource and prevent concurrency issues.
 */
@Component
public class Calendar {

	public static final int TIME_SPAN = 30;

	private final List<Boolean> calendar = new ArrayList<>(); // true = occupied, false = available

	@PostConstruct
	private void initialize() {
		// We handle a 30 day window for both availability and booking.
		for (int i = 0; i <= TIME_SPAN; i++) {
			calendar.add(false);
		}
	}

	/**
	 * Read availability for given date range
	 * @param startDate
	 * @param endDate
	 * @return A list of available dates
	 */
	public List<LocalDate> readAvailability(LocalDate startDate, LocalDate endDate) {
		List<LocalDate> availability = new ArrayList<>();

		int initialDay = getDayNumber(startDate);
		int finalDay = getDayNumber(endDate);

		for (int i = initialDay; i <= finalDay; i++) {
			if (!calendar.get(i).booleanValue()) {
				availability.add(LocalDate.now().plusDays(i));
			}
		}
		return availability;
	}

	/**
	 * Book the specified date range
	 * @param arrivalDate
	 * @param departureDate
	 */
	public void book(LocalDate arrivalDate, LocalDate departureDate) {
		int initialDay = getDayNumber(arrivalDate);
		int finalDay = getDayNumber(departureDate);

		for (int i = initialDay; i <= finalDay; i++) {
			calendar.set(i, true);
		}
	}

	/**
	 * Unbook the specified date range
	 * @param startDate
	 * @param endDate
	 */
	public void unbook(LocalDate startDate, LocalDate endDate) {
		int initialDay = getDayNumber(startDate);
		int finalDay = getDayNumber(endDate);

		for (int i = initialDay; i <= finalDay; i++) {
			calendar.set(i, false);
		}
	}

	/**
	 * Check availability for given date range
	 * @param startDate
	 * @param endDate
	 * @return true if all dates are available, otherwise false
	 */
	public boolean checkAvailability(LocalDate startDate, LocalDate endDate) {
		int initialDay = getDayNumber(startDate);
		int finalDay = getDayNumber(endDate);

		for (int i = initialDay; i <= finalDay; i++) {
			if (calendar.get(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check availability for a given date range excluding a second date range.
	 * Useful when checking availability to modify an existing reservation
	 * @param originalStartDate
	 * @param originalEndDate
	 * @param newStartDate
	 * @param newEndDate
	 * @return true if dates are available and/or within original date range, otherwise false
	 */
	public boolean checkOverlappingAvailability(LocalDate originalStartDate,
												   LocalDate originalEndDate,
												   LocalDate newStartDate,
												   LocalDate newEndDate) {
		int originalInitialDay = getDayNumber(originalStartDate);
		int originalFinalDay = getDayNumber(originalEndDate);
		int newInitialDay = getDayNumber(newStartDate);
		int newFinalDay = getDayNumber(newEndDate);

		List<Integer> originalDays = new ArrayList<>();
		for (int i = originalInitialDay; i <= originalFinalDay; i++) {
			originalDays.add(i);
		}
		List<Integer> daysToCheck = new ArrayList<>();
		for (int i = newInitialDay; i <= newFinalDay; i++) {
			if (!originalDays.contains(i)) {
				daysToCheck.add(i);
			}
		}
		if (daysToCheck.size() != 0) { // If new dates are not fully contained inside previous dates
			int initialDay = daysToCheck.get(0);
			int finalDay = daysToCheck.get(daysToCheck.size() - 1); // don't need size but length of the list

			for (int i = initialDay; i <= finalDay; i++) {
				if (calendar.get(i)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Move calendar forward by one day. The service is responsible for managing this operation appropriately.
	 */
	public void advanceCalendar() {
		calendar.remove(0);
		calendar.add(false);
	}

	private int getDayNumber(LocalDate date) {
		return (int) ChronoUnit.DAYS.between(LocalDate.now(), date);
	}
}
