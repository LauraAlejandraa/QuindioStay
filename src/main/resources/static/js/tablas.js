// Buscador y paginación sencillos para las tablas largas (sin librerías).
document.querySelectorAll('.badge.estado').forEach(function (b) {
  b.classList.add('e-' + b.textContent.trim());
});

document.querySelectorAll('table.tabla-paginada').forEach(function (tabla) {
  var porPagina = 25, pagina = 0, palabras = [], columna = -1;
  var filas = Array.from(tabla.tBodies[0].rows);
  var paginador = document.createElement('div');
  paginador.className = 'paginador d-flex gap-1 flex-wrap align-items-center my-2';
  tabla.parentNode.insertAdjacentElement('afterend', paginador);

  function visibles() {
    if (palabras.length === 0) return filas;
    return filas.filter(function (f) {
      var texto;
      if (columna >= 0 && f.cells[columna]) {
        texto = f.cells[columna].textContent.toLowerCase();
      } else {
        texto = f.textContent.toLowerCase();
      }
      return palabras.every(function (p) { return texto.indexOf(p) >= 0; });
    });
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
    var envoltura = document.createElement('div');
    envoltura.className = 'd-flex gap-2 align-items-center mb-2';
    buscador.classList.remove('mb-2');
    buscador.classList.add('flex-grow-1');
    buscador.parentNode.insertBefore(envoltura, buscador);
    envoltura.appendChild(buscador);

    var selector = document.createElement('select');
    selector.className = 'form-select form-select-sm w-auto';
    var opcionTodas = document.createElement('option');
    opcionTodas.value = '-1';
    opcionTodas.textContent = 'Todas las columnas';
    selector.appendChild(opcionTodas);
    if (tabla.tHead) {
      Array.from(tabla.tHead.rows[0].cells).forEach(function (th, i) {
        var texto = th.textContent.trim();
        if (!texto || texto.toLowerCase() === 'acciones') return;
        var opcion = document.createElement('option');
        opcion.value = String(i);
        opcion.textContent = texto;
        selector.appendChild(opcion);
      });
    }
    envoltura.appendChild(selector);

    function actualizarFiltro() {
      palabras = buscador.value.toLowerCase().trim().split(/\s+/).filter(Boolean);
      columna = parseInt(selector.value, 10);
      pagina = 0;
      pintar();
    }
    buscador.addEventListener('input', actualizarFiltro);
    selector.addEventListener('change', actualizarFiltro);
  }
  pintar();
});