(() => {
    "use strict";
    const proprietario = document.getElementById("idProprietario");
    const dados = document.getElementById("dados-veiculo");
    if (!proprietario || !dados) return;
    function atualizar() {
        dados.hidden = !proprietario.value;
        dados.disabled = !proprietario.value;
    }
    proprietario.addEventListener("change", atualizar);
    atualizar();
})();
