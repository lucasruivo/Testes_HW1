const loadBtn = document.getElementById("loadBtn");
const tableBody = document.querySelector("#bookingsTable tbody");

loadBtn.addEventListener("click", async () => {
  const municipality = document.getElementById("municipalityInput").value.trim();
  const url = municipality
    ? `/api/bookings?municipality=${encodeURIComponent(municipality)}`
    : `/api/bookings`;

  try {
    const response = await fetch(url);
    const bookings = await response.json();

    tableBody.innerHTML = "";

    bookings.forEach(b => {
      const status = (b.status || "").toUpperCase();

      let actionButton = '';
      if (status === "RECEBIDO") {
        actionButton = `<button onclick="updateStatus('${b.token}', 'EM_PROG')">Em Progresso</button>`;
      } else if (status === "EM_PROG") {
        actionButton = `<button onclick="updateStatus('${b.token}', 'CONCLUIDO')">Concluído</button>`;
      } else {
        actionButton = '<em>--------------</em>';
      }

      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${b.municipality}</td>
        <td>${b.description}</td>
        <td>${b.requestedDate}</td>
        <td>${b.timeSlot || "—"}</td>
        <td>${status}</td>
        <td>${actionButton}</td>
      `;
      tableBody.appendChild(tr);
    });
  } catch (err) {
    alert("Erro ao carregar bookings: " + err.message);
  }
});

async function updateStatus(token, status) {
  try {
    const resp = await fetch(`/api/bookings/${token}?status=${status}`, { method: "PUT" });

    if (!resp.ok) {
      const errMsg = await resp.text();
      throw new Error(errMsg);
    }

    alert(`Estado atualizado para ${status}`);
    loadBtn.click();
  } catch (err) {
    alert(`Erro ao atualizar estado: ${err.message}`);
  }
}