// TODO: NOMES A SEREM SUBSTITUÍDOS A POSTERIORI, NOMES TEMPORÁRIOS PARA VER SE TÁ FUNCIONANDO NUM TESTE

package br.ufs.ziploc.manutencao.fake;

import br.ufs.ziploc.equipamento.api.EquipamentoApi;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Profile("fake")
class EquipamentoApiFake implements EquipamentoApi {

    // TODO: CHECAR = Os ids precisam EXISTIR em unidade_equipamento (FK da tabela manutencao)
    private static final Map<String, Long> UNIDADES = Map.of(
            "SN-0001", 1L,
            "SN-0002", 2L);

    @Override
    public Optional<Long> buscarUnidadeIdPorNumeroSerie(String numeroSerie) {
        return Optional.ofNullable(UNIDADES.get(numeroSerie));
    }
}