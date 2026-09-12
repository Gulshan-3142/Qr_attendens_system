/**
 * SmartQR Attendance Faculty Portal Client Logic
 */

let currentTeacherToken = localStorage.getItem('teacher_token') || null;
let currentTeacher = JSON.parse(localStorage.getItem('teacher_profile') || 'null');
let currentSession = null;
let countdownTimer = null;
let serverDefaultValidity = 10;
let remainingSeconds = 10;
let totalValiditySeconds = 10;
let sseConnection = null;
let currentRoster = [];

document.addEventListener('DOMContentLoaded', async () => {
  if (window.lucide) lucide.createIcons();

  await fetchServerConfig();
  initAuthView();
  setupEventListeners();
});

async function fetchServerConfig() {
  try {
    const res = await fetch('/api/config');
    const data = await res.json();
    if (data.success && data.defaultQrValiditySeconds) {
      serverDefaultValidity = data.defaultQrValiditySeconds;
      totalValiditySeconds = serverDefaultValidity;
      remainingSeconds = serverDefaultValidity;
    }
  } catch (e) {
    console.warn('Could not fetch server config:', e);
  }
}

function setupEventListeners() {
  // Login Form
  document.getElementById('loginForm').addEventListener('submit', handleLogin);

  // New Session Modal
  document.getElementById('btnNewSession').addEventListener('click', () => {
    document.getElementById('modalCreateSession').classList.remove('hidden');
    document.getElementById('modalCreateSession').classList.add('flex');
  });

  document.getElementById('btnCloseModal').addEventListener('click', () => {
    document.getElementById('modalCreateSession').classList.add('hidden');
    document.getElementById('modalCreateSession').classList.remove('flex');
  });

  // Create Session Form
  document.getElementById('createSessionForm').addEventListener('submit', handleCreateSession);

  // Manual QR Refresh
  document.getElementById('btnManualRefreshQr').addEventListener('click', refreshQrToken);

  // End Session
  document.getElementById('btnEndSession').addEventListener('click', handleEndSession);

  // Export CSV
  document.getElementById('btnExportCsv').addEventListener('click', exportAttendanceCsv);

  // Get device GPS for session creator
  document.getElementById('btnUseCurrentGps').addEventListener('click', () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          document.getElementById('sessionLat').value = pos.coords.latitude.toFixed(6);
          document.getElementById('sessionLng').value = pos.coords.longitude.toFixed(6);
          alert(`📍 GPS Coordinates Acquired:\nLat: ${pos.coords.latitude.toFixed(6)}\nLng: ${pos.coords.longitude.toFixed(6)}\nAccuracy: ±${Math.round(pos.coords.accuracy)}m`);
        },
        (err) => {
          alert('Could not obtain device GPS. Using default campus baseline.');
        },
        { enableHighAccuracy: true }
      );
    }
  });

  // Roster Search
  document.getElementById('rosterSearch').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    const filtered = currentRoster.filter(
      st => st.name.toLowerCase().includes(q) || st.rollNumber.toLowerCase().includes(q)
    );
    renderRosterTable(filtered);
  });
}

function initAuthView() {
  const authHeader = document.getElementById('authHeaderState');
  const loginView = document.getElementById('loginView');
  const dashboardView = document.getElementById('dashboardView');

  if (currentTeacherToken && currentTeacher) {
    loginView.classList.add('hidden');
    dashboardView.classList.remove('hidden');

    authHeader.innerHTML = `
      <div class="flex items-center space-x-3">
        <div class="text-right hidden sm:block">
          <p class="text-xs font-semibold text-white">${currentTeacher.name}</p>
          <p class="text-[11px] text-slate-400">${currentTeacher.email}</p>
        </div>
        <button id="btnLogout" class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700 transition-colors">
          Sign Out
        </button>
      </div>
    `;

    document.getElementById('btnLogout').addEventListener('click', handleLogout);
    document.getElementById('teacherGreeting').textContent = `Welcome, ${currentTeacher.name}`;

    loadActiveSession();
  } else {
    loginView.classList.remove('hidden');
    dashboardView.classList.add('hidden');
    authHeader.innerHTML = '';
  }

  if (window.lucide) lucide.createIcons();
}

