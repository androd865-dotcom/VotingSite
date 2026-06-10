import removeForm from './removeForm.js';
const {default: socket} = await import ('./websocket.js');

// ==========================================
// ДЕЛЕГИРОВАНИЕ СОБЫТИЙ ДЛЯ ВСЕХ КЛИКОВ
// ==========================================
document.addEventListener('click', function(event) {
    // Закрытие формы по крестику
    if (event.target.closest('.form_close')) {
        removeForm();
        return;
    }

    // Кнопка "Выйти"
    if (event.target.closest('.header_auth__logout')) {
        logout(event);
        return;
    }

    // Кнопка "Создать голосование"
    if (event.target.closest('.header_auth__create')) {
        if (typeof makeVoteForm === 'function') {
            makeVoteForm(event);
        }
        return;
    }

    // Кнопка "Войти"
    if (event.target.closest('#login')) {
        loginForm(event);
        return;
    }

    // Кнопка "Зарегистрироваться"
    if (event.target.closest('#register')) {
        registerForm(event);
        return;
    }
});

// ==========================================
// ПРИВЯЗКА ОБРАБОТЧИКОВ ПРИ ЗАГРУЗКЕ
// ==========================================
document.addEventListener('DOMContentLoaded', function() {
    attachAuthEventListeners();
});

function attachAuthEventListeners() {
    const loginBtn = document.getElementById('login');
    const registerBtn = document.getElementById('register');

    if (loginBtn) {
        loginBtn.replaceWith(loginBtn.cloneNode(true));
        const newLoginBtn = document.getElementById('login');
        if (newLoginBtn) {
            newLoginBtn.addEventListener('click', function(event) {
                loginForm(event);
            });
        }
    }

    if (registerBtn) {
        registerBtn.replaceWith(registerBtn.cloneNode(true));
        const newRegisterBtn = document.getElementById('register');
        if (newRegisterBtn) {
            newRegisterBtn.addEventListener('click', function(event) {
                registerForm(event);
            });
        }
    }
}

// ==========================================
// ФОРМЫ
// ==========================================
function loginForm(event) {
    if (event) event.preventDefault();
    removeForm();
    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="login(event)">
            <button type="button" class="form_close">x</button>
            <div class="form_label">Логин</div>
            <input type="text" class="form_input" name="username" required>
            <div class="form_label">Пароль</div>
            <input type="password" class="form_input" name="password" required>
            <button type="submit" class="form_button">Войти</button>
        </form>
    `);
}

function registerForm(event) {
    if (event) event.preventDefault();
    removeForm();
    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="register(event)">
            <button type="button" class="form_close">x</button>
            <div class="form_label">Логин</div>
            <input type="text" class="form_input" name="username" required>
            <div class="form_label">Пароль (минимум 5 символов)</div>
            <input type="password" class="form_input" name="password" required minlength="5">
            <button type="submit" class="form_button">Зарегистрироваться</button>
        </form>
    `);
}

// ==========================================
// АВТОРИЗАЦИЯ
// ==========================================
async function login(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);
    const username = formData.get('username');
    const password = formData.get('password');

    const response = await fetch('http://localhost:3000/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({username, password})
    });

    if (response.ok) {
        // Получаем данные пользователя
        const userInfoResponse = await fetch(`http://localhost:3000/api/user/${username}`);
        const userData = await userInfoResponse.json();

        // Сохраняем базовую конфигурацию
        window.config = {
            authorized: true,
            username: username,
            isAdmin: username === 'admin123',
            votedTopics: userData.votedTopics || [],
            userId: userData.id
        };

        console.log('✅ Логин успешен:', username);
        console.log('window.config:', window.config);

        // Функция для отправки auth в WebSocket
        const sendAuth = () => {
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    action: 'auth',
                    login: username,
                    password: password
                }));
                console.log('🔐 WebSocket auth отправлен для:', username);
            } else {
                console.log('⏳ Ожидание открытия WebSocket...');
                socket.addEventListener('open', () => {
                    socket.send(JSON.stringify({
                        action: 'auth',
                        login: username,
                        password: password
                    }));
                }, { once: true });
            }
        };

        // Отправляем auth в WebSocket
        sendAuth();

        // Обновляем UI
        updateHeaderAfterLogin(username);

        // Закрываем форму
        removeForm();

    } else {
        const error = await response.json();
        alert(error.error || 'Неверный логин или пароль!');
    }
}

