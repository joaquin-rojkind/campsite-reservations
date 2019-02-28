package com.upgrade.campsite.repository;

import org.springframework.data.repository.CrudRepository;

import com.upgrade.campsite.model.Reservation;

public interface ReservationRepository extends CrudRepository<Reservation, Long> {

	Reservation findByUuid(String uuid);
}
