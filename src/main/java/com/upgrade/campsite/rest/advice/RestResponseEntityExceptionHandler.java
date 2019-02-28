package com.upgrade.campsite.rest.advice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.upgrade.campsite.service.exception.ReservationServiceErrorCode;
import com.upgrade.campsite.service.exception.ReservationServiceException;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	public static final String MALFORMED_DATE_ERROR_MESSAGE = "Dates must be properly formatted in the pattern of yyyy-MM-dd, e.g: 2019-03-30";
	public static final String ID_MISSING_ERROR_MESSAGE = "Id is missing in URL";

	private Map<ReservationServiceErrorCode, HttpStatus> reservationServiceErrorMapping;

	@PostConstruct
	private void initialize() {
		reservationServiceErrorMapping = ImmutableMap.<ReservationServiceErrorCode, HttpStatus>builder()
				.put(ReservationServiceErrorCode.UNAVAILABLE_DATES, HttpStatus.FORBIDDEN)
				.put(ReservationServiceErrorCode.RESERVATION_EXPIRED, HttpStatus.FORBIDDEN)
				.put(ReservationServiceErrorCode.RESERVATION_NOT_FOUND, HttpStatus.NOT_FOUND)
				.build();
	}

	@ExceptionHandler(value = {ReservationServiceException.class})
	public ResponseEntity<Object> handleReservationServiceException(ReservationServiceException ex, WebRequest request) {
		HttpStatus status = reservationServiceErrorMapping.get(ex.getErrorCode());
		ErrorDto errorDto = buildErrorDto(status, ex.getErrorCode().name(), ex.getMessage());
		return handleExceptionInternal(ex, errorDto, new HttpHeaders(), status, request);
	}

	/*
	 * Handles validation errors on ReservationDto fields (mapped from request body):
	 * - missing or invalid email
	 * - missing or invalid fullName
	 * - missing dates (blank, null or absent)
	 * - invalid date range
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatus status,
			WebRequest request) {

		List<String> errorMessages = new ArrayList<>();
		for (ObjectError error : ex.getBindingResult().getAllErrors()) {
			errorMessages.add(error.getDefaultMessage());
		}
		ErrorDto errorDto = buildErrorDto(status, ErrorCode.INVALID_PARAMETERS.name(), Joiner.on(". ").join(errorMessages));
		return handleExceptionInternal(ex, errorDto, new HttpHeaders(), status, request);
	}

	/*
	 * Handles validation errors on DateRangeDto fields (mapped from request params):
	 * - malformed dates
	 * - missing dates
	 * - invalid date range
	 */
	@Override
	protected ResponseEntity<Object> handleBindException(
			BindException ex,
			HttpHeaders headers,
			HttpStatus status,
			WebRequest request) {

		List<ObjectError> errors = ex.getBindingResult().getAllErrors();
		String message = MALFORMED_DATE_ERROR_MESSAGE;

		boolean isDateFormatErrorPresent = errors.stream()
				.flatMap(error -> Arrays.stream(error.getCodes()))
				.anyMatch(code -> code.equalsIgnoreCase("typeMismatch"));

		if (!isDateFormatErrorPresent) {
			message = errors.stream()
					.filter(error -> Arrays.stream(error.getCodes())
							.anyMatch(code -> code.equalsIgnoreCase("AvailabilityDateRange")))
					.findFirst()
					.orElse(null)
					.getDefaultMessage();
		}
		ErrorDto errorDto = buildErrorDto(status, ErrorCode.INVALID_PARAMETERS.name(), message);
		return handleExceptionInternal(ex, errorDto, new HttpHeaders(), status, request);
	}

	/*
	 * Handles validation errors on ReservationDto date fields (mapped from request body):
	 * - malformed dates
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpHeaders headers,
			HttpStatus status,
			WebRequest request) {

		ErrorDto errorDto = buildErrorDto(status, ErrorCode.INVALID_PARAMETERS.name(), MALFORMED_DATE_ERROR_MESSAGE);
		return handleExceptionInternal(ex, errorDto, new HttpHeaders(), status, request);
	}

	/*
	 * Handles missing id in path
	 */
	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
																		 HttpHeaders headers,
																		 HttpStatus status,
																		 WebRequest request) {
		ErrorDto errorDto = buildErrorDto(HttpStatus.BAD_REQUEST, ErrorCode.MISSING_PARAMETERS.name(), ID_MISSING_ERROR_MESSAGE);
		return handleExceptionInternal(ex, errorDto, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(value = {Exception.class})
	public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ErrorDto errorDto = buildErrorDto(status, status.name(), ex.getLocalizedMessage());
		return handleExceptionInternal(ex, errorDto, new HttpHeaders(), status, request);
	}

	/**
	 * Constructs an ErrorDto object to be used as the response body.
	 * @param status
	 * @param errorCode
	 * @param errorMessage
	 * @return
	 */
	private ErrorDto buildErrorDto(HttpStatus status, String errorCode, String errorMessage) {
		return new ErrorDto()
				.status(status.value())
				.code(status.name())
				.errorCode(errorCode)
				.message(errorMessage);
	}
}
