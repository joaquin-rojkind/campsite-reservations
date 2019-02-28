package com.upgrade.campsite.rest.advice;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDto {

	private Integer status;
	private String statusCode;
	private String errorCode;
	private String message;

	public Integer getStatus() {
		return status;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	public ErrorDto status(Integer status) {
		this.status = status;
		return this;
	}

	public ErrorDto code(String code) {
		this.statusCode = code;
		return this;
	}

	public ErrorDto errorCode(String errorCode) {
		this.errorCode = errorCode;
		return this;
	}

	public ErrorDto message(String message) {
		this.message = message;
		return this;
	}
}
