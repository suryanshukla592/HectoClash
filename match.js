const textViewTimer = document.getElementById('textViewTimer');
    const textViewPuzzle = document.getElementById('textViewPuzzle');
    const textViewExpression = document.getElementById('textViewExpression');
    const buttonSubmit = document.getElementById('buttonSubmit');
    const textViewFeedback = document.getElementById('textViewFeedback');
    const gridNumbers = document.getElementById('gridNumbers');
    const gridOperators = document.getElementById('gridOperators');

    let puzzle = '';
    let currentExpression = '';
    let nextNumberIndex = 0;
    let timer;

    function generatePuzzle() {
      puzzle = Array.from({ length: 6 }, () => Math.floor(Math.random() * 9) + 1).join('');
      textViewPuzzle.textContent = `Solve: ${puzzle} = 100`;
    }

    function startTimer() {
      let timeLeft = 120;
      textViewTimer.style.color = '#D49337';
      timer = setInterval(() => {
        textViewTimer.textContent = `Time Left: ${timeLeft}s`;
        if (timeLeft <= 30) textViewTimer.style.color = '#FF5555';
        if (timeLeft <= 0) {
          clearInterval(timer);
          textViewTimer.textContent = "Time's Up!";
          buttonSubmit.disabled = true;
        }
        timeLeft--;
      }, 1000);
    }

    function setupButtons() {
      gridNumbers.innerHTML = '';
      gridOperators.innerHTML = '';
      currentExpression = '';
      nextNumberIndex = 0;
      textViewExpression.textContent = 'Your Answer';

      [...puzzle].forEach((num, index) => {
        const btn = document.createElement('button');
        btn.textContent = num;
        btn.disabled = index !== 0;
        btn.onclick = () => {
          currentExpression += num;
          textViewExpression.textContent = currentExpression;
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
        const btn = document.createElement('button');
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
    function enableMinus() {
      const operatorButtons = document.querySelectorAll('#gridOperators button');
      operatorButtons.forEach(button => {
        if (button.textContent.trim() === '-') {
          button.disabled = false;
        }
      });
    }
    

    function handleOperator(op) {
      if (op === "(") {
        enableMinus();
      }
      
      if (op === '⬅️') {
        if (currentExpression.length > 0) {
          const last = currentExpression.slice(-1);
          currentExpression = currentExpression.slice(0, -1);
          textViewExpression.textContent = currentExpression;
          if (!isNaN(last)) {
            nextNumberIndex--;
            gridNumbers.children[nextNumberIndex].disabled = false;
            if (nextNumberIndex + 1 < puzzle.length) {
              gridNumbers.children[nextNumberIndex + 1].disabled = true;
            }
          }
        }
      } else if (op === '❌') {
        setupButtons();
      } else {
        currentExpression += op;
        textViewExpression.textContent = currentExpression;
        enableOperators(false);
      }
    }

    function evaluateExpression(expr) {
      try {
        const digitCount = [...expr].filter(ch => !isNaN(ch) && ch !== ' ').length;
        if (digitCount !== 6) return NaN;
        return Function(`"use strict"; return (${expr})`)();
      } catch (e) {
        return NaN;
      }
    }

    buttonSubmit.onclick = () => {
      const result = evaluateExpression(currentExpression);
      if (result === 100) {
        textViewFeedback.textContent = '🎉 Correct! You Won!';
        textViewFeedback.style.color = '#4CAF50';
        buttonSubmit.disabled = true;
        clearInterval(timer);
      } else {
        textViewFeedback.textContent = '❌ Wrong! Try Again.';
        textViewFeedback.style.color = '#F44336';
      }
    };

    generatePuzzle();
    setupButtons();
    startTimer();