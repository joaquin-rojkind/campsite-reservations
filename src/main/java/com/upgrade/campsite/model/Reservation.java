package com.upgrade.campsite.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "uuid", unique = true, length = 64, nullable = false)
	private String uuid;

	@Column(name = "email", length = 64, nullable = false)
	private String email;

	@Column(name = "full_name", length = 64, nullable = false)
	private String fullName;

	@Column(name = "arrival_date", nullable = false)
	private LocalDate arrivalDate;

	@Column(name = "departure_date", nullable = false)
	private LocalDate departureDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public LocalDate getArrivalDate() {
		return arrivalDate;
	}

	public void setArrivalDate(LocalDate arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

	public LocalDate getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(LocalDate departureDate) {
		this.departureDate = departureDate;
	}

	public Reservation uuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	public Reservation email(String email) {
		this.email = email;
		return this;
	}

	public Reservation fullName(String fullName) {
		this.fullName = fullName;
		return this;
	}

	public Reservation arrivalDate(LocalDate arrivalDate) {
		this.arrivalDate = arrivalDate;
		return this;
	}

	public Reservation departureDate(LocalDate departureDate) {
		this.departureDate = departureDate;
		return this;
	}
}
