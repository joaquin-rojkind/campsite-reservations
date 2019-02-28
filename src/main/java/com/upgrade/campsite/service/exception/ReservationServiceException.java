package com.upgrade.campsite.service.exception;

public class ReservationServiceException extends RuntimeException {

	private final ReservationServiceErrorCode errorCode;

	public ReservationServiceException(ReservationServiceErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ReservationServiceErrorCode getErrorCode() {
		return errorCode;
	}
}
