package com.upgrade.campsite.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.upgrade.campsite.calendar.Calendar;
import com.upgrade.campsite.model.Reservation;
import com.upgrade.campsite.repository.ReservationRepository;
import com.upgrade.campsite.rest.dto.AvailabilityDto;
import com.upgrade.campsite.rest.dto.DateRangeDto;
import com.upgrade.campsite.rest.dto.ReservationDto;
import com.upgrade.campsite.service.exception.ReservationServiceErrorCode;
import com.upgrade.campsite.service.exception.ReservationServiceException;

/*
 * There are two main features in this class:
 *
 *   - Thanks to the calendar cache implementation, the readAvailability operation does not require database access,
 *     hence it is optimized to handle large volumes of requests. The same is true for availability checking within the
 *     makeReservation and modifyReservation operations.
 *
 *   - The StampedLock ensures thread-safety access to the calendar resource for all operations, plus the readAvailability
 *     operation is optimized due to the optimistic read lock implementation.
 */
@Service
public class ReservationServiceImpl implements ReservationService {

	@Autowired
	private Calendar calendar;
	@Autowired
	private ReservationRepository reservationRepository;

	private final StampedLock lock = new StampedLock();

	@PostConstruct
	private void initialize() {
		syncUpCalendar();
	}

	@Override
	public AvailabilityDto readAvailability(DateRangeDto dateRangeDto) {

		LocalDate startDate = dateRangeDto.getStartDate();
		LocalDate endDate = dateRangeDto.getEndDate();

		if (dateRangeDto.isNullDates()) {
			startDate = LocalDate.now().plusDays(1);
			endDate = LocalDate.now().plusDays(Calendar.TIME_SPAN);
		}

		long stamp = lock.tryOptimisticRead();
		List<LocalDate> availability = calendar.readAvailability(startDate, endDate);

		if (lock.validate(stamp)) {
			return new AvailabilityDto(startDate, endDate, availability);
		} else {
			stamp = lock.readLock();
			try {
				availability = calendar.readAvailability(startDate, endDate);
				return new AvailabilityDto(startDate, endDate, availability);
			} finally {
				lock.unlock(stamp);
			}
		}
	}

	@Override
	@Transactional
	public ReservationDto makeReservation(ReservationDto reservationDto) {
		long stamp = lock.writeLock();
		try {
			if (!calendar.checkAvailability(reservationDto.getArrivalDate(), reservationDto.getDepartureDate())) {
				throw new ReservationServiceException(
						ReservationServiceErrorCode.UNAVAILABLE_DATES,
						String.format("The specified time range from arrival date %s through departure date %s is not available. Please specify a different time range.",
								reservationDto.getArrivalDate().toString(),
								reservationDto.getDepartureDate().toString()));
			}
			Reservation reservation = toEntity(reservationDto);
			reservation.uuid(UUID.randomUUID().toString());
			Reservation savedReservation = reservationRepository.save(reservation);

			calendar.book(savedReservation.getArrivalDate(), savedReservation.getDepartureDate());
			return toDto(savedReservation);

		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	@Transactional
	public ReservationDto modifyReservation(String uuid, ReservationDto reservationDto) {

		Reservation reservation = retrieveReservation(uuid);
		LocalDate originalArrivalDate = reservation.getArrivalDate();
		LocalDate originalDepartureDate = reservation.getDepartureDate();
		LocalDate newArrivalDate = reservationDto.getArrivalDate();
		LocalDate newDepartureDate = reservationDto.getDepartureDate();

		if (originalDepartureDate.isBefore(LocalDate.now().plusDays(1))) {
			throw new ReservationServiceException(
					ReservationServiceErrorCode.RESERVATION_EXPIRED,
					String.format("Reservation with id %s has already expired. Please submit a new reservation", uuid));
		}

		long stamp = lock.writeLock();
		try {
			if (!calendar.checkOverlappingAvailability(originalArrivalDate, originalDepartureDate, newArrivalDate, newDepartureDate)) {
				throw new ReservationServiceException(
						ReservationServiceErrorCode.UNAVAILABLE_DATES,
						String.format("The specified time range from arrival date %s through departure date %s is not available. Please specify a different time range.",
								newArrivalDate.toString(),
								newDepartureDate.toString()));
			}
			reservation
					.email(reservationDto.getEmail())
					.fullName(reservationDto.getFullName())
					.arrivalDate(newArrivalDate)
					.departureDate(newDepartureDate);
			Reservation updatedReservation = reservationRepository.save(reservation);

			calendar.unbook(originalArrivalDate, originalDepartureDate);
			calendar.book(newArrivalDate, newDepartureDate);
			return toDto(updatedReservation);

		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	@Transactional
	public void cancelReservation(String uuid) {
		Reservation reservation = retrieveReservation(uuid);
		if (reservation.getDepartureDate().isBefore(LocalDate.now().plusDays(1))) {
			throw new ReservationServiceException(
					ReservationServiceErrorCode.RESERVATION_EXPIRED,
					String.format("Reservation with id %s has already expired.", uuid));
		}
		long stamp = lock.writeLock();
		try {
			// Ideally this would not be a delete but a state change in the entity, scoped out for simplicity
			reservationRepository.delete(reservation);
			calendar.unbook(reservation.getArrivalDate(), reservation.getDepartureDate());

		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public ReservationDto readReservation(String uuid) {
		return toDto(retrieveReservation(uuid));
	}

	/* A cron job would call this method a few seconds before midnight.
	 * All existing threads would be allowed to finish but all operations would be blocked for new incoming threads.
	 * At 12:00 am the calendar would be moved forward by one day and operations would be re-enabled
	 */
	public void advanceCalendar() {
		// TODO: implement
		//calendar.advanceCalendar();
	}

	/* This method is invoked at application startup, it retrieves existing reservations from the repository
	 * and populates the calendar accordingly.
	 */
	private void syncUpCalendar() {
		// TODO: implement
	}

	private Reservation retrieveReservation(String uuid) {
		return Optional.ofNullable(reservationRepository.findByUuid(uuid))
				.orElseThrow(() -> new ReservationServiceException(
						ReservationServiceErrorCode.RESERVATION_NOT_FOUND,
						String.format("Reservation with id %s not found.", uuid)));
	}

	private Reservation toEntity(ReservationDto reservationDto) {
		return new Reservation()
				.uuid(reservationDto.getUuid())
				.email(reservationDto.getEmail())
				.fullName(reservationDto.getFullName())
				.arrivalDate(reservationDto.getArrivalDate())
				.departureDate(reservationDto.getDepartureDate());
	}

	private ReservationDto toDto(Reservation reservation) {
		return new ReservationDto()
				.uuid(reservation.getUuid())
				.email(reservation.getEmail())
				.fullName(reservation.getFullName())
				.arrivalDate(reservation.getArrivalDate())
				.departureDate(reservation.getDepartureDate());
	}
}
