package br.ufs.ziploc.cliente.api;

import br.ufs.ziploc.cliente.domain.StatusAdimplencia;

//Interface pública do módulo Cliente (Regras D2 e D3)
//Outros módulos devem usar apenas esta API para consultar dados ou solicitar alteração de adimplência

public interface ClienteApi {

    ClienteResumoDTO consultarResumo(Long clienteId);

    boolean verificarAdimplencia(Long clienteId);

    void alterarStatusAdimplencia(Long clienteId, StatusAdimplencia status);
}