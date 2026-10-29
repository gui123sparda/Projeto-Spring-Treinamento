package br.gov.ce.detran.treinamento.spring.notificacao;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificacoesController {
    private final Notificacoes notificacoes;

    public NotificacoesController(Notificacoes notificacoes) {
        this.notificacoes = notificacoes;
    }

    @GetMapping("/notificacoes")
    public ResponseEntity<Notificacoes.Consulta> consultar(@RequestParam(required = false) String apos) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(notificacoes.consultar(apos));
    }
}
