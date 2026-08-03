package com.tm.tsm_atelier.common.exception;

import com.tm.tsm_atelier.common.exception.custom.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		logger.warn("Validation failed for request: " + ex.getMessage());

		ProblemDetail problem = ProblemDetail.forStatus(422);
		problem.setTitle("Validation error");
		problem.setDetail("One or more fields are invalid.");

		Map<String, String> fields = new HashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fields.put(error.getField(), error.getDefaultMessage());
		}
		problem.setProperty("fields", fields);

		return ResponseEntity.status(422).body(problem);
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Email in use");
		return problem;
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("User not found");
		return problem;
	}

	@ExceptionHandler(EntityAlreadyExistsException.class)
	public ProblemDetail handleEntityAlreadyExistsException(EntityAlreadyExistsException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problemDetail.setTitle("Data conflict");
		return problemDetail;
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problem.setTitle("Invalid token");
		return problem;
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
		problem.setTitle("Authentication failed");
		return problem;
	}

	@ExceptionHandler(EmailNotVerifiedException.class)
	public ProblemDetail handleEmailNotVerified(EmailNotVerifiedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problem.setTitle("Email not verified");
		return problem;
	}

	@ExceptionHandler(EmailAlreadyVerifiedException.class)
	public ProblemDetail handleEmailAlreadyVerified(EmailAlreadyVerifiedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Email already verified");
		return problem;
	}

	@ExceptionHandler(DisabledException.class)
	public ProblemDetail handleDisabledException(DisabledException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"Please verify your email before logging in.");
		problem.setTitle("Email not verified");
		return problem;
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ProblemDetail handleAccessDenied(AuthorizationDeniedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problem.setTitle("Access denied");
		return problem;
	}

	@ExceptionHandler(FileUploadException.class)
	public ProblemDetail handleFileUploadException(FileUploadException ex) {
		logger.error("File upload error: ", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Upload Error");
		return problem;
	}

	@ExceptionHandler(InvalidFileTypeException.class)
	public ProblemDetail handleInvalidFileTypeException(InvalidFileTypeException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
		problem.setTitle("Invalid File Type");
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
			org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
				"O arquivo excede o limite de tamanho permitido de 5MB.");
		problem.setTitle("Payload Too Large");
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
	}

	@ExceptionHandler(AddressLimitExceededException.class)
	public ProblemDetail handleAddressLimitExceeded(AddressLimitExceededException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problem.setTitle("Address limit exceeded");
		return problem;
	}

	@ExceptionHandler(AddressNotFoundException.class)
	public ProblemDetail handleAddressNotFound(AddressNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Address not found");
		return problem;
	}

	@ExceptionHandler(OutOfStockException.class)
	public ProblemDetail handleOutOfStock(OutOfStockException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Out of stock");
		problem.setProperty("availableQuantity", ex.getAvailableQuantity());
		return problem;
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ProblemDetail handleTooManyRequests(TooManyRequestsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
		problem.setTitle("Too many requests");
		return problem;
	}

	@ExceptionHandler(AccountLockedException.class)
	public ProblemDetail handleAccountLocked(AccountLockedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
		problem.setTitle("Account Locked");
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception ex) {
		// Log the actual error for the developer, generic message for the frontend
		logger.error("Unexpected error: ", ex);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred. Please try again later.");
		problem.setTitle("Internal error");
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Resource not found");
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"A data conflict occurred. The resource may already exist.");
		problem.setTitle("Duplicate entry");
		return problem;
	}
}
