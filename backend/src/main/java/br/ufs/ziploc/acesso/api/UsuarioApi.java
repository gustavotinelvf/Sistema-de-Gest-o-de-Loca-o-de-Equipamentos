package br.ufs.ziploc.acesso.api;

import java.util.Optional;

/** Contrato público para consulta de usuários por outros módulos. */
public interface UsuarioApi {

    Optional<String> consultarPerfilAtivo(Long usuarioId);
}