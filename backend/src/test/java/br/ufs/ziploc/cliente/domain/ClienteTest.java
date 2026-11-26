package br.ufs.ziploc.cliente.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    private Cliente criarClienteValido() {
        Contato contato = new Contato("Maria Silva", "12345678901", "maria@construtora.com", "79999990000");
        Endereco endereco = new Endereco(
                "49000000", "Av. Beira Mar", "100", "Sala 2",
                "Jabotiana", "Aracaju", "SE"
        );
        return new Cliente(1L, "Construtora Horizonte Ltda", "12345678000199", contato, endereco);
    }

    @Test
    void deveNascerAtivoEAdimplenteAoCriarCliente_RF001() {
        Cliente cliente = criarClienteValido();
        cliente.aoPersistir();

        assertEquals(1L, cliente.getLocadoraId());
        assertTrue(cliente.isAtivo());
        assertEquals(StatusAdimplencia.ADIMPLENTE, cliente.getStatusAdimplencia());
        assertNotNull(cliente.getDataCadastro());
        assertNull(cliente.getDataAnonimizacao());
    }

    @Test
    void deveRejeitarRazaoSocialEmBranco_RF001() {
        Contato contato = new Contato("Maria Silva", "12345678901", "maria@construtora.com", "79999990000");
        Endereco endereco = new Endereco(
                "49000000", "Av. Beira Mar", "100", null,
                "Jabotiana", "Aracaju", "SE"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Cliente(1L, "   ", "12345678000199", contato, endereco)
        );
    }

    @Test
    void deveAtualizarDadosCadastraisMantendoCnpjELocadora_RF002() {
        Cliente cliente = criarClienteValido();
        Contato novoContato = new Contato("Carlos Souza", "98765432100", "carlos@construtora.com", "79988887777");
        Endereco novoEndereco = new Endereco(
                "49010000", "Rua Nova", "200", null,
                "Centro", "Aracaju", "SE"
        );

        cliente.atualizarDados("Construtora Horizonte S/A", novoContato, novoEndereco);

        assertEquals(1L, cliente.getLocadoraId());
        assertEquals("Construtora Horizonte S/A", cliente.getRazaoSocial());
        assertEquals("12345678000199", cliente.getCnpj());
        assertEquals("98765432100", cliente.getContato().getCpf());
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
        assertNull(cliente.getContato().getCpf());
        assertNull(cliente.getContato().getEmail());
        assertNull(cliente.getContato().getTelefone());
        assertNotNull(cliente.getDataAnonimizacao());
    }
}