// ==========================================
// РЕГИСТРАЦИЯ
// ==========================================
async function register(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const username = formData.get('username');
    const password = formData.get('password');

    if (password.length < 5) {
        alert("Этот пароль слишком короткий!");
        return;
    }

    const response = await fetch('http://localhost:3000/api/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    });

    if (response.ok) {
        removeForm();
        // Автоматический логин после регистрации
        await autoLogin(username, password);
    } else {
        const error = await response.json();
        alert(error.error || 'Пользователь с таким именем уже существует!');
        removeForm();
    }
}

// ==========================================
// АВТОМАТИЧЕСКИЙ ЛОГИН ПОСЛЕ РЕГИСТРАЦИИ
// ==========================================
async function autoLogin(username, password) {
    const response = await fetch('http://localhost:3000/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({username, password})
    });

    if (response.ok) {
        const userInfoResponse = await fetch(`http://localhost:3000/api/user/${username}`);
        const userData = await userInfoResponse.json();

        window.config = {
            authorized: true,
            username: username,
            isAdmin: username === 'admin123',
            votedTopics: userData.votedTopics || [],
            userId: userData.id
        };

        console.log('✅ Автоматический логин после регистрации:', username);

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                action: 'auth',
                login: username,
                password: password
            }));
        } else {
            socket.addEventListener('open', () => {
                socket.send(JSON.stringify({
                    action: 'auth',
                    login: username,
                    password: password
                }));
            }, { once: true });
        }

        updateHeaderAfterLogin(username);
        alert('Регистрация успешна! Вы автоматически вошли в систему.');
    } else {
        const error = await response.json();
        alert('Регистрация прошла успешно, но не удалось войти: ' + (error.error || 'Неизвестная ошибка'));
    }
}


// Обновление хедера при логине
function updateHeaderAfterLogin(username) {
    if (username === 'admin123') {
        document.querySelector('.header_auth').innerHTML = `
            <li class="header_auth__item">
                <button class="header_auth__button header_auth__create">Создать голосование</button>
            </li>
            <li class="header_auth__item header_auth__user">
                <span class="header_auth__username">👑 ${username}</span>
            </li>
            <li class="header_auth__item">
                <button class="header_auth__button header_auth__logout">Выйти</button>
            </li>
        `;
    } else {
        document.querySelector('.header_auth').innerHTML = `
            <li class="header_auth__item header_auth__user">
                <span class="header_auth__username">👤 ${username}</span>
            </li>
            <li class="header_auth__item">
                <button class="header_auth__button header_auth__logout">Выйти</button>
            </li>
        `;
    }
}

// ==========================================
// ВЫХОД ИЗ СИСТЕМЫ
// ==========================================
function logout(event) {
    if (event) event.preventDefault();

    // Очищаем конфигурацию
    window.config = {
        authorized: false,
        username: null,
        isAdmin: false,
        votedTopics: [],
        userId: null
    };

    console.log('👋 Пользователь вышел из системы');

    // Очищаем список голосований
    const votelist = document.querySelector('.votelist');
    if (votelist) {
        votelist.innerHTML = '';
    }

    // Запрашиваем голосования как гость
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send('Привет, сервер!');
    }

    // Обновляем UI
    document.querySelector('.header_auth').innerHTML = `
        <li>
            <button class="header_auth__button" id="login">Войти</button>
        </li>
        <li>
            <button class="header_auth__button" id="register">Зарегистрироваться</button>
        </li>
    `;

    // Заново вешаем обработчики
    attachAuthEventListeners();
}

// ==========================================
// ГЛОБАЛЬНЫЕ ФУНКЦИИ
// ==========================================
window.login = login;
window.register = register;
window.logout = logout;
window.loginForm = loginForm;
window.registerForm = registerForm;
window.updateHeaderAfterLogin = updateHeaderAfterLogin;
window.attachAuthEventListeners = attachAuthEventListeners;