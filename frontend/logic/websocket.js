function renderVote(data) {
    let variants = ''
    for (let i = 0; i < data.variants.length; i++) {
        if (data.many) {
            variants = variants + `
                <li class="votelist_content__list">
                    <input type="checkbox" class="votelist_content__checkbox" id="votelist_content__checkbox${data.variants[i].id}"/>
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${data.variants[i].id}">${data.variants[i].name}</label>
                </li>
            `
        } else {
            variants = variants + `
                <li class="votelist_content__list">
                    <input type="radio" class="votelist_content__checkbox" id="votelist_content__checkbox${data.variants[i].id}"/>
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${data.variants[i].id}">${data.variants[i].name}</label>
                </li>
            `
        }
    }

    votelist.innerHTML = `${votelist.innerHTML}   
            <li class="votelist_dropdown">
                <input type="checkbox" id=${data.id} class="votelist_dropdown__checkbox">
                <label for=${data.id} class="votelist_dropdown__label">${data.header}</label>
                <ul class="votelist_dropdown__content">
                    ${variants}
                    <li>
                        <button class="votelist_content__vote">Проголосовать</button>
                    </li>
                </ul>
            </li>
   `;
}

const socket = new WebSocket('ws://localhost:8000')
const votelist = document.querySelector('.votelist');

socket.addEventListener('open', (event) => {
    console.log('Подключение установлено!');
    socket.send('Привет, сервер!');
    console.log(event.data)

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
    const button = event.target.closest('.votelist_content__vote');
    if (button) {
        event.preventDefault();
        const dropdown = button.closest('.votelist_dropdown');
        const checkedInputs = dropdown.querySelectorAll('.votelist_content__checkbox:checked');

        const votes = Array.from(checkedInputs).map(input => {
            return Number(input.id.replace('votelist_content__checkbox', ''));
        })

        if (!votes.length) {
            console.log('Нельзя отправить!')
        } else {
            socket.send(JSON.stringify({
                id: dropdown.querySelector('.votelist_dropdown__checkbox').id,
                votes: votes
            }))
        }
        console.log(JSON.stringify({
            id: dropdown.querySelector('.votelist_dropdown__checkbox').id,
            votes: votes
        }));
    }
});