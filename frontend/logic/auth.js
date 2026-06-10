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
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
    if (response.ok) {
        if (username === 'admin123') {
            window.config = { isAdmin: true,  authorized: true};
            Object.freeze(config);
            window.isAdmin = true;
            document.querySelector('.header_auth').innerHTML = `
            <button class="header_auth__button" onclick="makeVoteForm(event)">Создать голосование</button>
            `

            document.querySelectorAll('.votelist_dropdown__label').forEach((label) => {
                label.innerHTML = `<div class="votelist_label__header">${label.innerHTML}</div>
                <div class="votelist_label__admin">
                    <button type="button">
                        <img src="../assets/edit.ico" alt="Иконка редактирования" class="votelist_admin__edit" onclick="editVote(event)">
                    </button>
                    <button type="button" class="votelist_admin__delete" onclick="deleteVote(event)">x</button>
                </div>`
            })
        }
        else {
            document.querySelector('.header_auth').innerHTML = username;
            window.config = { isAdmin: false,  authorized: true};
            Object.freeze(config);
        }
        removeForm();

    } else alert('Неверный логин или пароль!')
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