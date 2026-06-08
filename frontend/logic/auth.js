
async function login(name, password) {
    const response = await fetch('http://localhost:3000/api/login', {
        method: 'POST',
        body: JSON.stringify({name, password})
    })
    if (response.ok) {
        if (name === 'admin123') document.querySelector('.header_auth').innerHTML = `<button class="header_auth__button">Создать голосование</button>`
        else document.querySelector('.header_auth').innerHTML = name;
    } else console.log('Неверный логин или пароль!')
}

async function register(name, password) {
    if (password.length < 5) {console.log("Этот пароль слишком короткий!"); return;}
    const response = await fetch('http://localhost:3000/api/register', {
        method: 'POST',
        body: JSON.stringify({name, password})
    })
    if (response.ok) {
        document.querySelector('.header_auth').innerHTML = name;
    } else console.log('Пользователь с таким именем уже существует!');

}

function logout() {
    document.querySelector('.header_auth').innerHTML = `
        <li>
            <button class="header_auth__button">Войти</button>
        </li>
        <li>
            <button class="header_auth__button">Зарегистрироваться</button>
        </li>
    `;
}

function drawAuthorized() {

}
