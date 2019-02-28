package com.upgrade.campsite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.upgrade.campsite.calendar.Calendar;
import com.upgrade.campsite.model.Reservation;
import com.upgrade.campsite.repository.ReservationRepository;
import com.upgrade.campsite.rest.dto.AvailabilityDto;
import com.upgrade.campsite.rest.dto.DateRangeDto;
import com.upgrade.campsite.rest.dto.ReservationDto;
import com.upgrade.campsite.service.exception.ReservationServiceErrorCode;
import com.upgrade.campsite.service.exception.ReservationServiceException;

public class ReservationServiceImplTest {

	private static final String UUID = "4e40df32-7b64-4932-9219-a7bb4cf640cb";
	private static final String EMAIL = "someone@something.com";
	private static final String FULL_NAME = "John Smith";

	@Mock
	private Calendar calendar;
	@Mock
	private ReservationRepository reservationRepository;
	@InjectMocks
	private ReservationServiceImpl reservationService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void readAvailability_defaultRange_success_allAvailable() {

		LocalDate defaultStartDate = LocalDate.now().plusDays(1);
		LocalDate defaultEndDate = LocalDate.now().plusDays(Calendar.TIME_SPAN);

		DateRangeDto dateRangeDto = new DateRangeDto()
				.startDate(null)
				.endDate(null);

		List<LocalDate> availableDatesExpected = new ArrayList<>();
		for (int i = 1; i <= Calendar.TIME_SPAN; i++) {
			availableDatesExpected.add(LocalDate.now().plusDays(i));
		}

		when(calendar.readAvailability(defaultStartDate, defaultEndDate)).thenReturn(availableDatesExpected);

		AvailabilityDto availability = reservationService.readAvailability(dateRangeDto);

		assertThat(availability.getStartDate()).isEqualTo(defaultStartDate);
		assertThat(availability.getEndDate()).isEqualTo(defaultEndDate);
		assertThat(availability.getAvailableDates().size()).isEqualTo(30);
		assertThat(availability.getAvailableDates()).containsAll(availableDatesExpected);
	}

	@Test
	public void readAvailability_success_noneAvailable() {

		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(Calendar.TIME_SPAN);

		DateRangeDto dateRangeDto = new DateRangeDto()
				.startDate(startDate)
				.endDate(endDate);

		List<LocalDate> availableDatesExpected = new ArrayList<>();

		when(calendar.readAvailability(startDate, endDate)).thenReturn(availableDatesExpected);

		AvailabilityDto availability = reservationService.readAvailability(dateRangeDto);

		assertThat(availability.getStartDate()).isEqualTo(startDate);
		assertThat(availability.getEndDate()).isEqualTo(endDate);
		assertThat(availability.getAvailableDates()).isEmpty();
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

		Reservation reservation = new Reservation()
				.uuid(UUID)
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		when(calendar.checkAvailability(arrivalDate, departureDate)).thenReturn(true);
		when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

		ReservationDto confirmedReservation = reservationService.makeReservation(reservationDto);

		assertThat(confirmedReservation.getUuid()).isEqualTo(UUID);
		assertThat(confirmedReservation.getEmail()).isEqualTo(EMAIL);
		assertThat(confirmedReservation.getFullName()).isEqualTo(FULL_NAME);
		assertThat(confirmedReservation.getArrivalDate()).isEqualTo(arrivalDate);
		assertThat(confirmedReservation.getDepartureDate()).isEqualTo(departureDate);
	}

