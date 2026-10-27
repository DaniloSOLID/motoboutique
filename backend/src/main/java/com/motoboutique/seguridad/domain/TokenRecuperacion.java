package com.motoboutique.seguridad.domain;

import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de un solo uso para restablecer la contraseña.
 *
 * <p>Se guarda el hash SHA-256 (hexadecimal) del token, nunca el token en claro: el valor real
 * solo viaja en el enlace enviado al usuario.</p>
 */
@Entity
@Table(name = "tokens_recuperacion")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    /** Vigente = todavía no se usó y no llegó su hora de expiración. */
    public boolean estaVigente(Instant ahora) {
        return usadoEn == null && ahora.isBefore(expiraEn);
    }

    public void marcarUsado(Instant ahora) {
        this.usadoEn = ahora;
    }
}
