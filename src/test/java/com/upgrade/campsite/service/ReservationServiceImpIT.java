package com.upgrade.campsite.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.upgrade.campsite.calendar.Calendar;
import com.upgrade.campsite.model.Reservation;
import com.upgrade.campsite.repository.ReservationRepository;
import com.upgrade.campsite.rest.dto.ReservationDto;
import com.upgrade.campsite.service.exception.ReservationServiceException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReservationServiceImpIT {

	@Autowired
	private Calendar calendar;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private ReservationService reservationService;

	@Test
	public void makeConcurrentReservations() {

		// Create as many threads as the size of the time span
		int threads = Calendar.TIME_SPAN;
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(1);

		// Submit as many workers as threads in the pool
		for (int t = 0; t < threads; ++t) {
			executorService.submit(() -> {
				try {
					// Make the thread await for release signal
					latch.await();

					// Start off from day one
					int dayNumber = 1;
					boolean success = false;

					// Retry till success
					while (!success) {

						boolean result = true;

						// Try to book one single day
						ReservationDto reservationDto = new ReservationDto()
								.email("someone@something.com")
								.fullName("John Smith")
								.arrivalDate(LocalDate.now().plusDays(dayNumber))
								.departureDate(LocalDate.now().plusDays(dayNumber));
						try {
							reservationService.makeReservation(reservationDto);
						// Date is not available
						} catch (ReservationServiceException ex) {
							result = false;
						}
						success = result;
						// On each iteration try to book the day after
						dayNumber++;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		// Release all threads simultaneously
		latch.countDown();

		handleExecutorServiceShutdown(executorService);

		// Exactly 30 reservations have been created and saved to the database
		assertThat(((List<Reservation>)reservationRepository.findAll()).size()).isEqualTo(30);
		// Availability in the calendar is none
		assertThat(calendar.readAvailability(LocalDate.now().plusDays(1), LocalDate.now().plusDays(Calendar.TIME_SPAN))).isEmpty();
	}

	private void handleExecutorServiceShutdown(ExecutorService executorService) {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}
}
