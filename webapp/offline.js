(() => {
  "use strict";

  const app = document.getElementById("app");
  const DISTANCES = [3, 4, 5, 6, 0];
  const LEVELS = [
    { label: "20/200", logmar: 1.0 }, { label: "20/160", logmar: 0.9 },
    { label: "20/125", logmar: 0.8 }, { label: "20/100", logmar: 0.7 },
    { label: "20/80", logmar: 0.6 }, { label: "20/63", logmar: 0.5 },
    { label: "20/50", logmar: 0.4 }, { label: "20/40", logmar: 0.3 },
    { label: "20/32", logmar: 0.2 }, { label: "20/25", logmar: 0.1 },
    { label: "20/20", logmar: 0.0 }, { label: "20/16", logmar: -0.1 },
    { label: "20/12.5", logmar: -0.2 },
  ].map((item) => ({ ...item, decimal: Math.pow(10, -item.logmar) }));
  const DIRECTIONS = [0, 90, 180, 270];
  const DESCRIPTIONS = {
    "Afinacion": "Optotipos aislados",
    "Letras Alternas": "Secuencias alternativas de letras",
    "Letras Y Numeros": "Cartillas alfabéticas y numéricas",
    "Niños Y Direccional": "E direccional y símbolos infantiles",
    "Optotipos Convencionales": "Cartillas completas de agudeza visual",
    "Pruebas Y Ayudas Diagnosticas": "Duocromo, fijación y ayudas visuales",
    "Pruebas Y Ayudas Diagnósticas": "Duocromo, fijación y ayudas visuales",
    "Enfermedades Visuales": "Imágenes educativas de alteraciones oculares",
    "Optotipos Complementarios": "Cartillas normales, rotadas e invertidas",
    "Test De Stereopsis": "Láminas de percepción estereoscópica",
    "Test Del Color": "Láminas de percepción cromática",
    "Tipos De Lentes": "Material ilustrativo de lentes",
  };

  let groups = [];
  let view = "loading";
  let selectedHome = 0;
  let selectedGroup = 0;
  let imageIndex = 0;
  let distance = 3;
  let level = 10;
  let mode = "line";
  let randomSeed = 0;
  let inverted = false;
  let showHud = true;
  let hudTimer;
  let calibration = loadCalibration();
  let pxPerMm = calibration ? calibration.pxPerMm : 3.78;
  let screenInches = calibration && calibration.screenInches ? calibration.screenInches : 43;
  let calibrationStage = 0;
  let calibrationFocus = 0;
  let referencePx = calibration
    ? Math.round(calibration.pxPerMm * 100)
    : Math.round(estimatedPxPerMm(screenInches) * 100);
  let isCalibrated = Boolean(calibration);
  if (calibration && calibration.distanceM) distance = calibration.distanceM;

  function loadCalibration() {
    const stored = localStorage.getItem("avtv-calibration-v2");
    if (!stored) return null;
    try {
      const parsed = JSON.parse(stored);
      if (![2, 3].includes(parsed.version) || !(parsed.pxPerMm > 0)) return null;
      return parsed;
    } catch (_) {
      return null;
    }
  }

  function esc(value) {
    return String(value).replace(/[&<>"']/g, (char) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[char]);
  }

  function folderKey(group) {
    return group.folder.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toUpperCase();
  }

  function assetStatus(group, position) {
    const folder = folderKey(group);
    const total = group.images.length;
    if (group.distance === 0) return folder === "ENFERMEDADES VISUALES" || folder === "TIPOS DE LENTES" ? "keep" : "resource";
    if (folder.includes("PRUEBAS Y AYUDAS")) {
      if (position <= 2 || position === total) return "exclude";
      return "keep";
    }
    if (folder === "NIÑOS Y DIRECCIONAL") {
      if (position <= 19 || position === total - 1) return "keep";
      return "exclude";
    }
    if (folder === "LETRAS Y NUMEROS") {
      if (position === total) return "exclude";
      return "keep";
    }
    if (folder === "LETRAS ALTERNAS" && position === total) return "exclude";
    if (folder === "OPTOTIPOS CONVENSIONALES" && position === total) return "exclude";
    return "keep";
  }

  function visibleImages(group) {
    if (!group) return [];
    if (group.distance === 0) return group.images;
    return group.images.filter((_, index) => assetStatus(group, index + 1) !== "exclude");
  }

  function distanceGroups() {
    return groups.filter((group) => group.distance === distance);
  }

  function currentGroup() {
    const list = distanceGroups();
    return list[selectedGroup] || list[0];
  }

  function localSrc(src) {
    return src.replace(/^\/+/, "");
  }

  function estimatedPxPerMm(inches) {
    const diagonalPx = Math.hypot(window.innerWidth, window.innerHeight);
    return diagonalPx / (Math.max(15, inches) * 25.4);
  }

  function physicalScreenSize(inches) {
    const ratio = window.innerWidth / Math.max(1, window.innerHeight);
    const heightInches = inches / Math.sqrt((ratio * ratio) + 1);
    return {
      widthCm: heightInches * ratio * 2.54,
      heightCm: heightInches * 2.54,
    };
  }

  function showEntryHud() {
    showHud = true;
    clearTimeout(hudTimer);
    hudTimer = setTimeout(() => {
      showHud = false;
      if (view === "asset" || view === "exam") render();
    }, 2400);
  }

  function optotypeHeightMm(distanceM, decimal) {
    const minutes = 5 / decimal;
    return distanceM * 1000 * Math.tan((minutes / 60) * Math.PI / 180);
  }

  function landolt(direction, sizePx) {
    return `<svg class="landolt" width="${sizePx}" height="${sizePx}" viewBox="0 0 5 5" aria-label="C de Landolt" style="transform:rotate(${direction}deg)">
      <circle cx="2.5" cy="2.5" r="2" fill="none" stroke="currentColor" stroke-width="1"/>
      <rect x="2.5" y="2" width="2.5" height="1" fill="var(--exam-bg)"/>
    </svg>`;
  }

  function button(label, attrs = "") {
    return `<button ${attrs}>${label}</button>`;
  }

  function renderHome() {
    const options = [
      ["01", "Abrir cartillas", "Optotipos organizados por distancia"],
      ["02", "Calibrar pantalla", "Ajustar el tamaño físico con una regla"],
      ["03", "Optotipos dinámicos", "Generar letras con pantalla calibrada"],
    ];
    app.innerHTML = `
      <main class="home-shell">
        <header class="brand-row">
          <div class="brand-mark">AV</div>
          <div><p class="eyebrow">Sistema de evaluación visual</p><h1>Agudeza Visual</h1></div>
          <div class="status-pill"><span></span> Aplicación offline</div>
        </header>
        <section class="hero-copy">
          <p class="section-kicker">Listo para comenzar</p>
          <h2>Seleccione una opción<br>con el control remoto.</h2>
          <p>Use las flechas para navegar y presione OK para confirmar.</p>
        </section>
        <section class="option-grid">
          ${options.map((item, index) => `
            <button class="option-card ${selectedHome === index ? "active" : ""}" data-home="${index}">
              <span class="option-number">${item[0]}</span>
              <span class="option-title">${item[1]}</span>
              <span class="option-text">${item[2]}</span>
              <span class="option-arrow">→</span>
            </button>`).join("")}
        </section>
        <footer class="home-footer">
          <span><kbd>←</kbd><kbd>→</kbd> Navegar</span><span><kbd>OK</kbd> Seleccionar</span>
          <span class="calibration-state ${isCalibrated ? "ready" : ""}">${isCalibrated ? "Pantalla calibrada" : "Calibración pendiente"}</span>
        </footer>
      </main>`;
    app.querySelectorAll("[data-home]").forEach((element) => {
      element.addEventListener("click", () => openHome(Number(element.dataset.home)));
    });
  }

  function openHome(index) {
    selectedHome = index;
    if (index === 0) view = "library";
    else if (index === 1 || !isCalibrated) {
      calibrationStage = 0;
      calibrationFocus = 0;
      view = "calibration";
    } else {
      view = "exam";
      showEntryHud();
    }
    render();
  }

  function renderLibrary() {
    const list = distanceGroups();
    selectedGroup = Math.max(0, Math.min(selectedGroup, list.length - 1));
    const distancePosition = DISTANCES.indexOf(distance);
    app.innerHTML = `
      <main class="library-shell">
        <header class="library-header">
          <div><p class="section-kicker">Biblioteca de optotipos</p><h1>Seleccione distancia y cartilla</h1></div>
          ${button("← Menú principal", 'data-action="home"')}
        </header>
        <nav class="distance-tabs">
          ${DISTANCES.map((value) => `
            <button class="${distance === value ? "active" : ""}" data-distance="${value}">
              <strong>${value === 0 ? "Más" : value}</strong>
              <span>${value === 0 ? "recursos" : "metros"}</span>
            </button>`).join("")}
        </nav>
        <div class="clinical-banner ${isCalibrated ? "ready" : ""}">
          <strong>${isCalibrated ? "Pantalla calibrada" : "Calibración pendiente"}</strong>
          <span>${isCalibrated ? "Repita la calibración si cambia de pantalla o resolución." : "Calibre la pantalla antes de medir agudeza visual."}</span>
          ${!isCalibrated ? button("Calibrar ahora", 'data-action="calibrate"') : ""}
        </div>
        <nav class="menu-navigation">
          <div class="section-navigation">
            ${button(distance === 0 ? "← Volver a 6 metros" : "← Sección anterior", `data-move-distance="-1" ${distancePosition === 0 ? "disabled" : ""}`)}
            <strong>${distance === 0 ? "Más recursos" : `${distance} metros`}</strong>
            ${button("Sección siguiente →", `data-move-distance="1" ${distancePosition === DISTANCES.length - 1 ? "disabled" : ""}`)}
          </div>
          <div class="test-navigation">
            ${button("← Cartilla anterior", `data-move-group="-1" ${selectedGroup === 0 ? "disabled" : ""}`)}
            ${button("Abrir selección", 'class="open-test" data-action="open"')}
            ${button("Cartilla siguiente →", `data-move-group="1" ${selectedGroup >= list.length - 1 ? "disabled" : ""}`)}
          </div>
        </nav>
        <section class="category-grid">
          ${list.map((group, index) => `
            <button class="category-card ${selectedGroup === index ? "active" : ""}" data-group="${index}">
              <span class="category-index">${String(index + 1).padStart(2, "0")}</span>
              <strong>${esc(group.label)}</strong>
              <span>${esc(DESCRIPTIONS[group.label] || "Material visual complementario")}</span>
              <small>${visibleImages(group).length} imágenes disponibles</small>
            </button>`).join("")}
        </section>
        <footer class="library-footer">
          <span><kbd>↑</kbd><kbd>↓</kbd> Distancia</span>
          <span><kbd>←</kbd><kbd>→</kbd> Cartilla</span>
          <span><kbd>OK</kbd> Abrir</span><span>Contenido guardado en el dispositivo</span>
        </footer>
      </main>`;
    bindLibrary();
  }

  function bindLibrary() {
    app.querySelector('[data-action="home"]').onclick = () => { view = "home"; render(); };
    const calibrate = app.querySelector('[data-action="calibrate"]');
    if (calibrate) calibrate.onclick = () => { view = "calibration"; render(); };
    app.querySelectorAll("[data-distance]").forEach((element) => {
      element.onclick = () => { distance = Number(element.dataset.distance); selectedGroup = 0; render(); };
    });
    app.querySelectorAll("[data-move-distance]").forEach((element) => {
      element.onclick = () => moveDistance(Number(element.dataset.moveDistance));
    });
    app.querySelectorAll("[data-move-group]").forEach((element) => {
      element.onclick = () => { selectedGroup += Number(element.dataset.moveGroup); render(); };
    });
    app.querySelector('[data-action="open"]').onclick = openSelectedGroup;
    app.querySelectorAll("[data-group]").forEach((element) => {
      element.onclick = () => { selectedGroup = Number(element.dataset.group); openSelectedGroup(); };
    });
  }

  function moveDistance(step) {
    const position = Math.max(0, Math.min(DISTANCES.length - 1, DISTANCES.indexOf(distance) + step));
    distance = DISTANCES[position];
    selectedGroup = 0;
    render();
  }

  function openSelectedGroup() {
    imageIndex = 0;
    view = "asset";
    showEntryHud();
    render();
  }

  function renderCalibration() {
    const physical = physicalScreenSize(screenInches);
    if (calibrationStage === 0) {
      app.innerHTML = `
        <main class="display-setup-shell">
          ${button("← Volver", 'class="floating-back" data-action="back"')}
          <section class="display-setup-copy">
            <p class="section-kicker">Paso 1 de 2 · Perfil de pantalla</p>
            <h1>Indique el tamaño real del televisor.</h1>
            <p>Ingrese la diagonal en pulgadas. La aplicación detecta la resolución visible y calcula el ancho y alto físicos para adaptar la interfaz y aproximar la escala inicial.</p>
          </section>
          <section class="display-settings-card">
            <div class="calibration-option ${calibrationFocus === 0 ? "active" : ""}">
              <div><span>Diagonal de la pantalla</span><strong>${screenInches.toFixed(1)} pulgadas</strong></div>
              <div class="stepper">${button("−", 'data-inch-adjust="-1"')}<input id="screen-inches" type="number" min="15" max="120" step="0.1" value="${screenInches.toFixed(1)}">${button("+", 'data-inch-adjust="1"')}</div>
            </div>
            <div class="calibration-option ${calibrationFocus === 1 ? "active" : ""}">
              <div><span>Distancia de examen</span><strong>${distance} metros</strong></div>
              <div class="stepper">${button("−", 'data-distance-adjust="-1"')}<output>${distance} m</output>${button("+", 'data-distance-adjust="1"')}</div>
            </div>
            <div class="screen-summary">
              <span>Resolución visible</span><strong>${window.innerWidth} × ${window.innerHeight} px</strong>
              <span>Dimensiones estimadas</span><strong>${physical.widthCm.toFixed(1)} × ${physical.heightCm.toFixed(1)} cm</strong>
            </div>
            ${button("Continuar a verificación de 100 mm", `class="continue-calibration ${calibrationFocus === 2 ? "active" : ""}" data-action="continue"`)}
          </section>
          <footer class="setup-hint"><kbd>↑</kbd><kbd>↓</kbd> Seleccionar <kbd>←</kbd><kbd>→</kbd> Ajustar <kbd>OK</kbd> Continuar</footer>
        </main>`;
      app.querySelector('[data-action="back"]').onclick = () => { view = "home"; render(); };
      app.querySelectorAll("[data-inch-adjust]").forEach((element) => {
        element.onclick = () => adjustScreenInches(Number(element.dataset.inchAdjust));
      });
      app.querySelectorAll("[data-distance-adjust]").forEach((element) => {
        element.onclick = () => adjustDistance(Number(element.dataset.distanceAdjust));
      });
      app.querySelector("#screen-inches").onchange = (event) => {
        screenInches = Math.max(15, Math.min(120, Number(event.target.value) || 43));
        referencePx = Math.round(estimatedPxPerMm(screenInches) * 100);
        render();
      };
      app.querySelector('[data-action="continue"]').onclick = continueCalibration;
      return;
    }

    app.innerHTML = `
      <main class="calibration-shell">
        ${button("← Pantalla", 'class="floating-back" data-action="back"')}
        <div class="calibration-copy">
          <p class="section-kicker">Paso 2 de 2 · Verificación física</p>
          <h1>Ajuste la barra hasta que mida exactamente 100 mm.</h1>
          <p>En el TV seleccione “Ajustar a pantalla”, “1:1” o “Sin overscan”. Mida entre los extremos verdes con una regla física. Las pulgadas proporcionan una aproximación; esta barra confirma la escala clínica real.</p>
          <div class="calibration-profile"><strong>${screenInches.toFixed(1)}″</strong><span>${physical.widthCm.toFixed(1)} × ${physical.heightCm.toFixed(1)} cm · ${distance} m</span></div>
        </div>
        <div class="ruler-stage">
          <div class="ruler-label">100 mm</div>
          <div class="reference-line" style="width:${referencePx}px"></div>
          <div class="ruler-value">${Math.round(referencePx)} px · pantalla ${window.innerWidth} × ${window.innerHeight}</div>
        </div>
        <div class="calibration-actions">
          ${button("−", 'data-adjust="-1"')}
          ${button("Guardar calibración", 'class="confirm" data-action="save"')}
          ${button("+", 'data-adjust="1"')}
        </div>
      </main>`;
    app.querySelector('[data-action="back"]').onclick = () => { calibrationStage = 0; calibrationFocus = 0; render(); };
    app.querySelectorAll("[data-adjust]").forEach((element) => {
      element.onclick = () => { referencePx = Math.max(40, referencePx + Number(element.dataset.adjust)); render(); };
    });
    app.querySelector('[data-action="save"]').onclick = saveCalibration;
  }

  function adjustScreenInches(step) {
    screenInches = Math.max(15, Math.min(120, Math.round((screenInches + step) * 10) / 10));
    referencePx = Math.round(estimatedPxPerMm(screenInches) * 100);
    render();
  }

  function adjustDistance(step) {
    distance = Math.max(3, Math.min(6, distance + step));
    render();
  }

  function continueCalibration() {
    referencePx = Math.max(40, Math.round(estimatedPxPerMm(screenInches) * 100));
    calibrationStage = 1;
    render();
  }

  function saveCalibration() {
    pxPerMm = referencePx / 100;
    isCalibrated = true;
    const physical = physicalScreenSize(screenInches);
    calibration = {
      version: 3,
      pxPerMm,
      screenInches,
      screenWidthCm: physical.widthCm,
      screenHeightCm: physical.heightCm,
      distanceM: distance,
      viewport: `${window.innerWidth}x${window.innerHeight}`,
      savedAt: new Date().toISOString(),
    };
    localStorage.setItem("avtv-calibration-v2", JSON.stringify(calibration));
    view = "library";
    render();
  }

  function renderAsset() {
    const group = currentGroup();
    const images = visibleImages(group);
    const image = images[imageIndex] || images[0];
    if (!image) {
      app.innerHTML = `<main class="empty-state"><p class="section-kicker">Más recursos</p><h1>Seleccione otra sección para continuar.</h1>${button("← Volver", 'data-action="back"')}</main>`;
      app.querySelector('[data-action="back"]').onclick = () => { view = "library"; render(); };
      return;
    }
    const width = window.innerWidth;
    const height = window.innerHeight;
    const baseScale = group.distance === 0 ? 1 : pxPerMm / 3.78;
    const displayScale = Math.min(baseScale, width * 0.98 / image.width, height * 0.98 / image.height);
    const cropWidth = Math.max(1, (image.width - 4) * displayScale);
    const cropHeight = Math.max(1, (image.height - 4) * displayScale);
    app.innerHTML = `
      <main class="asset-shell ${group.distance === 0 ? "resource-view" : "clinical-view"}">
        <div class="asset-hud ${showHud ? "visible" : ""}">
          <div><strong>${esc(group.label)}</strong><span>Cartilla</span></div>
          <div><strong>${group.distance === 0 ? "Recurso" : `${group.distance} m`}</strong><span>${group.distance === 0 ? "Sección" : "Distancia"}</span></div>
          <div><strong>${imageIndex + 1} / ${images.length}</strong><span>Imagen</span></div>
          ${button("← Volver", 'data-action="back"')}
          ${button("Salir", 'data-action="home"')}
        </div>
        <section class="asset-stage">
          <div class="cropped-optotype" style="width:${cropWidth}px;height:${cropHeight}px">
            <img src="${esc(localSrc(image.src))}" alt="${esc(group.label)}" style="width:${image.canvasWidth * displayScale}px;height:${image.canvasHeight * displayScale}px;transform:translate(${-(image.x + 2) * displayScale}px,${-(image.y + 2) * displayScale}px)">
          </div>
        </section>
        <div class="integrated-brand"><span>AV</span> Agudeza Visual</div>
        <div class="asset-navigation ${showHud ? "visible" : ""}">
          ${button("← Volver", 'data-action="back"')}
          ${button("← Anterior", 'data-image="-1"')}
          <span>${imageIndex + 1} de ${images.length}</span>
          ${button("Siguiente →", 'data-image="1"')}
        </div>
      </main>`;
    app.querySelectorAll('[data-action="back"]').forEach((element) => element.onclick = () => { view = "library"; render(); });
    app.querySelector('[data-action="home"]').onclick = () => { view = "home"; render(); };
    app.querySelectorAll("[data-image]").forEach((element) => {
      element.onclick = () => {
        imageIndex = (imageIndex + Number(element.dataset.image) + images.length) % images.length;
        render();
      };
    });
  }

  function renderExam() {
    if (!isCalibrated) {
      view = "calibration";
      render();
      return;
    }
    const sequence = Array.from({ length: 5 }, (_, index) => DIRECTIONS[(index * 3 + randomSeed) % DIRECTIONS.length]);
    const rowLevels = mode === "chart"
      ? [Math.max(0, level - 2), Math.max(0, level - 1), level, Math.min(LEVELS.length - 1, level + 1)]
      : [level];
    const rows = mode === "single"
      ? landolt(sequence[0], optotypeHeightMm(distance, LEVELS[level].decimal) * pxPerMm)
      : rowLevels.map((rowLevel, rowIndex) => {
          const rowHeight = optotypeHeightMm(distance, LEVELS[rowLevel].decimal) * pxPerMm;
          const count = 5;
          return `<div class="optotype-row" style="gap:${rowHeight}px">${sequence.slice(0, count).map((direction) => landolt(direction, rowHeight)).join("")}</div>`;
        }).join("");
    app.innerHTML = `
      <main class="exam-shell ${inverted ? "inverted" : ""}">
        <div class="exam-hud ${showHud ? "visible" : ""}">
          <div><strong>${LEVELS[level].label}</strong><span>${LEVELS[level].logmar.toFixed(1)} logMAR · ${LEVELS[level].decimal.toFixed(2)} decimal</span></div>
          <div><strong>${distance} m</strong><span>Distancia</span></div>
          <div><strong>${mode === "chart" ? "Cartilla" : mode === "line" ? "Línea" : "Individual"}</strong><span>Presentación</span></div>
          ${button("Contraste", 'data-action="contrast"')}${button("Salir", 'data-action="home"')}
        </div>
        <section class="optotype-stage">${rows}</section>
        <div class="control-hint ${showHud ? "visible" : ""}">
          <span>↑ mayor</span><span>↓ menor</span><span>← → aleatorizar</span><span>OK cambiar vista</span>
        </div>
      </main>`;
    app.querySelector('[data-action="contrast"]').onclick = () => { inverted = !inverted; render(); };
    app.querySelector('[data-action="home"]').onclick = () => { view = "home"; render(); };
  }

  function render() {
    if (view === "home") renderHome();
    else if (view === "library") renderLibrary();
    else if (view === "calibration") renderCalibration();
    else if (view === "asset") renderAsset();
    else if (view === "exam") renderExam();
    else app.innerHTML = `<main class="empty-state"><h1>Cargando cartillas…</h1></main>`;
  }

  function normalizedKey(event) {
    const aliases = { Accept: "Enter", OK: "Enter", Select: "Enter", Spacebar: "Enter", GoBack: "Backspace", BrowserBack: "Backspace" };
    const codes = { 4: "Backspace", 13: "Enter", 23: "Enter", 37: "ArrowLeft", 38: "ArrowUp", 39: "ArrowRight", 40: "ArrowDown", 66: "Enter", 96: "Enter", 160: "Enter", 179: "Enter", 415: "Enter", 461: "Backspace", 10009: "Backspace" };
    return aliases[event.key] || codes[event.keyCode || event.which] || event.key;
  }

  window.addEventListener("keydown", (event) => {
    const key = normalizedKey(event);
    if (["ArrowLeft","ArrowRight","ArrowUp","ArrowDown","Enter","Backspace","Escape"].includes(key)) {
      event.preventDefault();
      event.stopPropagation();
    }
    if (view === "home") {
      if (key === "ArrowLeft" || key === "ArrowUp") selectedHome = Math.max(0, selectedHome - 1);
      else if (key === "ArrowRight" || key === "ArrowDown") selectedHome = Math.min(2, selectedHome + 1);
      else if (key === "Enter") return openHome(selectedHome);
      render();
      return;
    }
    if (key === "Backspace" || key === "Escape") {
      view = view === "asset" ? "library" : "home";
      render();
      return;
    }
    if (view === "library") {
      const list = distanceGroups();
      if (key === "ArrowLeft") selectedGroup = Math.max(0, selectedGroup - 1);
      else if (key === "ArrowRight") selectedGroup = Math.min(list.length - 1, selectedGroup + 1);
      else if (key === "ArrowUp") return moveDistance(-1);
      else if (key === "ArrowDown") return moveDistance(1);
      else if (key === "Enter") return openSelectedGroup();
      render();
      return;
    }
    if (view === "asset") {
      const images = visibleImages(currentGroup());
      if (key === "ArrowLeft") imageIndex = (imageIndex - 1 + images.length) % images.length;
      else if (key === "ArrowRight") imageIndex = (imageIndex + 1) % images.length;
      else if (key === "ArrowUp") imageIndex = Math.max(0, imageIndex - 5);
      else if (key === "ArrowDown") imageIndex = Math.min(images.length - 1, imageIndex + 5);
      render();
      return;
    }
    if (view === "calibration") {
      if (calibrationStage === 0) {
        if (key === "ArrowUp") calibrationFocus = Math.max(0, calibrationFocus - 1);
        else if (key === "ArrowDown") calibrationFocus = Math.min(2, calibrationFocus + 1);
        else if (key === "ArrowLeft" && calibrationFocus === 0) return adjustScreenInches(-1);
        else if (key === "ArrowRight" && calibrationFocus === 0) return adjustScreenInches(1);
        else if (key === "ArrowLeft" && calibrationFocus === 1) return adjustDistance(-1);
        else if (key === "ArrowRight" && calibrationFocus === 1) return adjustDistance(1);
        else if (key === "Enter" && calibrationFocus === 2) return continueCalibration();
      } else {
        if (key === "ArrowLeft") referencePx = Math.max(40, referencePx - 1);
        else if (key === "ArrowRight") referencePx += 1;
        else if (key === "ArrowDown") referencePx = Math.max(40, referencePx - 10);
        else if (key === "ArrowUp") referencePx += 10;
        else if (key === "Enter") return saveCalibration();
      }
      render();
      return;
    }
    if (view === "exam") {
      if (key === "ArrowUp") level = Math.max(0, level - 1);
      else if (key === "ArrowDown") level = Math.min(LEVELS.length - 1, level + 1);
      else if (key === "ArrowLeft" || key === "ArrowRight") randomSeed += 1;
      else if (key === "Enter") mode = mode === "chart" ? "line" : mode === "line" ? "single" : "chart";
      render();
    }
  }, true);

  window.addEventListener("resize", () => {
    if (view === "asset" || view === "exam") render();
  });

  function startWithManifest(data) {
    if (!Array.isArray(data) || data.length === 0) {
      throw new Error("El manifiesto local no contiene cartillas.");
    }
    groups = data;
    view = "home";
    render();
  }

  if (Array.isArray(window.OPTOTYPES_MANIFEST)) {
    startWithManifest(window.OPTOTYPES_MANIFEST);
  } else {
    fetch("optotypes/manifest.json")
      .then((response) => {
        if (!response.ok) throw new Error("No fue posible abrir el manifiesto local.");
        return response.json();
      })
      .then(startWithManifest)
      .catch((error) => {
        app.innerHTML = `<main class="empty-state"><h1>No fue posible cargar las cartillas.</h1><p>${esc(error.message)}</p></main>`;
      });
  }
})();
