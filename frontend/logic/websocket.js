const socket = new WebSocket('wss://localhost:5000')

socket.addEventListener('open', (event) => {
    console.log('Подключение установлено!');
    socket.send('Привет, сервер!');
})

socket.addEventListener('close', (event) => {
    if (event.wasClean) 
        console.log('Соединение закрыто чисто!');
    else 
        console.log('Соединение сброшено');

    console.log(`Код: ${event.code}, причина: ${event.reason}`);
})