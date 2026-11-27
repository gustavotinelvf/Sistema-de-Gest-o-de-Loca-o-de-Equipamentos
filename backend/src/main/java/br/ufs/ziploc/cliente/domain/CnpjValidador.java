package br.ufs.ziploc.cliente.domain;

//Validação de formato e dígitos verificadores de CNPJ (RD007)

public final class CnpjValidador {

    private static final int[] PESOS_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DIGITO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidador() {
    }

    public static String limpar(String cnpj) {
        return cnpj == null ? null : cnpj.replace(".", "").replace("/", "").replace("-", "");
    }

    public static boolean ehValido(String cnpj) {
        if (cnpj == null || !cnpj.matches("[0-9]{14}|[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2}")) {
            return false;
        }

        String digitos = limpar(cnpj);

        if (digitos.chars().distinct().count() == 1) {
            return false;
        }

        int primeiroDigito = calcularDigito(digitos, PESOS_PRIMEIRO_DIGITO);
        int segundoDigito = calcularDigito(digitos, PESOS_SEGUNDO_DIGITO);

        return (digitos.charAt(12) - '0') == primeiroDigito
                && (digitos.charAt(13) - '0') == segundoDigito;
    }

    private static int calcularDigito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
