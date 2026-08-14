package com.tm.tsm_atelier.domain.admin.service;

import static com.tm.tsm_atelier.common.utils.AuditUtils.currentActor;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.domain.admin.dto.AuditLogResponseDTO;
import com.tm.tsm_atelier.domain.admin.dto.AuditLogSearchFilter;
import com.tm.tsm_atelier.domain.admin.entity.AdminAuditLog;
import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import com.tm.tsm_atelier.domain.admin.repository.AdminAuditLogRepository;
import com.tm.tsm_atelier.domain.admin.repository.AuditLogSpecification;
import com.tm.tsm_atelier.domain.product.enums.StockChangeReason;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escreve e lê o rastro de alterações do painel.
 *
 * O ator não é parâmetro. Ele sai do contexto de segurança, dentro deste
 * serviço — foi para isto que AuditUtils foi extraído. Se cada chamador
 * passasse o seu, bastaria um esquecer para a linha registrar a ação errada em
 * nome de ninguém, e nada no compilador apontaria isso.
 *
 * MANDATORY na classe, e não REQUIRES_NEW. A linha de auditoria precisa
 * compartilhar o destino da alteração que ela descreve. Numa transação própria,
 * um rollback do serviço chamador deixaria registrado um cancelamento que não
 * aconteceu — e o registro é a única coisa que este código produz, então uma
 * mentira aqui é pior do que a ausência. A contrapartida é aceita de propósito:
 * se a gravação do rastro falhar, a alteração cai junto, porque mudança
 * administrativa sem registro é exatamente o que esta tabela existe para
 * impedir.
 *
 * Como efeito colateral útil, MANDATORY recusa qualquer chamada feita fora de
 * uma transação: um serviço novo que esqueça o @Transactional falha na hora, em
 * vez de gravar auditoria que não acompanha nada.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AuditService {

	private final AdminAuditLogRepository auditLogRepository;

	/**
	 * Registra uma ação sem par antes/depois: criação, exclusão, restauração, ou
	 * uma edição cujo diff não cabe numa coluna.
	 */
	public void record(AuditedEntity entityType, Object entityId, AuditAction action) {
		record(entityType, entityId, action, null, null, null, null);
	}

	public void record(AuditedEntity entityType, Object entityId, AuditAction action, String details) {
		record(entityType, entityId, action, null, null, null, details);
	}

	/**
	 * Registra a mudança de um campo. Um par igual não vira linha: o PUT de produto
	 * reenvia o preço promocional a cada salvamento, e sem esta guarda o histórico
	 * de preço acumularia uma entrada "de 79.90 para 79.90" por edição de
	 * descrição.
	 */
	public void recordChange(AuditedEntity entityType, Object entityId, AuditAction action, Object previousValue,
			Object newValue) {
		if (Objects.equals(previousValue, newValue)) {
			return;
		}

		record(entityType, entityId, action, previousValue, newValue, null, null);
	}

	/**
	 * O único evento que preenche todas as colunas, e por isso tem método próprio
	 * em vez de uma chamada de sete argumentos no StockService.
	 *
	 * O código do SKU vai junto porque a linha precisa continuar legível sozinha: o
	 * SKU pode ser removido depois, e um histórico que só faz sentido enquanto o
	 * registro existe não serve para investigar o que aconteceu com ele.
	 */
	public void recordStockChange(Long skuId, String skuCode, int previousQuantity, int newQuantity,
			StockChangeReason reason) {
		record(AuditedEntity.PRODUCT_SKU, skuId, AuditAction.STOCK_ADJUSTED, previousQuantity, newQuantity,
				reason.name(), "SKU " + skuCode);
	}

	@Transactional(readOnly = true)
	public Page<AuditLogResponseDTO> search(AuditLogSearchFilter filter, Pageable pageable) {
		if (filter.createdFrom() != null && filter.createdTo() != null
				&& filter.createdFrom().isAfter(filter.createdTo())) {
			throw new BusinessRuleException(
					"createdFrom (" + filter.createdFrom() + ") is after createdTo (" + filter.createdTo() + ").");
		}

		Specification<AdminAuditLog> spec = Specification
				.where(AuditLogSpecification.hasEntityType(filter.entityType()))
				.and(AuditLogSpecification.hasEntityId(filter.entityId()))
				.and(AuditLogSpecification.hasActor(filter.actor()))
				.and(AuditLogSpecification.hasAction(filter.action()))
				.and(AuditLogSpecification.createdFrom(filter.createdFrom()))
				.and(AuditLogSpecification.createdTo(filter.createdTo()));

		return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
	}

	private void record(AuditedEntity entityType, Object entityId, AuditAction action, Object previousValue,
			Object newValue, String reason, String details) {
		auditLogRepository.save(AdminAuditLog.builder().actor(currentActor()).entityType(entityType)
				.entityId(String.valueOf(entityId)).action(action).previousValue(asText(previousValue))
				.newValue(asText(newValue)).reason(reason).details(details).createdAt(LocalDateTime.now()).build());
	}

	/**
	 * null continua null em vez de virar a string "null": a coluna é anulável e a
	 * diferença entre "não se aplica" e o texto de quatro letras some para sempre
	 * depois de gravada. É o caso real de retirar uma promoção, onde o valor novo é
	 * ausência de preço.
	 */
	private String asText(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private AuditLogResponseDTO toResponse(AdminAuditLog log) {
		return new AuditLogResponseDTO(log.getId(), log.getActor(), log.getEntityType(), log.getEntityId(),
				log.getAction(), log.getPreviousValue(), log.getNewValue(), log.getReason(), log.getDetails(),
				log.getCreatedAt());
	}
}
