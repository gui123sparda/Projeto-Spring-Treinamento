(function () {
    "use strict";

    const campo = document.getElementById("placa");
    if (!campo) {
        return;
    }

    const posicoes = [/[A-Z]/, /[A-Z]/, /[A-Z]/, /[0-9]/, /[A-Z0-9]/, /[0-9]/, /[0-9]/];

    function filtrar(valor) {
        const caracteres = valor.toUpperCase().replace(/[^A-Z0-9]/g, "");
        let placa = "";
        for (const caractere of caracteres) {
            if (placa.length === posicoes.length) {
                break;
            }
            if (posicoes[placa.length].test(caractere)) {
                placa += caractere;
            }
        }
        return placa;
    }

    function atualizar() {
        const cursor = campo.selectionStart === null ? campo.value.length : campo.selectionStart;
        const caracteresAntesDoCursor = filtrar(campo.value.slice(0, cursor)).length;
        campo.value = filtrar(campo.value);
        if (document.activeElement === campo) {
            campo.setSelectionRange(caracteresAntesDoCursor, caracteresAntesDoCursor);
        }
    }

    campo.addEventListener("input", atualizar);
    campo.form.addEventListener("submit", atualizar);
    atualizar();
}());
