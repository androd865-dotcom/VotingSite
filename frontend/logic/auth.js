import makeVoteForm from './index.js'

async function login(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const username = formData.get('username');
    const password = formData.get('password');

    console.log('Username:', username);
    console.log('Password:', password)

    const response = await fetch('http://localhost:3000/api/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
    if (response.ok) {
        if (username === 'admin123') document.querySelector('.header_auth').innerHTML = `
            <button class="header_auth__button" onclick="makeVoteForm()">Создать голосование</button>
        `
        else document.querySelector('.header_auth').innerHTML = username;
        const oldForm = document.querySelector('.form');
        if (oldForm) {
            oldForm.remove();
        }
    } else console.log('Неверный логин или пароль!')
}

async function register(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const username = formData.get('username');
    const password = formData.get('password');

    console.log('Username:', username);
    console.log('Password:', password)

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

function removeForm() {
    const oldForm = document.querySelector('.form');
    if (oldForm) {
        oldForm.remove();
    }
}