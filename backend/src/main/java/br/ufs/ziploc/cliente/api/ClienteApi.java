package br.ufs.ziploc.cliente.api;

// Interface pública do módulo Cliente (Regras D2 e D3).
// Outros módulos devem usar esta API para consultar dados
// e verificar a adimplência do cliente.
// A alteração da adimplência segue as regras RD010 e RF018.

public interface ClienteApi {

    ClienteResumoDTO consultarResumo(Long clienteId);

    boolean verificarAdimplencia(Long clienteId);

}