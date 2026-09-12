/**
 * SmartQR Attendance Student Portal & Mobile App Simulator Logic
 */

let currentStudentToken = localStorage.getItem('student_token') || null;
let currentStudent = JSON.parse(localStorage.getItem('student_profile') || 'null');
let html5QrScanner = null;

// Get or generate a persistent unique Device ID for this browser / device
let deviceId = localStorage.getItem('student_device_id');
if (!deviceId) {
  deviceId = 'web_dev_' + Math.random().toString(36).substring(2, 10) + '_' + Date.now().toString(36);
  localStorage.setItem('student_device_id', deviceId);
}
let deviceModel = navigator.userAgent.includes('Mobile') ? 'Mobile Browser' : (navigator.platform || 'Mac/PC Client');

document.addEventListener('DOMContentLoaded', () => {
  if (window.lucide) lucide.createIcons();

  updateDeviceLabel();
  initStudentView();
  setupStudentListeners();
});

function updateDeviceLabel() {
  const el = document.getElementById('labelCurrentDeviceId');
  if (el) el.textContent = deviceId;
}

function setupStudentListeners() {
  // Verification Form
  document.getElementById('studentVerifyForm').addEventListener('submit', handleStudentVerify);

  // Logout
  document.getElementById('btnStudentLogout').addEventListener('click', handleStudentLogout);

  // Switch / New Simulated Phone Button
  document.getElementById('btnSwitchDevice').addEventListener('click', () => {
    const newId = 'phone_' + Math.random().toString(36).substring(2, 7) + '_' + Date.now().toString(36).slice(-4);
    deviceId = newId;
    localStorage.setItem('student_device_id', newId);
    updateDeviceLabel();
    alert(`📱 Switched to New Simulated Phone:\n\nNew Hardware ID: ${newId}\n\nYou can now test login/registration on this new device.`);
  });

  // Open Scanner Modal
  document.getElementById('btnOpenScanner').addEventListener('click', openCameraScanner);
  document.getElementById('btnCloseScanner').addEventListener('click', closeCameraScanner);

  // Manual Token Submit
  document.getElementById('btnSubmitManualToken').addEventListener('click', () => {
    const token = document.getElementById('manualQrTokenInput').value.trim();
    if (token) {
      closeCameraScanner();
      processAttendanceScan(token);
    } else {
      alert('Please enter a valid QR token.');
    }
  });

  // Reset GPS Simulation to classroom (7.7m)
  document.getElementById('btnResetGps').addEventListener('click', () => {
    document.getElementById('simStudentLat').value = '28.613960';
    document.getElementById('simStudentLng').value = '77.209040';
    alert('📍 GPS set to classroom zone (Lat: 28.613960, Lng: 77.209040 ~ 7.7m away)');
  });
}

function initStudentView() {
  const onboardingView = document.getElementById('studentOnboardingView');
  const dashboardView = document.getElementById('studentDashboardView');

  if (currentStudentToken && currentStudent) {
    onboardingView.classList.add('hidden');
    dashboardView.classList.remove('hidden');

    document.getElementById('dashStudentName').textContent = currentStudent.name;
    document.getElementById('dashStudentMeta').textContent = `Roll: ${currentStudent.rollNumber} • ID: ${currentStudent.collegeId} • Sem ${currentStudent.semester || 6}`;

    loadStudentDashboard();
  } else {
    onboardingView.classList.remove('hidden');
    dashboardView.classList.add('hidden');
  }

  if (window.lucide) lucide.createIcons();
}

async function handleStudentVerify(e) {
  e.preventDefault();
  const name = document.getElementById('stuName').value.trim();
  const rollNumber = document.getElementById('stuRoll').value.trim();
  const collegeId = document.getElementById('stuCollegeId').value.trim();

  try {
    const res = await fetch('/api/auth/student/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name,
        rollNumber,
        collegeId,
        deviceId,
        deviceModel
      })
    });

    const data = await res.json();
    if (data.success) {
      currentStudentToken = data.token;
      currentStudent = data.student;
      localStorage.setItem('student_token', data.token);
      localStorage.setItem('student_profile', JSON.stringify(data.student));
      initStudentView();
    } else {
      if (data.code === 'DEVICE_LOCKED') {
        alert(`${data.error}\n\nBound Device: ${data.boundDeviceModel || 'Primary Device'}\nYour Current Device ID: ${deviceId}`);
      } else {
        alert(data.error || 'Student verification failed.');
      }
    }
  } catch (err) {
    alert('Server connection error. Ensure backend is running.');
  }
}

