const municipalitySelect = document.getElementById("municipality");
const timeslotSelect = document.getElementById("timeslot");
const resultDiv = document.getElementById("statusResult");
const bookingSuccess = document.getElementById("bookingSuccess");
const bookingError = document.getElementById("bookingError");
const cancelMessage = document.getElementById("cancelMessage");

// === Preencher municípios ===
fetch("/api/municipios")
  .then(r => r.json())
  .then(data => {
    data.forEach(m => {
      const opt = document.createElement("option");
      opt.value = m;
      opt.textContent = m;
      municipalitySelect.appendChild(opt);
    });
  })
  .catch(err => {
    bookingError.textContent = "Erro ao carregar municípios: " + err.message;
  });

// === Preencher timeslots ===
const timeslots = ["09:00-11:00","11:00-13:00","13:00-15:00","15:00-17:00","17:00-19:00"];
timeslots.forEach(slot => {
  const opt = document.createElement("option");
  opt.value = slot;
  opt.textContent = slot;
  timeslotSelect.appendChild(opt);
});

// === Submeter novo pedido ===
document.getElementById("bookingForm").addEventListener("submit", e => {
  e.preventDefault();
  bookingSuccess.textContent = "";
  bookingError.textContent = "";

  const booking = {
    municipality: municipalitySelect.value,
    description: document.getElementById("description").value,
    requestedDate: document.getElementById("requestedDate").value,
    timeSlot: timeslotSelect.value
  };

  fetch("/api/bookings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(booking)
  })
  .then(resp => {
    if (!resp.ok) return resp.json().then(err => { throw new Error(err.error); });
    return resp.json();
  })
  .then(data => {
    bookingSuccess.textContent = "Pedido criado com sucesso! Guarde o seu código: " + data.token;
  })
  .catch(err => {
    bookingError.textContent = "Erro: " + err.message;
  });
});

// === Ver pedido por token ===
document.getElementById("checkBtn").addEventListener("click", async () => {
  const token = document.getElementById("tokenInput").value.trim();
  resultDiv.innerHTML = "";
  cancelMessage.textContent = "";
  if (!token) {
    bookingError.textContent = "Introduza um código de pedido!";
    return;
  }

  try {
    const resp = await fetch(`/api/bookings/${token}`);
    if (!resp.ok) throw new Error("Pedido não encontrado");
    const b = await resp.json();
    resultDiv.innerHTML = `
      <p><strong>Município:</strong> ${b.municipality}</p>
      <p><strong>Descrição:</strong> ${b.description}</p>
      <p><strong>Data:</strong> ${b.requestedDate}</p>
      <p><strong>Horário:</strong> ${b.timeSlot}</p>
      <p><strong>Estado:</strong> ${b.status}</p>
      ${b.status !== 'CANCELADO' ? `<button onclick="cancelBooking('${b.token}')">Cancelar Pedido</button>` : ''}
    `;
  } catch (err) {
    bookingError.textContent = err.message;
  }
});

// === Cancelar pedido ===
async function cancelBooking(token) {
  cancelMessage.textContent = "";
  if (!confirm("Tem a certeza que quer cancelar este pedido?")) return;
  
  const resp = await fetch(`/api/bookings/${token}`, { method: "DELETE" });
  if (resp.ok) {
    cancelMessage.textContent = "Pedido cancelado com sucesso.";
    document.getElementById("checkBtn").click();
  } else {
    cancelMessage.textContent = "Erro ao cancelar pedido.";
  }
}