// Buscador y paginación sencillos para las tablas largas (sin librerías).
document.querySelectorAll('.badge.estado').forEach(function (b) {
  b.classList.add('e-' + b.textContent.trim());
});

// Pasa a minúsculas y quita tildes ("Ramírez" -> "ramirez", "Íñigo" -> "inigo").
function normalizarBusqueda(texto) {
  return (texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

// ¿El texto cumple con UNA palabra buscada? Todo se busca por el INICIO:
//  - Palabras con letras ("lau", "c", "ramirez", "gmail"): deben coincidir con el
//    INICIO de alguna palabra del texto. Así "lau" encuentra "Laura" o "Lauris",
//    pero no "Claudia". En un correo cuentan como separadores @ . _ - (así
//    "cardona" encuentra "laura.cardona@gmail.com" y "gmail" también).
//  - Números, fechas, horas y montos ("315", "1094", "14/11", "10:00", "55000"):
//    deben coincidir con el INICIO de un número de la celda. Así "315" encuentra
//    el teléfono 3151013315 pero NO 3131515753, y "14/11" encuentra 14/11/2026.
//    En los montos se ignoran los puntos y comas de miles ("55000" = "$55.000").
var BORDE = '(^|[^a-z0-9])';
function escaparRegex(t) { return t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
function sinSeparadoresMiles(t) { return t.replace(/(\d)[.,](?=\d)/g, '$1'); }

function cumplePalabra(texto, palabra) {
  if (/[a-z]/.test(palabra) || !/^\d/.test(palabra)) {
    // Con letras o símbolos al inicio ("#502", "$55"): inicio de palabra.
    var re = new RegExp(BORDE + escaparRegex(palabra));
    return re.test(texto) || re.test(sinSeparadoresMiles(texto))
      || new RegExp(BORDE + escaparRegex(sinSeparadoresMiles(palabra))).test(sinSeparadoresMiles(texto));
  }
  // Empieza por dígito: se compara con el inicio de cada número del texto.
  // Un "número" es una seguidilla de dígitos con . , : / - en medio
  // (fechas 14/11/2026, horas 10:00, montos 55.000), que no esté pegada a letras.
  var limpia = palabra.replace(/[.,]/g, '');
  var numeros = /(^|[^a-z0-9])(\d[\d.,:\/-]*)/g, m;
  while ((m = numeros.exec(texto)) !== null) {
    var num = m[2];
    if (num.indexOf(palabra) === 0) return true;
    if (num.replace(/[.,]/g, '').indexOf(limpia) === 0) return true;
  }
  return false;
}

// Si se escriben varias palabras con letras ("lau c"), arma una expresión que
// las exige SEGUIDAS y en ese orden ("Laura Cardona"). Devuelve null si hay una sola.
function fraseSeguida(palabras) {
  var conLetras = palabras.filter(function (p) { return /[a-z]/.test(p); });
  if (conLetras.length < 2) return null;
  var partes = conLetras.map(function (p) { return escaparRegex(p) + '[a-z0-9]*'; });
  return new RegExp('(^|[^a-z0-9])' + partes.join('[^a-z0-9]+'));
}

document.querySelectorAll('table.tabla-paginada').forEach(function (tabla) {
  var porPagina = 25, pagina = 0, palabras = [], columna = -1;
  var filas = Array.from(tabla.tBodies[0].rows);
  var paginador = document.createElement('div');
  paginador.className = 'paginador d-flex gap-1 flex-wrap align-items-center my-2';
  tabla.parentNode.insertAdjacentElement('afterend', paginador);

  // Columnas que no se usan al buscar en "Todas las columnas" (la de botones).
  var ignoradas = [];
  if (tabla.tHead) {
    Array.from(tabla.tHead.rows[0].cells).forEach(function (th, i) {
      if (th.textContent.trim().toLowerCase() === 'acciones') ignoradas.push(i);
    });
  }
  // Texto de cada fila ya normalizado; las celdas se separan con espacio
  // para que no se peguen palabras de columnas vecinas.
  var textos = filas.map(function (f) {
    return Array.from(f.cells).map(function (c, i) {
      return ignoradas.indexOf(i) >= 0 ? '' : normalizarBusqueda(c.textContent);
    }).join(' ');
  });

  function textoDe(f, n) {
    return (columna >= 0 && f.cells[columna])
      ? normalizarBusqueda(f.cells[columna].textContent)
      : textos[n];
  }

  function visibles() {
    if (palabras.length === 0) return filas;
    // 1) Todas las palabras escritas deben cumplirse (no basta con una).
    var cumplen = [];
    filas.forEach(function (f, n) {
      var texto = textoDe(f, n);
      if (palabras.every(function (p) { return cumplePalabra(texto, p); })) {
        cumplen.push({ fila: f, texto: texto });
      }
    });
    // 2) Con varias palabras ("lau c"), si hay filas donde aparecen seguidas
    //    ("Laura Cardona"), se muestran solo esas. Si no hay ninguna
    //    (p. ej. "cardona laura"), se dejan todas las del paso 1.
    var frase = fraseSeguida(palabras);
    if (frase) {
      var seguidas = cumplen.filter(function (c) { return frase.test(c.texto); });
      if (seguidas.length > 0) cumplen = seguidas;
    }
    return cumplen.map(function (c) { return c.fila; });
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
      palabras = normalizarBusqueda(buscador.value).trim().split(/\s+/).filter(Boolean);
      columna = parseInt(selector.value, 10);
      pagina = 0;
      pintar();
    }
    buscador.addEventListener('input', actualizarFiltro);
    selector.addEventListener('change', actualizarFiltro);
  }
  pintar();
});