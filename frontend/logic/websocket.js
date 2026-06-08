function renderVote(data) {
    let variants = ''
    for (let i = 0; i < data.variants.length; i++) {
        if (data.many) {
            variants = variants + `
                <li class="votelist_content__list">
                    <input type="checkbox" class="votelist_content__checkbox" id="votelist_content__checkbox${secondCounter}"/>
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${secondCounter++}">Я люблю собак</label>
                </li>
            `
        } else {
            variants = variants + `
                <li class="votelist_content__list">
                    <input type="radio" class="votelist_content__checkbox" id="votelist_content__checkbox${secondCounter}"/>
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${secondCounter++}">Я люблю собак</label>
                </li>
            `
        }
    }

    votelist.innerHTML = `${votelist.innerHTML} +   
            <li class="votelist_dropdown">
                <input type="checkbox" id="votelist_dropdown__checkbox${counter}" class="votelist_dropdown__checkbox">
                <label for="votelist_dropdown__checkbox${counter++}" class="votelist_dropdown__label">Кто из животных вам нравится?</label>
                <ul class="votelist_dropdown__content">
                    ${variants}
                </ul>
            </li>
   `;
}

const socket = new WebSocket('ws://localhost:8000/websocket')
const votelist = document.querySelector('.votelist');
-
let counter = 0;
let secondCounter = 0;



socket.addEventListener('open', (event) => {
    console.log('Подключение установлено!');
    socket.send('Привет, сервер!');


})

socket.addEventListener('message', (event) => {
    console.log('Получены данные от сервера!');
    const json = event.data;

    console.log('Данные от сервера:', json);
    const data = JSON.parse(json);

    if (Array.isArray(data))
        data.forEach(vote => renderVote(vote))

    else renderVote(data)

})

socket.addEventListener('error', (event) => {
    console.error('Ошибка WebSocket');
})

socket.addEventListener('close', (event) => {
    if (event.wasClean) 
        console.log('Соединение закрыто чисто!');
    else 
        console.log('Соединение сброшено');

    console.log(`Код: ${event.code}, причина: ${event.reason}`);
})

votelist.addEventListener('click', (event) => {
    if (event.target.closest('.votelist_content__vote')){
        console.log('Клик по кнопке');
        event.preventDefault();
    }

    console.log(event.target)
});
