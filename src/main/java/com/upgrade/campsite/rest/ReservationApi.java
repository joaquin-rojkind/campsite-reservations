package com.upgrade.campsite.rest;


import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.upgrade.campsite.rest.dto.AvailabilityDto;
import com.upgrade.campsite.rest.dto.DateRangeDto;
import com.upgrade.campsite.rest.dto.ReservationDto;
import com.upgrade.campsite.service.ReservationService;

@RestController
@RequestMapping("/api")
public class ReservationApi {

	@Autowired
	private ReservationService reservationService;

	@GetMapping("/reservations")
	public ResponseEntity<AvailabilityDto> readAvailability(@Valid DateRangeDto dateRangeDto) {
 		AvailabilityDto availabilityDto = reservationService.readAvailability(dateRangeDto);
		return new ResponseEntity<AvailabilityDto>(availabilityDto, HttpStatus.OK);
	}

	@PostMapping("/reservations")
	public ResponseEntity<ReservationDto> makeReservation(@RequestBody @Valid ReservationDto reservationDto) {
		ReservationDto createdReservationDto = reservationService.makeReservation(reservationDto);
		return new ResponseEntity<ReservationDto>(createdReservationDto, HttpStatus.OK);
	}

	@PutMapping("/reservations/{id}")
	public ResponseEntity<ReservationDto> modifyReservation(@PathVariable String id, @RequestBody @Valid ReservationDto reservationDto) {
		ReservationDto updatedReservationDto = reservationService.modifyReservation(id, reservationDto);
		return new ResponseEntity<ReservationDto>(updatedReservationDto, HttpStatus.OK);
	}

	@DeleteMapping("/reservations/{id}")
	public ResponseEntity<HttpStatus> cancelReservation(@PathVariable String id) {
		reservationService.cancelReservation(id);
		return new ResponseEntity<HttpStatus>(HttpStatus.OK);
	}

	@GetMapping("/reservations/{id}")
	public ResponseEntity<ReservationDto> readReservation(@PathVariable String id) {
		ReservationDto reservationDto = reservationService.readReservation(id);
		return new ResponseEntity<ReservationDto>(reservationDto, HttpStatus.OK);
	}
}
