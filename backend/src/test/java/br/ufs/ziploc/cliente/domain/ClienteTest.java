package br.ufs.ziploc.cliente.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    private Cliente criarClienteValido() {
        Contato contato = new Contato("Maria Silva", "maria@construtora.com", "79999990000");
        Endereco endereco = new Endereco(
                "49000000", "Av. Beira Mar", "100", "Sala 2",
                "Jabotiana", "Aracaju", "SE"
        );
        return new Cliente("Construtora Horizonte Ltda", "12345678000199", contato, endereco);
    }

    @Test
    void deveNascerAtivoEAdimplenteAoCriarCliente_RF001() {
        Cliente cliente = criarClienteValido();
        cliente.aoPersistir();

        assertTrue(cliente.isAtivo());
        assertEquals(StatusAdimplencia.ADIMPLENTE, cliente.getStatusAdimplencia());
        assertNotNull(cliente.getDataCadastro());
        assertNull(cliente.getAnonimizadoEm());
    }

    @Test
    void deveAtualizarDadosCadastraisMantendoCnpj_RF002() {
        Cliente cliente = criarClienteValido();
        Contato novoContato = new Contato("Carlos Souza", "carlos@construtora.com", "79988887777");
        Endereco novoEndereco = new Endereco(
                "49010000", "Rua Nova", "200", null,
                "Centro", "Aracaju", "SE"
        );

        cliente.atualizarDados("Construtora Horizonte S/A", novoContato, novoEndereco);

        assertEquals("Construtora Horizonte S/A", cliente.getRazaoSocial());
        assertEquals("12345678000199", cliente.getCnpj());
        assertEquals("carlos@construtora.com", cliente.getContato().getEmail());
        assertEquals("Centro", cliente.getEndereco().getBairro());
    }

    @Test
    void deveInativarEReativarClientePreservandoHistorico_RF003_RF044() {
        Cliente cliente = criarClienteValido();

        cliente.inativar();
        assertFalse(cliente.isAtivo());

        cliente.reativar();
        assertTrue(cliente.isAtivo());
        assertEquals("12345678000199", cliente.getCnpj());
    }

    @Test
    void deveAlterarStatusDeAdimplencia_RF018() {
        Cliente cliente = criarClienteValido();

        cliente.alterarStatusAdimplencia(StatusAdimplencia.INADIMPLENTE);
        assertEquals(StatusAdimplencia.INADIMPLENTE, cliente.getStatusAdimplencia());

        cliente.alterarStatusAdimplencia(StatusAdimplencia.ADIMPLENTE);
        assertEquals(StatusAdimplencia.ADIMPLENTE, cliente.getStatusAdimplencia());
    }

    @Test
    void deveAnonimizarDadosPessoaisDeContato_RD005() {
        Cliente cliente = criarClienteValido();

        cliente.anonimizarDados();

        assertEquals("ANONIMIZADO", cliente.getContato().getNome());
        assertNotEquals("maria@construtora.com", cliente.getContato().getEmail());
        assertEquals("0000000000", cliente.getContato().getTelefone());
        assertNotNull(cliente.getAnonimizadoEm());
    }
}