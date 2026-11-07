// Buscador y paginación sencillos para las tablas largas (sin librerías).
document.querySelectorAll('.badge.estado').forEach(function (b) {
  b.classList.add('e-' + b.textContent.trim());
});

document.querySelectorAll('table.tabla-paginada').forEach(function (tabla) {
  var porPagina = 25, pagina = 0, filtro = '';
  var filas = Array.from(tabla.tBodies[0].rows);
  var paginador = document.createElement('div');
  paginador.className = 'paginador d-flex gap-1 flex-wrap align-items-center my-2';
  tabla.parentNode.insertAdjacentElement('afterend', paginador);

  function visibles() {
    return filas.filter(function (f) { return f.textContent.toLowerCase().indexOf(filtro) >= 0; });
  }
  function pintar() {
    var v = visibles();
    var paginas = Math.max(1, Math.ceil(v.length / porPagina));
    if (pagina >= paginas) pagina = paginas - 1;
    filas.forEach(function (f) { f.style.display = 'none'; });
    v.slice(pagina * porPagina, (pagina + 1) * porPagina).forEach(function (f) { f.style.display = ''; });
    paginador.innerHTML = '<span class="text-muted small me-2">' + v.length + ' registros</span>';
    if (paginas > 1) {
      for (var i = 0; i < paginas; i++) {
        if (paginas > 12 && Math.abs(i - pagina) > 3 && i !== 0 && i !== paginas - 1) continue;
        var b = document.createElement('button');
        b.className = 'btn btn-sm ' + (i === pagina ? 'btn-success' : 'btn-outline-secondary');
        b.textContent = i + 1;
        b.onclick = (function (n) { return function () { pagina = n; pintar(); }; })(i);
        paginador.appendChild(b);
      }
    }
  }
  var buscador = document.querySelector('.buscador[data-tabla="' + tabla.id + '"]');
  if (buscador) {
    buscador.addEventListener('input', function () { filtro = buscador.value.toLowerCase(); pagina = 0; pintar(); });
  }
  pintar();
});
