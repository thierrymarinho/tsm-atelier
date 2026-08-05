package com.tm.tsm_atelier.domain.auth.dto;

/**
 * Corpo opcional de POST /refresh. O SPA continua mandando o token pelo cookie
 * httpOnly e nem envia corpo; este caminho existe para clientes que não têm
 * cookie jar — app mobile, script, integração — que antes recebiam um
 * refreshToken no JSON do login sem ter como devolvê-lo.
 */
public record RefreshRequestDTO(String refreshToken) {
}