async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('loginEmail').value;
  const password = document.getElementById('loginPassword').value;

  try {
    const res = await fetch('/api/auth/teacher/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await res.json();
    if (data.success) {
      currentTeacherToken = data.token;
      currentTeacher = data.teacher;
      localStorage.setItem('teacher_token', data.token);
      localStorage.setItem('teacher_profile', JSON.stringify(data.teacher));
      initAuthView();
    } else {
      alert(data.error || 'Login failed.');
    }
  } catch (err) {
    alert('Server connection error. Ensure backend is running.');
  }
}

function handleLogout() {
  localStorage.removeItem('teacher_token');
  localStorage.removeItem('teacher_profile');
  currentTeacherToken = null;
  currentTeacher = null;
  clearInterval(countdownTimer);
  if (sseConnection) sseConnection.close();
  initAuthView();
}

async function loadActiveSession() {
  try {
    const res = await fetch('/api/teacher/sessions/active', {
      headers: { 'Authorization': `Bearer ${currentTeacherToken}` }
    });
    const data = await res.json();

    if (data.success && data.session) {
      renderSession(data.session);
    } else {
      // Create active session if none exists
      createDefaultActiveSession();
    }
  } catch (err) {
    console.error('Failed to load active session:', err);
  }
}

async function createDefaultActiveSession() {
  try {
    const res = await fetch('/api/teacher/sessions/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentTeacherToken}`
      },
      body: JSON.stringify({
        subjectId: 'sub_dsa',
        roomName: 'LH-301 (CSE Wing)',
        latitude: 28.4660931,
        longitude: 77.5072981,
        allowedRadiusMeters: 1000,
        qrValiditySeconds: serverDefaultValidity
      })
    });
    const data = await res.json();
    if (data.success) {
      renderSession(data.session);
    }
  } catch (err) {
    console.error('Failed to create default session:', err);
  }
}

async function handleCreateSession(e) {
  e.preventDefault();
  const subjectId = document.getElementById('sessionSubjectSelect').value;
  const roomName = document.getElementById('sessionRoomName').value;
  const latitude =  28.4660931;//parseFloat(document.getElementById('sessionLat').value);
  const longitude = 77.5072981;//parseFloat(document.getElementById('sessionLng').value);
  const allowedRadiusMeters = 1000;
  const qrValiditySeconds = parseInt(document.getElementById('sessionValiditySelect').value);

  try {
    const res = await fetch('/api/teacher/sessions/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentTeacherToken}`
      },
      body: JSON.stringify({
        subjectId,
        roomName,
        latitude,
        longitude,
        allowedRadiusMeters,
        qrValiditySeconds
      })
    });

    const data = await res.json();
    if (data.success) {
      document.getElementById('modalCreateSession').classList.add('hidden');
      document.getElementById('modalCreateSession').classList.remove('flex');
      renderSession(data.session);
    } else {
      alert(data.error || 'Failed to create session.');
    }
  } catch (err) {
    alert('Server error creating session.');
  }
}

function renderSession(session) {
  currentSession = session;
  totalValiditySeconds = session.qrValiditySeconds || serverDefaultValidity || 10;
  remainingSeconds = totalValiditySeconds;

  document.getElementById('sessionSubjectTitle').textContent = session.subject_name || 'Data Structures & Algorithms';
  document.getElementById('sessionRoomSubtitle').textContent = `${session.room_name} • GPS: ${session.latitude.toFixed(4)}° N, ${session.longitude.toFixed(4)}° E`;
  document.getElementById('sessionRadiusBadge').textContent = `Radius: ${session.allowed_radius_meters}m`;

  if (session.qrDataUrl) {
    document.getElementById('dynamicQrImage').src = session.qrDataUrl;
  }

  startQrCountdown();
  connectSseStream(session.id);
  fetchLiveAttendance(session.id);
}

