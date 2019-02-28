package com.upgrade.campsite.rest.dto;

import java.time.LocalDate;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.upgrade.campsite.rest.validation.BookingDateRange;

@JsonInclude(JsonInclude.Include.NON_NULL)
@BookingDateRange(arrivalDate = "arrivalDate", departureDate = "departureDate")
public class ReservationDto {

	private String uuid;

	@NotBlank(message = "Field 'email' is required")
	@Size(max = 30, message = "Email can not be longer than 30 characters")
	@Email(message = "Please provide a valid email address")
	private String email;

	@NotBlank(message = "Field 'fullName' is required")
	@Size(max = 30, message = "Full name can not be longer than 30 characters")
	private String fullName;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate arrivalDate;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate departureDate;

	public String getUuid() {
		return uuid;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public LocalDate getArrivalDate() {
		return arrivalDate;
	}

	public LocalDate getDepartureDate() {
		return departureDate;
	}

	public ReservationDto uuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	public ReservationDto email(String email) {
		this.email = email;
		return this;
	}

	public ReservationDto fullName(String fullName) {
		this.fullName = fullName;
		return this;
	}

	public ReservationDto arrivalDate(LocalDate arrivalDate) {
		this.arrivalDate = arrivalDate;
		return this;
	}

	public ReservationDto departureDate(LocalDate departureDate) {
		this.departureDate = departureDate;
		return this;
	}
}