function handleStudentLogout() {
  localStorage.removeItem('student_token');
  localStorage.removeItem('student_profile');
  currentStudentToken = null;
  currentStudent = null;
  initStudentView();
}

async function loadStudentDashboard() {
  if (!currentStudent) return;

  try {
    // 1. Fetch Overall Attendance Summary
    const sumRes = await fetch(`/api/student/attendance/summary?studentId=${currentStudent.id}`, {
      headers: { 'Authorization': `Bearer ${currentStudentToken}` }
    });
    const sumData = await sumRes.json();
    if (sumData.success && sumData.summary) {
      const s = sumData.summary;
      document.getElementById('dashAttendancePct').textContent = `${s.attendancePercentage}%`;
      document.getElementById('dashAttendanceStatus').textContent = s.statusLabel.toUpperCase();
      document.getElementById('dashAttendanceStatus').className = s.attendancePercentage >= 75
        ? 'inline-block mt-1 text-[11px] font-bold text-emerald-400'
        : 'inline-block mt-1 text-[11px] font-bold text-rose-400';
      document.getElementById('dashPresentCount').textContent = s.presentClasses;
      document.getElementById('dashAbsentCount').textContent = s.absentClasses;
      document.getElementById('dashTotalCount').textContent = s.totalClasses;
      document.getElementById('dashAvgPerDay').textContent = s.averageClassesPerDay;
    }

    // 2. Fetch Subject-wise Breakdown
    const subRes = await fetch(`/api/student/attendance/subjects?studentId=${currentStudent.id}`, {
      headers: { 'Authorization': `Bearer ${currentStudentToken}` }
    });
    const subData = await subRes.json();
    if (subData.success && subData.subjects) {
      renderSubjectList(subData.subjects);
    }

    // 3. Fetch Leave Calculator
    const calcRes = await fetch('/api/student/calculators/leave', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentStudentToken}`
      },
      body: JSON.stringify({
        presentClasses: sumData.summary?.presentClasses || 33,
        totalClasses: sumData.summary?.totalClasses || 40,
        desiredPercentage: 75
      })
    });
    const calcData = await calcRes.json();
    if (calcData.success) {
      document.getElementById('calcLeaveResult').textContent = calcData.message;
      document.getElementById('calcLeaveResult').className = calcData.canTakeLeave
        ? 'p-3 bg-slate-950 rounded-xl text-xs text-emerald-400 font-semibold border border-emerald-500/20'
        : 'p-3 bg-slate-950 rounded-xl text-xs text-rose-400 font-semibold border border-rose-500/20';
    }
  } catch (err) {
    console.error('Failed to load student dashboard:', err);
  }
}

function renderSubjectList(subjects) {
  const container = document.getElementById('studentSubjectList');
  if (!subjects || subjects.length === 0) {
    container.innerHTML = '<p class="text-xs text-slate-500">No subjects found.</p>';
    return;
  }

  container.innerHTML = subjects.map(sub => {
    const isSafe = sub.attendancePercentage >= 75;
    const colorClass = isSafe ? 'text-emerald-400' : 'text-rose-400';
    const bgBarClass = isSafe ? 'bg-emerald-500' : 'bg-rose-500';

    return `
      <div class="bg-slate-900 border border-slate-800 rounded-2xl p-3.5 space-y-2">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs font-bold text-white">${sub.subjectName}</p>
            <p class="text-[10px] text-slate-400">${sub.subjectCode} • ${sub.teacherName || 'Faculty'}</p>
          </div>
          <span class="text-xs font-extrabold ${colorClass}">${sub.attendancePercentage}%</span>
        </div>
        <div class="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
          <div class="h-full ${bgBarClass}" style="width: ${Math.min(100, sub.attendancePercentage)}%"></div>
        </div>
        <div class="flex justify-between text-[10px] text-slate-400">
          <span>Present: ${sub.presentClasses} / ${sub.totalClasses}</span>
          <span class="${isSafe ? 'text-emerald-400' : 'text-rose-400'} font-medium">${isSafe ? 'Safe' : 'Alert'}</span>
        </div>
      </div>
    `;
  }).join('');
}

function openCameraScanner() {
  const modal = document.getElementById('scannerModal');
  modal.classList.remove('hidden');
  modal.classList.add('flex');

  if (window.Html5Qrcode) {
    html5QrScanner = new Html5Qrcode("qrReaderContainer");
    html5QrScanner.start(
      { facingMode: "environment" },
      { fps: 10, qrbox: 250 },
      (decodedText) => {
        closeCameraScanner();
        processAttendanceScan(decodedText);
      },
      (errorMessage) => {
        // scan frame error, ignore
      }
    ).catch(err => {
      console.warn("Camera access denied or unavailable:", err);
    });
  }
}

function closeCameraScanner() {
  const modal = document.getElementById('scannerModal');
  modal.classList.add('hidden');
  modal.classList.remove('flex');

  if (html5QrScanner) {
    html5QrScanner.stop().then(() => html5QrScanner.clear()).catch(() => {});
    html5QrScanner = null;
  }
}

async function processAttendanceScan(qrToken) {
  const stepsModal = document.getElementById('verifyStepsModal');
  stepsModal.classList.remove('hidden');
  stepsModal.classList.add('flex');

  const s1 = document.getElementById('step1');
  const s2 = document.getElementById('step2');
  const s3 = document.getElementById('step3');
  const s4 = document.getElementById('step4');
  const s5 = document.getElementById('step5');

  s1.textContent = '⏳ 1. QR Code Decoded...';
  s2.textContent = '○ 2. Student Authentication';
  s3.textContent = '○ 3. Session Active Validation';
  s4.textContent = '○ 4. GPS Geofence & Device Check';
  s5.textContent = '○ 5. Database Confirmation';

  await new Promise(r => setTimeout(r, 250));
  s1.textContent = '✓ 1. QR Code & Token Signature Validated';
  s2.textContent = '⏳ 2. Verifying Student Identity & Enrollment...';

  await new Promise(r => setTimeout(r, 250));
  s2.textContent = '✓ 2. Student Identity Verified';
  s3.textContent = '⏳ 3. Checking Active Class Session...';

  await new Promise(r => setTimeout(r, 250));
  s3.textContent = '✓ 3. Session Active';
  s4.textContent = '⏳ 4. Checking Device Lock & Geofence (<=20m)...';

  const lat = parseFloat(document.getElementById('simStudentLat').value) || 28.613960;
  const lng = parseFloat(document.getElementById('simStudentLng').value) || 77.209040;

  try {
    const res = await fetch('/api/attendance/scan', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentStudentToken}`
      },
      body: JSON.stringify({
        studentId: currentStudent.id,
        qrToken,
        latitude: lat,
        longitude: lng,
        accuracy: 6,
        isMockLocation: false,
        deviceId,
        deviceModel
      })
    });

    const data = await res.json();
    await new Promise(r => setTimeout(r, 300));
    stepsModal.classList.add('hidden');
    stepsModal.classList.remove('flex');

    if (data.success && data.attendance) {
      alert(`🎉 Attendance Marked Successfully!\n\nSubject: ${data.attendance.subjectName}\nDate: ${data.attendance.date}\nTime: ${data.attendance.time}\nDistance: ${data.attendance.distanceMeters}m (Within 20m Zone)\nStatus: Present`);
      loadStudentDashboard();
    } else {
      alert(`❌ Attendance Failed:\n\n${data.error || 'Verification failed.'}`);
    }
  } catch (err) {
    stepsModal.classList.add('hidden');
    stepsModal.classList.remove('flex');
    alert('Failed to connect to backend server.');
  }
}
