package com.tm.tsm_atelier.infrastructure.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.tm.tsm_atelier.domain.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

	private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

	@Value("${stripe.webhook-secret}")
	private String endpointSecret;

	private final OrderService orderService;

	public StripeWebhookController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader) {

		logger.debug("Received Stripe Webhook request. Validating signature...");

		Event event;

		try {
			event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
		} catch (SignatureVerificationException e) {
			logger.warn("Invalid Stripe signature received from IP/request. Signature: {}", sigHeader, e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
		} catch (Exception e) {
			logger.error("Failed to parse Stripe Webhook payload", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
		}

		logger.info("Successfully verified Stripe Webhook event. Event ID: {}, Type: {}", event.getId(),
				event.getType());

		// Handle the event. Any failure below must surface as 5xx: Stripe only retries
		// on server errors, and answering 200 for an event we did not process means a
		// captured payment is silently lost (the order stays PENDING_PAYMENT and gets
		// cancelled by the expiration scheduler).
		try {
			switch (event.getType()) {
				case "payment_intent.succeeded" : {
					String intentId = requireObjectId(event, payload);
					logger.info("Processing payment success for PaymentIntent ID: {}", intentId);
					orderService.handlePaymentSuccess(intentId);
					logger.info("Finished processing payment success for PaymentIntent ID: {}", intentId);
					break;
				}
				case "payment_intent.payment_failed" : {
					String intentId = requireObjectId(event, payload);
					logger.warn("Processing payment failure for PaymentIntent ID: {}", intentId);
					orderService.handlePaymentFailure(intentId);
					logger.info("Finished processing payment failure for PaymentIntent ID: {}", intentId);
					break;
				}
				default :
					logger.debug("Unhandled Stripe event type received: {}", event.getType());
			}
		} catch (Exception e) {
			logger.error("Failed to process Stripe event. Event ID: {}, Type: {} — returning 500 so Stripe retries",
					event.getId(), event.getType(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
		}

		return ResponseEntity.ok("Success");
	}

	private String requireObjectId(Event event, String payload) {
		String objectId = extractObjectId(event, payload);
		if (objectId == null) {
			throw new IllegalStateException("Could not extract PaymentIntent ID from event " + event.getId());
		}
		return objectId;
	}

	private String extractObjectId(Event event, String payload) {
		// Try strict deserialization first
		if (event.getDataObjectDeserializer().getObject().isPresent()) {
			return ((com.stripe.model.HasId) event.getDataObjectDeserializer().getObject().get()).getId();
		}
		// Fallback to regex if there's an API version mismatch between stripe-java and
		// the webhook
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(pi_[^\"]+)\"").matcher(payload);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}
}
