package com.motoboutique.shared.web;

import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estado")
public class EstadoAplicacionController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EstadoAplicacionController(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{modulo}")
    JsonNode obtener(@AuthenticationPrincipal UsuarioAutenticado usuario,
                     @PathVariable String modulo) throws Exception {
        return jdbcTemplate.query("SELECT datos FROM estado_aplicacion WHERE usuario_id = ? AND modulo = ?",
                rs -> rs.next() ? objectMapper.readTree(rs.getString("datos")) : objectMapper.createObjectNode(),
                usuario.getId(), modulo);
    }

    @PutMapping("/{modulo}")
    void guardar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                 @PathVariable String modulo, @RequestBody JsonNode datos) {
        jdbcTemplate.update("INSERT INTO estado_aplicacion(usuario_id, modulo, datos, modificado_en) "
                        + "VALUES (?, ?, CAST(? AS jsonb), now()) "
                        + "ON CONFLICT(usuario_id, modulo) DO UPDATE SET datos = EXCLUDED.datos, modificado_en = now()",
                usuario.getId(), modulo, datos.toString());
    }
}