	@Test
	public void makeReservation_unavailableDates() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		when(calendar.checkAvailability(arrivalDate, departureDate)).thenReturn(false);
		assertThatThrownBy(() -> reservationService.makeReservation(reservationDto))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.UNAVAILABLE_DATES);
	}

	@Test
	public void modifyReservation_success() {

		LocalDate originalArrivalDate = LocalDate.now().plusDays(2);
		LocalDate originaldepartureDate = LocalDate.now().plusDays(2);

		String newEmail = "new@email.com";
		String newFullname = "New Name";
		LocalDate newArrivalDate = LocalDate.now().plusDays(2);
		LocalDate newDepartureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.email(newEmail)
				.fullName(newFullname)
				.arrivalDate(newArrivalDate)
				.departureDate(newDepartureDate);

		Reservation originalReservation = new Reservation()
				.uuid(UUID)
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(originalArrivalDate)
				.departureDate(originaldepartureDate);

		Reservation updatedReservation = new Reservation()
				.uuid(UUID)
				.email(newEmail)
				.fullName(newFullname)
				.arrivalDate(newArrivalDate)
				.departureDate(newDepartureDate);

		when(reservationRepository.findByUuid(UUID)).thenReturn(originalReservation);
		when(calendar.checkOverlappingAvailability(originalArrivalDate, originaldepartureDate, newArrivalDate, newDepartureDate)).thenReturn(true);
		when(reservationRepository.save(any(Reservation.class))).thenReturn(updatedReservation);

		ReservationDto confirmedReservation = reservationService.modifyReservation(UUID, reservationDto);

		assertThat(confirmedReservation.getUuid()).isEqualTo(UUID);
		assertThat(confirmedReservation.getEmail()).isEqualTo(newEmail);
		assertThat(confirmedReservation.getFullName()).isEqualTo(newFullname);
		assertThat(confirmedReservation.getArrivalDate()).isEqualTo(newArrivalDate);
		assertThat(confirmedReservation.getDepartureDate()).isEqualTo(newDepartureDate);
	}

	@Test
	public void modifyReservation_unavailableDates() {

		LocalDate originalArrivalDate = LocalDate.now().plusDays(2);
		LocalDate originaldepartureDate = LocalDate.now().plusDays(2);
		LocalDate newArrivalDate = LocalDate.now().plusDays(2);
		LocalDate newDepartureDate = LocalDate.now().plusDays(4);

		ReservationDto reservationDto = new ReservationDto()
				.arrivalDate(newArrivalDate)
				.departureDate(newDepartureDate);

		Reservation originalReservation = new Reservation()
				.arrivalDate(originalArrivalDate)
				.departureDate(originaldepartureDate);

		when(reservationRepository.findByUuid(UUID)).thenReturn(originalReservation);
		when(calendar.checkOverlappingAvailability(originalArrivalDate, originaldepartureDate, newArrivalDate, newDepartureDate)).thenReturn(false);

		assertThatThrownBy(() -> reservationService.modifyReservation(UUID, reservationDto))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.UNAVAILABLE_DATES);
	}

	@Test
	public void modifyReservation_notFound() {

		when(reservationRepository.findByUuid(UUID)).thenReturn(null);

		assertThatThrownBy(() -> reservationService.modifyReservation(UUID, new ReservationDto()))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	public void modifyReservation_expired() {

		Reservation reservation = new Reservation()
				.arrivalDate(LocalDate.now())
				.departureDate(LocalDate.now());

		when(reservationRepository.findByUuid(UUID)).thenReturn(reservation);

		assertThatThrownBy(() -> reservationService.modifyReservation(UUID, new ReservationDto()))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.RESERVATION_EXPIRED);
	}

	@Test
	public void cancelReservation_success() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		Reservation reservation = new Reservation()
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		when(reservationRepository.findByUuid(UUID)).thenReturn(reservation);
		reservationService.cancelReservation(UUID);

		verify(reservationRepository, times(1)).delete(reservation);
		verify(calendar, times(1)).unbook(arrivalDate, departureDate);
	}

	@Test
	public void cancelReservation_notFound() {

		when(reservationRepository.findByUuid(UUID)).thenReturn(null);

		assertThatThrownBy(() -> reservationService.cancelReservation(UUID))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	public void cancelReservation_expired() {

		Reservation reservation = new Reservation()
				.arrivalDate(LocalDate.now())
				.departureDate(LocalDate.now());

		when(reservationRepository.findByUuid(UUID)).thenReturn(reservation);

		assertThatThrownBy(() -> reservationService.cancelReservation(UUID))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.RESERVATION_EXPIRED);
	}

	@Test
	public void readReservation_success() {

		LocalDate arrivalDate = LocalDate.now().plusDays(2);
		LocalDate departureDate = LocalDate.now().plusDays(4);

		Reservation reservation = new Reservation()
				.uuid(UUID)
				.email(EMAIL)
				.fullName(FULL_NAME)
				.arrivalDate(arrivalDate)
				.departureDate(departureDate);

		when(reservationRepository.findByUuid(UUID)).thenReturn(reservation);

		ReservationDto returnedReservation = reservationService.readReservation(UUID);

		assertThat(returnedReservation.getUuid()).isEqualTo(UUID);
		assertThat(returnedReservation.getEmail()).isEqualTo(EMAIL);
		assertThat(returnedReservation.getFullName()).isEqualTo(FULL_NAME);
		assertThat(returnedReservation.getArrivalDate()).isEqualTo(arrivalDate);
		assertThat(returnedReservation.getDepartureDate()).isEqualTo(departureDate);
	}

	@Test
	public void readReservation_notFound() {

		when(reservationRepository.findByUuid(UUID)).thenReturn(null);

		assertThatThrownBy(() -> reservationService.readReservation(UUID))
				.isInstanceOf(ReservationServiceException.class)
				.hasFieldOrPropertyWithValue("errorCode", ReservationServiceErrorCode.RESERVATION_NOT_FOUND);
	}
}
