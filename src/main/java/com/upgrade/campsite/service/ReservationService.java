package com.upgrade.campsite.service;

import com.upgrade.campsite.rest.dto.AvailabilityDto;
import com.upgrade.campsite.rest.dto.DateRangeDto;
import com.upgrade.campsite.rest.dto.ReservationDto;
import com.upgrade.campsite.service.exception.ReservationServiceException;

public interface ReservationService {

	/**
	 * Read availability for the given date range
	 * @param dateRangeDto the date range to check
	 * @return A list of available dates
	 */
	AvailabilityDto readAvailability(DateRangeDto dateRangeDto);

	/**
	 * Make a reservation
	 * @param reservationDto The intended reservation
	 * @return The newly created reservation
	 * @throws ReservationServiceException with error code UNAVAILABLE_DATES if requested dates are not available
	 */
	ReservationDto makeReservation(ReservationDto reservationDto);

	/**
	 * Modify an existing reservation
	 * @param uuid The uuid of the existing reservation
	 * @param reservationDto The modified reservation
	 * @return The updated reservation
	 * @throws ReservationServiceException with error code RESERVATION_NOT_FOUND if the reservation does not exist
	 * @throws ReservationServiceException with error code RESERVATION_EXPIRED if the original reservation has expired (departure date is today or before today)
	 * @throws ReservationServiceException with error code UNAVAILABLE_DATES if requested new dates are not available
	 */
	ReservationDto modifyReservation(String uuid, ReservationDto reservationDto);

	/**
	 * Cancel a reservation
	 * @param uuid The uuid of the reservation
	 * @throws ReservationServiceException with error code RESERVATION_NOT_FOUND if the reservation does not exist
	 * @throws ReservationServiceException with error code RESERVATION_EXPIRED if the reservation has already expired (departure date is today or before today)
	 */
	void cancelReservation(String uuid);

	/**
	 * Read a reservation
	 * @param uuid The uuid of the reservation
	 * @return The given reservation
	 * @throws ReservationServiceException with error code RESERVATION_NOT_FOUND if the reservation does not exist
	 */
	ReservationDto readReservation(String uuid);
}
