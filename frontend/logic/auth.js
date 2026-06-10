import makeVoteForm, {deleteVote, editVote} from './index.js';
import removeForm from './removeForm.js';

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
        if (username === 'admin123') {
            document.querySelector('.header_auth').innerHTML = `
                <button class="header_auth__button" onclick="makeVoteForm(event)">Создать голосование</button>
            `;
        } else {
            document.querySelector('.header_auth').innerHTML = username;
        }

        // Закрываем форму
        removeForm();



    } else {
        const error = await response.json();
        alert(error.error || 'Неверный логин или пароль!');
    }
}

async function refreshAllVotes() {
    const response = await fetch('http://localhost:3000/api/votes');
    const votes = await response.json();

    const votelist = document.querySelector('.votelist');
    votelist.innerHTML = '';
    votes.forEach(vote => renderVote(vote));
}

async function register(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const username = formData.get('username');
    const password = formData.get('password');


    if (password.length < 5) {alert("Этот пароль слишком короткий!"); return;}
    const response = await fetch('http://localhost:3000/api/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
    if (response.ok) {
        document.querySelector('.header_auth').innerHTML = username;

    } else alert('Пользователь с таким именем уже существует!');
    removeForm();

}

function logout(event) {
    event.preventDefault();
    document.querySelector('.header_auth').innerHTML = `
        <li>
            <button class="header_auth__button" id="login">Войти</button>
        </li>
        <li>
            <button class="header_auth__button" id="register">Зарегистрироваться</button>
        </li>
    `;
}

document.querySelector('#login').addEventListener('click', () => {
    removeForm();
    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="login(event)">
            <button type="button" class="form_close" onclick="removeForm()">x</button>
            <label for="username" class="form_label">Имя пользователя</label>
            <input type="text" id="username" class="form_input" name="username" required>
            
            <label for="password" class="form_label">Пароль</label>
            <input type="password" id="password" class="form_input" name="password" required>
            
            <button type="submit" class="form_button">Войти</button>
        </form>
    `);
});

document.querySelector('#register').addEventListener('click', () => {

    removeForm();

    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="register(event)">
            <button type="button" class="form_close" onclick="removeForm()">x</button>

            <label for="username" class="form_label">Имя пользователя</label>
            <input type="text" id="username" class="form_input" name="username" required>
            
            <label for="password" class="form_label">Пароль</label>
            <input type="password" id="password" class="form_input" name="password" required>
            
            <button type="submit" class="form_button">Зарегистрироваться</button>
        </form>
    `);
});



window.login = login;
window.register = register;
window.logout = logout;
window.removeForm = removeForm;