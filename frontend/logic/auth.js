import makeVoteForm from './index.js';
import removeForm from './removeForm.js';

async function login(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const username = formData.get('username');
    const password = formData.get('password');

    const response = await fetch('http://localhost:3000/api/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
    if (response.ok) {
        if (username === 'admin123') {
            document.querySelector('.header_auth').innerHTML = `
            <button class="header_auth__button" onclick="makeVoteForm(event)">Создать голосование</button>
            `
            document.querySelectorAll('.votelist_dropdown__label').forEach((label) => {
                label.innerHTML = label.innerHTML
            })
        }
        else document.querySelector('.header_auth').innerHTML = username;
        removeForm();
    } else console.log('Неверный логин или пароль!')
}

async function register(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const username = formData.get('username');
    const password = formData.get('password');

    if (password.length < 5) {console.log("Этот пароль слишком короткий!"); return;}
    const response = await fetch('http://localhost:3000/api/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
    if (response.ok) {
        document.querySelector('.header_auth').innerHTML = username;

    } else console.log('Пользователь с таким именем уже существует!');
    removeForm();

}

async function isAuthorized() {
    return await fetch('http://localhost:3000/api/isAuthorized', {
        method: 'GET'
    })
}

async function logout(event) {
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