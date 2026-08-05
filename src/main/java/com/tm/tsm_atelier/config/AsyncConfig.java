package com.tm.tsm_atelier.config;

import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean("emailTaskExecutor")
	public Executor emailTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("email-async-");
		executor.setTaskDecorator(mdcPropagatingDecorator());
		executor.initialize();
		return executor;
	}

	/**
	 * O MDC é guardado numa ThreadLocal, então nada dele atravessa para o pool de
	 * envio de e-mail sozinho: a falha registrada em OrderConfirmationEmailListener
	 * apareceria sem requestId, justamente no caso em que ninguém está olhando a
	 * tela para reportar o problema. Copiar o contexto na submissão da tarefa
	 * mantém o rastro ligado à requisição que originou o envio.
	 */
	private TaskDecorator mdcPropagatingDecorator() {
		return runnable -> {
			Map<String, String> callerContext = MDC.getCopyOfContextMap();

			return () -> {
				if (callerContext != null) {
					MDC.setContextMap(callerContext);
				}
				try {
					runnable.run();
				} finally {
					// As threads do pool são reaproveitadas, então o contexto precisa
					// morrer com a tarefa e não vazar para a próxima.
					MDC.clear();
				}
			};
		};
	}
}
