package br.ufs.ziploc.integracao.cep;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ViaCepGatewayTimeoutTest {

    @Test
    @Timeout(12)
    void consultar_servidorLento_respeitaTimeoutDeLeitura() throws Exception {
        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        var executor = Executors.newSingleThreadExecutor();
        var requisicaoRecebida = new CountDownLatch(1);
        var liberarResposta = new CountDownLatch(1);

        servidor.setExecutor(executor);

        servidor.createContext("/", exchange -> {
            requisicaoRecebida.countDown();

            try {
                liberarResposta.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });

        servidor.start();

        try {
            String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
            ViaCepGateway gateway = new ViaCepGateway(url);

            long inicio = System.nanoTime();

            assertThrows(CepIndisponivelException.class,
                    () -> gateway.consultar("49010000"));

            Duration tempo = Duration.ofNanos(System.nanoTime() - inicio);

            assertTrue(requisicaoRecebida.getCount() == 0,
                    "O servidor deveria receber a requisição");

            assertTrue(tempo.compareTo(Duration.ofMillis(2500)) >= 0,
                    "A consulta falhou antes do timeout esperado: " + tempo);

            assertTrue(tempo.compareTo(Duration.ofSeconds(6)) < 0,
                    "A consulta excedeu o limite aceitável: " + tempo);

        } finally {
            liberarResposta.countDown();
            servidor.stop(0);
            executor.shutdownNow();
        }
    }
}
