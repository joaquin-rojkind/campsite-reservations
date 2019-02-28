package com.upgrade.campsite.rest;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.upgrade.campsite.calendar.Calendar;
import com.upgrade.campsite.model.Reservation;
import com.upgrade.campsite.repository.ReservationRepository;
import com.upgrade.campsite.rest.advice.ErrorCode;
import com.upgrade.campsite.rest.advice.RestResponseEntityExceptionHandler;
import com.upgrade.campsite.rest.dto.ReservationDto;
import com.upgrade.campsite.rest.validation.AvailabilityDateRange;
import com.upgrade.campsite.rest.validation.BookingDateRange;
import com.upgrade.campsite.service.exception.ReservationServiceErrorCode;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationApiIT {

	private static final String EMAIL = "someone@something.com";
	private static final String FULL_NAME = "John Smith";

	@Autowired
	private Calendar calendar;
	@Autowired
	private ReservationRepository reservationRepository;

	@LocalServerPort
	int port;

	@Before
	public void setUp() {
		RestAssured.port = port;
	}

	@After
	public void tearDown() {
		reservationRepository.deleteAll();
		calendar.unbook(LocalDate.now().plusDays(1), LocalDate.now().plusDays(Calendar.TIME_SPAN));
	}


	/************************************************/
	/************** DOMAIN TEST CASES  **************/
	/************************************************/

	@Test
	public void getAvailability_defaultRange_allAvailable() {

		LocalDate defaultStartDate = LocalDate.now().plusDays(1);
		LocalDate defaultEndDate = LocalDate.now().plusDays(Calendar.TIME_SPAN);

		List<String> availableDatesExpected = new ArrayList<>();
		for (int i = 1; i <= Calendar.TIME_SPAN; i++) {
			availableDatesExpected.add(LocalDate.now().plusDays(i).toString());
		}
		when().
				get("/api/reservations").
		then()
				.statusCode(HttpStatus.SC_OK)
				.body("startDate", Matchers.is(defaultStartDate.toString()))
				.body("endDate", Matchers.is(defaultEndDate.toString()))
				.body("availableDates", Matchers.hasSize(Calendar.TIME_SPAN))
				.body("availableDates", Matchers.contains(availableDatesExpected.toArray()));
	}

	@Test
	public void getAvailability_smallRange_allAvailable() {

		LocalDate startDate = LocalDate.now().plusDays(15);
		LocalDate endDate = LocalDate.now().plusDays(25);

		List<String> availableDatesExpected = new ArrayList<>();
		for (int i = 15; i <= 25; i++) {
			availableDatesExpected.add(LocalDate.now().plusDays(i).toString());
		}
		when()
				.get("/api/reservations" + getDateRangeQueryParams(startDate, endDate)).
		then()
				.statusCode(HttpStatus.SC_OK)
				.body("startDate", Matchers.is(startDate.toString()))
				.body("endDate", Matchers.is(endDate.toString()))
				.body("availableDates", Matchers.hasSize(11))
				.body("availableDates", Matchers.contains(availableDatesExpected.toArray()));
	}

	@Test
	public void getAvailability_smallRange_someAvailable() {

		makeReservation(LocalDate.now().plusDays(19), LocalDate.now().plusDays(21));

		LocalDate startDate = LocalDate.now().plusDays(18);
		LocalDate endDate = LocalDate.now().plusDays(22);

		List<String> availableDatesExpected = Arrays.asList(startDate.toString(), endDate.toString());

		when()
				.get("/api/reservations" + getDateRangeQueryParams(startDate, endDate)).
		then()
				.statusCode(HttpStatus.SC_OK)
				.body("startDate", Matchers.is(startDate.toString()))
				.body("endDate", Matchers.is(endDate.toString()))
				.body("availableDates", Matchers.hasSize(2))
				.body("availableDates", Matchers.contains(availableDatesExpected.toArray()));
	}

	@Test
	public void getAvailability_smallRange_noneAvailable() {

		LocalDate startDate = LocalDate.now().plusDays(19);
		LocalDate endDate = LocalDate.now().plusDays(21);

		makeReservation(startDate, endDate);

		when()
				.get("/api/reservations" + getDateRangeQueryParams(startDate, endDate)).
		then()
				.statusCode(HttpStatus.SC_OK)
				.body("startDate", Matchers.is(startDate.toString()))
				.body("endDate", Matchers.is(endDate.toString()))
				.body("availableDates", Matchers.hasSize(0));
	}

	@Test
	public void makeReservation_success() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_OK).
				body("uuid", Matchers.notNullValue()).
				body("email", Matchers.equalTo(EMAIL)).
				body("fullName", Matchers.equalTo(FULL_NAME)).
				body("arrivalDate", Matchers.equalTo(arrivalDate.toString())).
				body("departureDate", Matchers.equalTo(departureDate.toString()));
	}

	@Test
	public void makeReservation_notAvailable() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		makeReservation(arrivalDate, departureDate);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_FORBIDDEN).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.UNAVAILABLE_DATES.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void readReservation_success() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		String uuid = makeReservation(arrivalDate, departureDate);

		when()
				.get("/api/reservations/" + uuid).
		then().
				statusCode(HttpStatus.SC_OK).
				body("uuid", Matchers.equalTo(uuid)).
				body("email", Matchers.equalTo(EMAIL)).
				body("fullName", Matchers.equalTo(FULL_NAME)).
				body("arrivalDate", Matchers.equalTo(arrivalDate.toString())).
				body("departureDate", Matchers.equalTo(departureDate.toString()));
	}

	@Test
	public void readReservation_notFound() {

		when()
				.get("/api/reservations/xxx-xxx-xxx-xxx").
		then().
				statusCode(HttpStatus.SC_NOT_FOUND).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.RESERVATION_NOT_FOUND.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void modifyReservation_success() {

		String uuid = makeReservation(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));

		String newEmail = "new@new.com";
		String newFullname = "Albert Smith";
		LocalDate newArrivalDate = LocalDate.now().plusDays(7);
		LocalDate newDepartureDate = LocalDate.now().plusDays(8);

		ReservationDto reservationDto = new ReservationDto()
				.email(newEmail)
				.fullName(newFullname)
				.arrivalDate(newArrivalDate)
				.departureDate(newDepartureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				put("/api/reservations/" + uuid).
		then().
				statusCode(HttpStatus.SC_OK).
				body("uuid", Matchers.equalTo(uuid)).
				body("email", Matchers.equalTo(newEmail)).
				body("fullName", Matchers.equalTo(newFullname)).
				body("arrivalDate", Matchers.equalTo(newArrivalDate.toString())).
				body("departureDate", Matchers.equalTo(newDepartureDate.toString()));
	}

	@Test
	public void modifyReservation_notAvailable() {

		makeReservation(LocalDate.now().plusDays(8), LocalDate.now().plusDays(8));
		String uuid = makeReservation(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));

		LocalDate newArrivalDate = LocalDate.now().plusDays(7);
		LocalDate newDepartureDate = LocalDate.now().plusDays(8);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(newArrivalDate)
				.departureDate(newDepartureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				put("/api/reservations/" + uuid).
		then().
				statusCode(HttpStatus.SC_FORBIDDEN).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.UNAVAILABLE_DATES.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void modifyReservation_expired() {

		String uuid = UUID.randomUUID().toString();

		Reservation reservation = new Reservation()
				.uuid(uuid)
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(LocalDate.now().minusDays(1))
				.departureDate(LocalDate.now());

		reservationRepository.save(reservation);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(LocalDate.now().plusDays(1))
				.departureDate(LocalDate.now().plusDays(3));

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				put("/api/reservations/" + uuid).
		then().
				statusCode(HttpStatus.SC_FORBIDDEN).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.RESERVATION_EXPIRED.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void modifyReservation_notFound() {

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(LocalDate.now().plusDays(1))
				.departureDate(LocalDate.now().plusDays(3));

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				put("/api/reservations/xxx-xxx-xxx-xxx").
		then().
				statusCode(HttpStatus.SC_NOT_FOUND).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.RESERVATION_NOT_FOUND.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void cancelReservation_success() {
		String uuid = makeReservation(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
		when().
				delete("/api/reservations/" + uuid).
		then().
				statusCode(HttpStatus.SC_OK);
	}

	@Test
	public void cancelReservation_expired() {

		String uuid = UUID.randomUUID().toString();

		Reservation reservation = new Reservation()
				.uuid(uuid)
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(LocalDate.now().minusDays(1))
				.departureDate(LocalDate.now());

		reservationRepository.save(reservation);

		when().
				delete("/api/reservations/" + uuid).
		then().
				statusCode(HttpStatus.SC_FORBIDDEN).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.RESERVATION_EXPIRED.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void cancelReservation_notFound() {
		when().
				delete("/api/reservations/xxx-xxx-xxx-xxx").
		then().
				statusCode(HttpStatus.SC_NOT_FOUND).
				body("errorCode", Matchers.equalTo(ReservationServiceErrorCode.RESERVATION_NOT_FOUND.name())).
				body("message", Matchers.notNullValue());
	}


	/************************************************/
	/************ VALIDATION TEST CASES  ************/
	/************************************************/

	@Test
	public void getAvailability_outOfRange_lowerBound() {

		LocalDate startDate = LocalDate.now();
		LocalDate endDate = LocalDate.now().plusDays(25);

		when()
				.get("/api/reservations" + getDateRangeQueryParams(startDate, endDate)).
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(AvailabilityDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void getAvailability_outOfRange_upperBound() {

		LocalDate startDate = LocalDate.now().plusDays(20);
		LocalDate endDate = LocalDate.now().plusDays(Calendar.TIME_SPAN + 1);

		when()
				.get("/api/reservations" + getDateRangeQueryParams(startDate, endDate)).
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(AvailabilityDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void getAvailability_invertedDates() {

		LocalDate startDate = LocalDate.now().plusDays(10);
		LocalDate endDate = LocalDate.now().plusDays(5);

		when()
				.get("/api/reservations" + getDateRangeQueryParams(startDate, endDate)).
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(AvailabilityDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void getAvailability_startDateMissing() {

		LocalDate endDate = LocalDate.now().plusDays(5);

		when()
				.get("/api/reservations?endDate=" + endDate.toString()).
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(AvailabilityDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void getAvailability_malformedDate() {
		when()
				.get("/api/reservations?startDate=2019-02-26&endDate=2019/02/28").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(RestResponseEntityExceptionHandler.MALFORMED_DATE_ERROR_MESSAGE));
	}

	@Test
	public void makeReservation_emailMissing() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void makeReservation_invalidEmail() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.email("someone.com")
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void makeReservation_fullNameMissing() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void makeReservation_invalidFullName_length() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName("John Benajmin Tarsitant Smith Claybourne")
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.notNullValue());
	}

	@Test
	public void makeReservation_invalidDateRange_greaterThanThreeDays() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(5);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
				when().
				post("/api/reservations/").
				then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(BookingDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void makeReservation_dateOutOfRange_lowerBound() {

		LocalDate arrivalDate = LocalDate.now();
		LocalDate departureDate = LocalDate.now().plusDays(2);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(BookingDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void makeReservation_dateOutOfRange_upperBound() {

		LocalDate arrivalDate = LocalDate.now().plusDays(1);
		LocalDate departureDate = LocalDate.now().plusDays(Calendar.TIME_SPAN + 1);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(BookingDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void makeReservation_invertedDates() {

		LocalDate arrivalDate = LocalDate.now().plusDays(7);
		LocalDate departureDate = LocalDate.now().plusDays(5);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(BookingDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void makeReservation_arrivalDateMissing() {

		LocalDate departureDate = LocalDate.now().plusDays(7);

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.departureDate(departureDate);

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(BookingDateRange.INVALID_DATE_RANGE_MESSAGE));
	}

	@Test
	public void makeReservation_malformedDate() {

		given().
				contentType(ContentType.JSON).
				with().
				body("{\"email\": \"someone@something.com\",\"fullName\": \"John Smith\",\"" +
						"arrivalDate\": \"2019-02-21\"," +
						"\"departureDate\": \"2019/02/21\"}").
		when().
				post("/api/reservations/").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.INVALID_PARAMETERS.name())).
				body("message", Matchers.equalTo(RestResponseEntityExceptionHandler.MALFORMED_DATE_ERROR_MESSAGE));
	}

	@Test
	public void modifyReservation_idMissing() {

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(LocalDate.now().plusDays(1))
				.departureDate(LocalDate.now().plusDays(3));

		given().
				contentType(ContentType.JSON).
				with().
				body(reservationDto).
		when().
				put("/api/reservations").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.MISSING_PARAMETERS.name())).
				body("message", Matchers.equalTo(RestResponseEntityExceptionHandler.ID_MISSING_ERROR_MESSAGE));
	}

	@Test
	public void cancelReservation_idMissing() {
		when().
				delete("/api/reservations").
		then().
				statusCode(HttpStatus.SC_BAD_REQUEST).
				body("errorCode", Matchers.equalTo(ErrorCode.MISSING_PARAMETERS.name())).
				body("message", Matchers.equalTo(RestResponseEntityExceptionHandler.ID_MISSING_ERROR_MESSAGE));
	}

	private String makeReservation(LocalDate arrivalDate, LocalDate departureDate) {

		ReservationDto reservationDto = new ReservationDto()
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		return given().
					contentType(ContentType.JSON).
					with().
					body(reservationDto).
				when().
					post("/api/reservations").
					jsonPath().
					getString("uuid");
	}

	private String getDateRangeQueryParams(LocalDate startDate, LocalDate endDate) {
		return new StringBuilder()
				.append("?startDate=")
				.append(startDate.toString())
				.append("&endDate=")
				.append(endDate.toString())
				.toString();
	}
}
