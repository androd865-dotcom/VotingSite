const socket = new WebSocket('ws://localhost:5000')

socket.addEventListener('open', (event) => {
    console.log('Подключение установлено!');
    socket.send('Привет, сервер!');
})

socket.addEventListener('message', (event) => {
    console.log('Получены данные от сервера!');
    const json = event.data;

    console.log('Данные от сервера:', json);
    const data = JSON.parse(json);


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