import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { getFirestore, doc, getDoc } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
  authDomain: "hectoclash-e0c38.firebaseapp.com",
  projectId: "hectoclash-e0c38",
  storageBucket: "hectoclash-e0c38.appspot.com",
  messagingSenderId: "590113172954",
  appId: "1:590113172954:web:5a969945a9aa9283c424e3",
  measurementId: "G-3ZZHRLSZK4"
};

import { DotLottie } from 'https://cdn.jsdelivr.net/npm/@lottiefiles/dotlottie-web/+esm';

let dotLottie;


dotLottie = new DotLottie({
  autoplay: true,
  loop: true,
  canvas: document.getElementById("matchmakingAnimation"),
  src: "loading.json",
});


const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

let ws;
let uid = null;
let roomId = null;
let puzzle = "";
let currentExpression = "";
let nextNumberIndex = 0;
let timer;
let timeLeft = 120;

const expressionDisplay = document.getElementById("expressionDisplay");
const feedback = document.getElementById("feedback");
const timerElem = document.getElementById("timer");
const gridNumbers = document.getElementById("gridNumbers");
const gridOperators = document.getElementById("gridOperators");
const submitBtn = document.getElementById("submit");

onAuthStateChanged(auth, async (user) => {
  if (user) {
    uid = user.uid;
    const userDoc = await getDoc(doc(db, "Users", uid));
    if (userDoc.exists()) {
      const data = userDoc.data();
      document.getElementById("playerName").textContent = data.Username;
      document.getElementById("playerRating").textContent = "Rating: " + (data.Rating || 0);
      document.getElementById("playerAvgTime").textContent = "⏱ : " + (data.Time || 0) + "s";
      document.getElementById("playerAccuracy").textContent = "🎯 : " + (Math.floor(data.Accuracy) || 0) + "%";
      document.getElementById("playerProfile").src = data["Profile Picture URL"] || "defaultdp.png";
      setupWebSocket();
    }
  } else {
    window.location.href = "login.html";
  }
});

function setupWebSocket() {
  ws = new WebSocket("ws://3.111.203.229:8080/ws");

  ws.onopen = () => {
    ws.send(JSON.stringify({ uid }));
  };

  ws.onmessage = async (event) => {
    const msg = JSON.parse(event.data);

    switch (msg.type) {
      case "start":
        roomId = msg.room_id;
        puzzle = msg.content;
        const opponentId = (msg.opponent === uid) ? msg.player : msg.opponent;

        if (dotLottie) {
          dotLottie.stop(); // stop animation
          document.getElementById("matchmakingAnimation").remove(); // optional cleanup
        }

        document.getElementById("puzzle").textContent = "Solve: " + puzzle + " = 100";
        document.getElementById("match").style.display = "block";
        document.getElementById("matchmaking").style.display = "none";
        startTimer();
        setupButtons();

        const opponentDoc = await getDoc(doc(db, "Users", opponentId));
        if (opponentDoc.exists()) {
          const data = opponentDoc.data();
          document.getElementById("opponentName").textContent = data.Username;
          document.getElementById("opponentRating").textContent = "Rating: " + (data.Rating || 0);
          document.getElementById("opponentAvgTime").textContent = "⏱ : " + (data.Time || 0) + "s";
          document.getElementById("opponentAccuracy").textContent = "🎯 : " + (Math.floor(data.Accuracy) || 0) + "%";
          document.getElementById("opponentProfile").src = data["Profile Picture URL"] || "defaultdp.png";
        }
        break;

      case "feedback":
        feedback.textContent = msg.content;
        submitBtn.disabled = false;
        break;

      case "result":
        feedback.textContent = msg.content;
        clearInterval(timer);
        submitBtn.disabled = true;
        break;
    }
  };

  ws.onclose = () => alert("WebSocket disconnected");
  ws.onerror = (err) => console.error("WebSocket Error:", err);
}

function startTimer() {
  timeLeft = 120;
  timer = setInterval(() => {
    timerElem.textContent = `Time Left: ${timeLeft}s`;
    timerElem.style.color = timeLeft <= 30 ? "#FF5555" : "#D49337";

    if (--timeLeft <= 0) {
      clearInterval(timer);
      timerElem.textContent = "Time's Up!";
      sendResult("Timeout");
      submitBtn.disabled = true;
    }
  }, 1000);
}

function sendResult(result) {
  const json = {
    type: "result",
    content: result,
    uid,
    room_id: roomId
  };
  ws.send(JSON.stringify(json));
}

function setupButtons() {
  gridNumbers.innerHTML = "";
  gridOperators.innerHTML = "";
  currentExpression = "";
  nextNumberIndex = 0;
  expressionDisplay.textContent = "Your Answer";

  [...puzzle].forEach((num, index) => {
    const btn = document.createElement("button");
    btn.textContent = num;
    btn.disabled = index !== 0;
    btn.onclick = () => {
      currentExpression += num;
      expressionDisplay.textContent = currentExpression;
      btn.disabled = true;
      if (nextNumberIndex + 1 < puzzle.length) {
        gridNumbers.children[nextNumberIndex + 1].disabled = false;
      }
      nextNumberIndex++;
      enableOperators(true);
    };
    gridNumbers.appendChild(btn);
  });

  const ops = ['+', '-', '*', '/', '(', ')', '^', '⬅️', '❌'];
  ops.forEach(op => {
    const btn = document.createElement("button");
    btn.textContent = op;
    btn.onclick = () => handleOperator(op);
    gridOperators.appendChild(btn);
  });

  enableOperators(false);
}

function enableOperators(state) {
  [...gridOperators.children].forEach(btn => {
    if (!['(', ')', '⬅️', '❌'].includes(btn.textContent)) {
      btn.disabled = !state;
    }
  });
}

function handleOperator(op) {
  if (op === '⬅️') {
    if (currentExpression.length > 0) {
      const last = currentExpression.slice(-1);
      currentExpression = currentExpression.slice(0, -1);
      expressionDisplay.textContent = currentExpression;
      if (!isNaN(last)) {
        nextNumberIndex--;
        gridNumbers.children[nextNumberIndex].disabled = false;
      }
    }
  } else if (op === '❌') {
    setupButtons();
  } else {
    currentExpression += op;
    expressionDisplay.textContent = currentExpression;
    enableOperators(false);
  }
}

submitBtn.addEventListener("click", () => {
  const digitsUsed = currentExpression.replace(/\D/g, "").length;
  if (digitsUsed !== 6) {
    feedback.textContent = "Use exactly 6 digits!";
    return;
  }

  try {
    const result = math.evaluate(currentExpression);
    if (isNaN(result)) throw new Error();

    const json = {
      type: "submit",
      uid,
      room_id: roomId,
      answer: result.toString(),
      expression: currentExpression
    };

    ws.send(JSON.stringify(json));
    submitBtn.disabled = true;
    feedback.textContent = "Submitting...";
  } catch {
    feedback.textContent = "Invalid Expression!";
  }
});