function startQrCountdown() {
  clearInterval(countdownTimer);
  remainingSeconds = totalValiditySeconds;

  const secondsEl = document.getElementById('countdownSeconds');
  const barEl = document.getElementById('countdownProgressBar');

  secondsEl.textContent = remainingSeconds;
  barEl.style.width = '100%';

  countdownTimer = setInterval(async () => {
    remainingSeconds--;
    secondsEl.textContent = remainingSeconds;
    const pct = Math.max(0, (remainingSeconds / totalValiditySeconds) * 100);
    barEl.style.width = `${pct}%`;

    if (remainingSeconds <= 0) {
      await refreshQrToken();
    }
  }, 1000);
}

async function refreshQrToken() {
  if (!currentSession) return;

  try {
    const res = await fetch(`/api/teacher/sessions/${currentSession.id}/qr`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentTeacherToken}`
      },
      body: JSON.stringify({ validitySeconds: totalValiditySeconds })
    });

    const data = await res.json();
    if (data.success && data.qrDataUrl) {
      document.getElementById('dynamicQrImage').src = data.qrDataUrl;
      currentSession.qrToken = data.qrToken;
      remainingSeconds = totalValiditySeconds;
    }
  } catch (err) {
    console.error('Failed to refresh QR:', err);
  }
}

async function fetchLiveAttendance(sessionId) {
  try {
    const res = await fetch(`/api/teacher/sessions/${sessionId}/live-attendance`, {
      headers: { 'Authorization': `Bearer ${currentTeacherToken}` }
    });

    const data = await res.json();
    if (data.success) {
      document.getElementById('statTotalStudents').textContent = data.metrics.totalStudents;
      document.getElementById('statPresentStudents').textContent = data.metrics.presentCount;
      document.getElementById('statAbsentStudents').textContent = data.metrics.absentCount;
      document.getElementById('statAttendancePercent').textContent = `${data.metrics.attendancePercentage}%`;

      currentRoster = data.students || [];
      renderRosterTable(currentRoster);
    }
  } catch (err) {
    console.error('Failed to fetch live attendance:', err);
  }
}

function renderRosterTable(students) {
  const tbody = document.getElementById('rosterTableBody');
  if (!students || students.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-slate-500">No students enrolled or found.</td></tr>`;
    return;
  }

  tbody.innerHTML = students.map(st => {
    const isPresent = st.status === 'PRESENT';
    const statusBadge = isPresent
      ? `<span class="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5"></span> PRESENT
        </span>`
      : `<span class="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
          <span class="w-1.5 h-1.5 rounded-full bg-rose-400 mr-1.5"></span> ABSENT
        </span>`;

    const deviceBadge = st.isDeviceBound
      ? `<div class="flex items-center gap-1.5">
          <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-mono font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20" title="${st.deviceId}">
            🔒 ${st.deviceModel || 'Bound Phone'}
          </span>
          <button onclick="resetDeviceBinding('${st.studentId}', '${st.name}')" class="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-amber-300 text-[10px] transition-colors" title="Reset Device Binding">
            <i data-lucide="rotate-ccw" class="w-3 h-3 inline"></i>
          </button>
        </div>`
      : `<span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-mono font-medium bg-slate-800 text-slate-400">
          🔓 Unlocked
        </span>`;

    const distanceLabel = st.distanceMeters !== null
      ? `<span class="font-mono text-slate-300">${st.distanceMeters}m</span> <span class="text-emerald-400 text-[10px]">✓ in zone</span>`
      : `<span class="text-slate-500">—</span>`;

    const timeLabel = st.markedAt
      ? new Date(st.markedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : `<span class="text-slate-500">—</span>`;

    const toggleBtn = isPresent
      ? `<button onclick="toggleStudentAttendance('${st.studentId}', 'ABSENT')" class="px-2.5 py-1 rounded bg-rose-600/10 text-rose-400 hover:bg-rose-600/20 border border-rose-500/20 font-medium text-xs">Mark Absent</button>`
      : `<button onclick="toggleStudentAttendance('${st.studentId}', 'PRESENT')" class="px-2.5 py-1 rounded bg-emerald-600/10 text-emerald-400 hover:bg-emerald-600/20 border border-emerald-500/20 font-medium text-xs">Mark Present</button>`;

    const resetBtn = st.isDeviceBound
      ? `<button onclick="resetDeviceBinding('${st.studentId}', '${st.name}')" class="ml-1.5 px-2 py-1 rounded bg-amber-600/10 text-amber-400 hover:bg-amber-600/20 border border-amber-500/20 font-medium text-[11px]" title="Reset Hardware Lock">Reset Device</button>`
      : '';

    return `
      <tr class="hover:bg-slate-800/40 transition-colors">
        <td class="py-3 px-4 font-semibold text-white">${st.name}</td>
        <td class="py-3 px-4 font-mono text-slate-400">${st.rollNumber}</td>
        <td class="py-3 px-4">${statusBadge}</td>
        <td class="py-3 px-4">${deviceBadge}</td>
        <td class="py-3 px-4">${distanceLabel}</td>
        <td class="py-3 px-4">${timeLabel}</td>
        <td class="py-3 px-4 text-right flex items-center justify-end">
          ${toggleBtn}
          ${resetBtn}
        </td>
      </tr>
    `;
  }).join('');

  if (window.lucide) lucide.createIcons();
}

window.resetDeviceBinding = async function(studentId, studentName) {
  if (!confirm(`Are you sure you want to RESET the device binding for ${studentName}?\n\nThis will remove the current hardware lock so the student can register their new mobile device.`)) {
    return;
  }

  try {
    const res = await fetch(`/api/teacher/students/${studentId}/reset-device`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentTeacherToken}`
      }
    });

    const data = await res.json();
    if (data.success) {
      alert(`✅ Device lock successfully removed for ${studentName}. The student can now log in from a new device.`);
      if (currentSession) fetchLiveAttendance(currentSession.id);
    } else {
      alert(data.error || 'Failed to reset device binding.');
    }
  } catch (err) {
    alert('Server connection error during device reset.');
  }
};

window.toggleStudentAttendance = async function(studentId, status) {
  if (!currentSession) return;
  try {
    const res = await fetch(`/api/teacher/sessions/${currentSession.id}/manual-attendance`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentTeacherToken}`
      },
      body: JSON.stringify({ studentId, status })
    });
    const data = await res.json();
    if (data.success) {
      fetchLiveAttendance(currentSession.id);
    }
  } catch (err) {
    alert('Failed to update attendance.');
  }
};

