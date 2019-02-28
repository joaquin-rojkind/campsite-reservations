package com.upgrade.campsite.rest.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class AvailabilityDto {

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private List<LocalDate> availableDates;

	public AvailabilityDto() {
	}

	public AvailabilityDto(LocalDate startDate, LocalDate endDate, List<LocalDate> availableDates) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.availableDates = availableDates;
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

	public List<LocalDate> getAvailableDates() {
		return availableDates;
	}

	public void setAvailableDates(List<LocalDate> availableDates) {
		this.availableDates = availableDates;
	}
}
