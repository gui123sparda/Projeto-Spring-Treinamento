package br.gov.ce.detran.treinamento.spring.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String anterior = MDC.get("requestId");
        String requestId = UUID.randomUUID().toString();
        String caminho = request.getRequestURI().replaceAll("[\\r\\n\\t]", "_");
        long inicio = System.nanoTime();
        boolean falhou = false;
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            LOG.info("Requisição iniciada: {} {}", request.getMethod(), caminho);
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error exception) {
            falhou = true;
            LOG.error("Falha ao processar {} {}", request.getMethod(), caminho, exception);
            throw exception;
        } finally {
            try {
                int status = falhou ? 500 : response.getStatus();
                long duracao = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                var nivel = status >= 500 ? LOG.atError() : status >= 400 ? LOG.atWarn() : LOG.atInfo();
                nivel.log("Requisição concluída: {} {} status={} duracaoMs={}",
                        request.getMethod(), caminho, status, duracao);
            } finally {
                if (anterior == null) {
                    MDC.remove("requestId");
                } else {
                    MDC.put("requestId", anterior);
                }
            }
        }
    }
}
