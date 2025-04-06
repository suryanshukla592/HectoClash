import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
    import { getFirestore, collection, getDocs, query, orderBy } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
    import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";

    // Firebase config
    const firebaseConfig = {
      apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
      authDomain: "hectoclash-e0c38.firebaseapp.com",
      databaseURL: "https://hectoclash-e0c38-default-rtdb.firebaseio.com",
      projectId: "hectoclash-e0c38",
      storageBucket: "hectoclash-e0c38.appspot.com", // FIXED typo here
      messagingSenderId: "590113172954",
      appId: "1:590113172954:web:5a969945a9aa9283c424e3",
      measurementId: "G-3ZZHRLSZK4"
    };

    // Initialize Firebase services
    const app = initializeApp(firebaseConfig);
    const db = getFirestore(app);
    const auth = getAuth(app);

    const roomListDiv = document.getElementById('roomList');
    const spectatorView = document.getElementById('spectatorView');

    const puzzleText = document.getElementById('puzzleText');
    const player1Name = document.getElementById('player1Name');
    const player2Name = document.getElementById('player2Name');
    const player1Expression = document.getElementById('player1Expression');
    const player2Expression = document.getElementById('player2Expression');
    const player1Feedback = document.getElementById('player1Feedback');
    const player2Feedback = document.getElementById('player2Feedback');

    let player1UID = '';
    let player2UID = '';
    let ws;
    let selectedRoomId = '';
    let isConnected = false;
    const pingInterval = 30000;

    function connectWebSocket() {
      const uid = localStorage.getItem('uid') || 'guest';
      ws = new WebSocket(`ws://3.111.203.229:8080/ws?uid=${uid}`);

      ws.onopen = () => {
        console.log('[WS] Connected');
        isConnected = true;
        ws.send(JSON.stringify({ type: 'requestRoomList', uid }));
        startPing();
      };

      //       async function getNameFromUID(uid) {
      //   try {
      //     const doc = await db.collection("users").doc(uid).get();
      //     if (doc.exists && doc.data().name) {
      //       return doc.data().name;
      //     }
      //   } catch (e) {
      //     console.error("Error getting user data:", e);
      //   }
      //   return uid; // fallback to UID
      // }
      async function getNameFromUID(uid) {
        try {
          const docRef = collection(db, "users");
          const snapshot = await getDocs(query(docRef));
          for (const doc of snapshot.docs) {
            if (doc.id === uid && doc.data().name) {
              return doc.data().name;
            }
          }
        } catch (e) {
          console.error("Error getting user data:", e);
        }
        return uid;
      }
      

      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        switch (message.type) {
          case 'roomList':
            console.log('[WS] Received room list:', message.content);
            renderRoomList(message.content);
            break;


          case 'puzzle':
            puzzleText.textContent = 'Puzzle: ' + message.content;
            break;

          case 'playerMeta':
            if (message.opponent === 'Player1') {
              player1UID = message.player;
              getNameFromUID(message.player).then(name => {
                player1Name.textContent = 'Player 1: ' + name;
              });
            } else if (message.opponent === 'Player2') {
              player2UID = message.player;
              getNameFromUID(message.player).then(name => {
                player2Name.textContent = 'Player 2: ' + name;
              });
            }
            break;

          case 'expressionUpdate':
            while (message.expression !== "Your Answer") {
              if (message.opponent === player1UID || message.opponent === 'Player1') {
                player1Expression.textContent = message.expression;
                console.log(message.expression)
              } else if (message.opponent === player2UID || message.opponent === 'Player2') {
                player2Expression.textContent = message.expression;
              }
            }
            break;

          case 'feedbackUpdate':
            if (message.uid === player1UID) {
              player1Feedback.textContent = message.feedback;
            } else if (message.uid === player2UID) {
              player2Feedback.textContent = message.feedback;
            }
            break;

          default:
            console.log('[WS] Unknown message type:', message.type);
        }
      };

      

     
    }

    function renderRoomList(rooms) {
      roomListDiv.innerHTML = '<h2>Available Rooms</h2>';
      for (const [roomId, players] of Object.entries(rooms)) {
        const div = document.createElement('div');
        div.className = 'room-item';
        div.textContent = `${players.Player1} vs ${players.Player2}`;
        div.onclick = () => spectateRoom(roomId);
        roomListDiv.appendChild(div);
      }
    }

    function spectateRoom(roomId) {
      selectedRoomId = roomId;
      roomListDiv.classList.add('hidden');
      spectatorView.classList.remove('hidden');
      const uid = localStorage.getItem('uid') || 'guest';
    
      connectWebSocket(); // WebSocket will connect asynchronously
    
      // Wait for WebSocket to open
      ws.onopen = () => {
        console.log('[WS] Connected (from spectateRoom)');
        isConnected = true;
        startPing();
        ws.send(JSON.stringify({
          type: 'spectateRoom',
          room_id: roomId,
          uid: uid,
        }));
      };
    }
    

    function startPing() {
      setInterval(() => {
        if (isConnected && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'ping' }))
        }
      }, pingInterval);
    }

    function stopPing() {
      clearInterval(startPing);
    }

    window.onload = connectWebSocket;