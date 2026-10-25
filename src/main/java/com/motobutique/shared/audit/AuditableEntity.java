package com.motobutique.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Clase base de TODAS las entidades del sistema.
 *
 * <p>Aporta: id, auditoría (quién y cuándo creó/modificó), versión para bloqueo
 * optimista y marca de eliminado lógico. Las tablas deben incluir estas columnas:
 * id, creado_por, creado_en, modificado_por, modificado_en, version, eliminado.</p>
 *
 * <p>Los setters son protegidos a propósito: los campos de auditoría los llena Spring Data
 * y no deben poder modificarse desde controllers ni mappers.</p>
 *
 * <p>equals/hashCode se basan en el id (y en la clase real, para soportar proxies lazy de
 * Hibernate). Una entidad nueva, sin id, solo es igual a sí misma.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    @Column(name = "creado_por", nullable = false, updatable = false, length = 100)
    private String creadoPor;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @LastModifiedBy
    @Column(name = "modificado_por", length = 100)
    private String modificadoPor;

    @LastModifiedDate
    @Column(name = "modificado_en")
    private Instant modificadoEn;

    /** Bloqueo optimista: Hibernate lo incrementa en cada actualización. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Eliminado lógico: el registro no se borra, solo se marca. */
    @Column(name = "eliminado", nullable = false)
    private boolean eliminado = false;

    public void marcarEliminado() {
        this.eliminado = true;
    }

    public void restaurar() {
        this.eliminado = false;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> claseOtro = o instanceof HibernateProxy proxyOtro
                ? proxyOtro.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> claseEsta = this instanceof HibernateProxy proxyEsta
                ? proxyEsta.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (claseEsta != claseOtro) {
            return false;
        }
        AuditableEntity otra = (AuditableEntity) o;
        return getId() != null && Objects.equals(getId(), otra.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
