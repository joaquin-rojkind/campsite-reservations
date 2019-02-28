package com.upgrade.campsite.rest.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.upgrade.campsite.rest.validation.AvailabilityDateRange;

@AvailabilityDateRange(startDate = "startDate", endDate = "endDate")
public class DateRangeDto {

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;

	public boolean isNullDates() {
		return startDate == null && endDate == null;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public DateRangeDto startDate(LocalDate startDate) {
		this.startDate = startDate;
		return this;
	}

	public DateRangeDto endDate(LocalDate endDate) {
		this.endDate = endDate;
		return this;
	}
}