function connectSseStream(sessionId) {
  if (sseConnection) sseConnection.close();

  sseConnection = new EventSource(`/api/teacher/sessions/${sessionId}/stream`);

  sseConnection.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data);
      if (data.type === 'ATTENDANCE_MARKED') {
        // Play subtle ding or refresh metrics
        fetchLiveAttendance(sessionId);
      }
    } catch (err) {
      console.error('SSE parse error:', err);
    }
  };
}

async function handleEndSession() {
  if (!currentSession || !confirm('Are you sure you want to end this attendance session?')) return;
  try {
    const res = await fetch(`/api/teacher/sessions/${currentSession.id}/close`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${currentTeacherToken}` }
    });
    const data = await res.json();
    if (data.success) {
      clearInterval(countdownTimer);
      alert('Class attendance session closed.');
      loadActiveSession();
    }
  } catch (err) {
    alert('Failed to close session.');
  }
}

function exportAttendanceCsv() {
  if (!currentRoster || currentRoster.length === 0) {
    alert('No attendance data to export.');
    return;
  }

  let csvContent = 'data:text/csv;charset=utf-8,Student Name,Roll Number,College ID,Status,Distance (Meters),Marked Time\n';
  currentRoster.forEach(st => {
    csvContent += `"${st.name}","${st.rollNumber}","${st.collegeId}","${st.status}","${st.distanceMeters || ''}","${st.markedAt || ''}"\n`;
  });

  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `Attendance_${currentSession ? currentSession.room_name : 'Report'}_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
