package com.tm.tsm_atelier.common.exception;

import com.tm.tsm_atelier.common.exception.custom.*;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException.Reference;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
			@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {

		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			ResponseEntity<Object> enumProblem = invalidEnumValue(error);
			if (enumProblem != null) {
				return enumProblem;
			}
		}

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

	private ResponseEntity<Object> invalidEnumValue(FieldError error) {
		if (!error.contains(TypeMismatchException.class)) {
			return null;
		}

		Class<?> required = error.unwrap(TypeMismatchException.class).getRequiredType();

		if (required == null || !required.isEnum()) {
			return null;
		}

		logger.warn("Rejected value '" + error.getRejectedValue() + "' for enum field " + error.getField());

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Invalid value");
		problem.setDetail("'" + error.getRejectedValue() + "' is not a valid value for " + error.getField() + ".");
		problem.setProperty("field", error.getField());
		problem.setProperty("allowedValues", Arrays.stream(required.getEnumConstants()).map(Object::toString).toList());

		return ResponseEntity.badRequest().body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
			@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {

		if (ex.getCause() instanceof InvalidFormatException cause && cause.getTargetType() != null
				&& cause.getTargetType().isEnum()) {

			String field = fieldPath(cause);

			logger.warn("Rejected value '" + cause.getValue() + "' for enum field " + field);

			ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
			problem.setTitle("Invalid value");
			problem.setDetail("'" + cause.getValue() + "' is not a valid value for " + field + ".");
			problem.setProperty("field", field);
			problem.setProperty("allowedValues",
					Arrays.stream(cause.getTargetType().getEnumConstants()).map(Object::toString).toList());

			return ResponseEntity.badRequest().body(problem);
		}

		return super.handleHttpMessageNotReadable(ex, headers, status, request);
	}

	private String fieldPath(InvalidFormatException cause) {
		StringBuilder path = new StringBuilder();

		for (Reference reference : cause.getPath()) {
			if (reference.getPropertyName() == null) {
				path.append('[').append(reference.getIndex()).append(']');
			} else {
				if (!path.isEmpty()) {
					path.append('.');
				}
				path.append(reference.getPropertyName());
			}
		}

		return path.isEmpty() ? "the request body" : path.toString();
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

	/**
	 * CONTENT_TOO_LARGE, e não PAYLOAD_TOO_LARGE: a RFC 9110 renomeou o 413 e o
	 * Spring depreciou a constante antiga. O número na resposta é o mesmo, mas as
	 * duas não são a mesma constante do enum, e HttpStatus.valueOf(413) já devolve
	 * a nova — comparar por identidade com a antiga daria falso.
	 */
	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(@NonNull MaxUploadSizeExceededException ex,
			@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
				"O arquivo excede o limite de tamanho permitido de 5MB.");
		problem.setTitle("Payload Too Large");
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(problem);
	}

	@ExceptionHandler(AddressLimitExceededException.class)
	public ProblemDetail handleAddressLimitExceeded(AddressLimitExceededException ex) {
		// UNPROCESSABLE_CONTENT, e não UNPROCESSABLE_ENTITY: a RFC 9110 renomeou o
		// 422 e a constante antiga está depreciada. Mesmo número na resposta.
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
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
		problem.setProperty("skuId", ex.getSkuId());
		problem.setProperty("reason", ex.getReason() == null ? null : ex.getReason().name());
		if (ex.getMaxUnitsPerItem() != null) {
			problem.setProperty("maxUnitsPerItem", ex.getMaxUnitsPerItem());
		}
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

	/**
	 * Sem este handler, validações de negócio voltariam como 500 — reportando erro
	 * de servidor para o que na verdade é erro do cliente, e enchendo o log de
	 * ERROR com stack traces de situações rotineiras.
	 *
	 *
	 * O tipo capturado era IllegalArgumentException, o que também engolia IAE vindo
	 * de dentro do framework: um erro de programação era devolvido ao cliente como
	 * 400 e nunca aparecia como falha. Agora só as exceções de domínio chegam aqui,
	 * e o resto volta a cair no handler genérico, que loga.
	 */
	@ExceptionHandler(BusinessRuleException.class)
	public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ProblemDetail handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Invalid status transition");
		problem.setProperty("from", ex.getFrom());
		problem.setProperty("to", ex.getTo());
		return problem;
	}

	/**
	 * 409, e não 400: o dado enviado pode estar perfeitamente válido — apenas
	 * partiu de uma leitura que envelheceu. A ação do cliente é recarregar e
	 * reenviar, que é justamente o que 409 comunica.
	 */
	@ExceptionHandler(StaleResourceException.class)
	public ProblemDetail handleStaleResource(StaleResourceException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Stale data");
		return problem;
	}

	/**
	 * Conflito de versão detectado pelo próprio Hibernate, e não pela checagem
	 * explícita do serviço — duas transações gravando a mesma linha ao mesmo tempo.
	 * Sem este handler viraria 500, quando a resposta certa é a mesma do caso
	 * acima: recarregue e tente de novo.
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
		logger.warn("Optimistic locking conflict: " + ex.getMessage());

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"This record was changed by someone else while you were editing. Reload and try again.");
		problem.setTitle("Stale data");
		return problem;
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problem.setTitle("Access denied");
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception ex) {
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
