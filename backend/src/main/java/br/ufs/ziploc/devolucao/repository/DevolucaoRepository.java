package br.ufs.ziploc.devolucao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ufs.ziploc.devolucao.domain.Devolucao;
import java.util.List;



public interface DevolucaoRepository extends JpaRepository<Devolucao,Long> {

    /* findByItemContratoId: É uma busca mais unitária, ou seja, procura a devolução de um item específico*/
    Optional<Devolucao> findByItemContratoId(Long itemContratoId);
    /*findByItemContratoIdLista: O método recebe uma lista de ids de uma só vez de um contrato escolhido e os devolve */
    List<Devolucao> findByItemContratoIdIn(List <Long> itemContratoIds);
}
