package com.tm.tsm_atelier.infrastructure.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.port.PaymentGatewayPort;
import com.tm.tsm_atelier.domain.order.port.PaymentIntentResult;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentAdapter implements PaymentGatewayPort {

	@Value("${stripe.api-key}")
	private String stripeApiKey;

	@PostConstruct
	public void init() {
		Stripe.apiKey = stripeApiKey;
	}

	@Override
	public PaymentIntentResult createPaymentIntent(Order order) {
		try {
			// A Stripe cobra em centavos: R$ 10,00 vira 1000.
			long amountInCents = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();

			PaymentIntentCreateParams params = PaymentIntentCreateParams.builder().setAmount(amountInCents)
					.setCurrency("brl").putMetadata("orderId", order.getId().toString())
					.putMetadata("userId", order.getUser().getId().toString()).build();

			// Chaveado pelo pedido para que uma retentativa — nossa ou do SDK —
			// reaproveite o mesmo PaymentIntent em vez de criar uma cobrança duplicada.
			RequestOptions options = RequestOptions.builder()
					.setIdempotencyKey("order-" + order.getId() + "-payment-intent").build();

			PaymentIntent paymentIntent = PaymentIntent.create(params, options);

			return new PaymentIntentResult(paymentIntent.getId(), paymentIntent.getClientSecret());

		} catch (StripeException e) {
			throw new RuntimeException("Failed to communicate with Stripe payment gateway", e);
		}
	}
}
