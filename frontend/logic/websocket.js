export function renderVote(data) {
    let variants = ''
    const groupName = `vote_${data.id}`; // Уникальное имя группы

    for (let i = 0; i < data.variants.length; i++) {
        if (data.many) {
            variants += `
                <li class="votelist_content__list">
                    <input type="checkbox" 
                           class="votelist_content__checkbox" 
                           id="votelist_content__checkbox${data.id}_${data.variants[i].id}"
                           name="${groupName}"
                           value="${data.variants[i].id}"/>
                    <label class="votelist_content__checkboxDescription" 
                           for="votelist_content__checkbox${data.id}_${data.variants[i].id}">
                        ${escapeHtml(data.variants[i].name)}
                    </label>
                </li>
            `;
        } else {
            variants += `
                <li class="votelist_content__list">
                    <input type="radio" 
                           class="votelist_content__checkbox" 
                           id="votelist_content__checkbox${data.id}_${data.variants[i].id}" 
                           name="${groupName}"
                           value="${data.variants[i].id}"/>
                    <label class="votelist_content__checkboxDescription" 
                           for="votelist_content__checkbox${data.id}_${data.variants[i].id}">
                        ${escapeHtml(data.variants[i].name)}
                    </label>
                </li>
            `;
        }
    }

    // Проверяем, голосовал ли пользователь
    const hasVoted = window.config?.authorized && window.config.votedTopics?.includes(data.id);
    const isAdmin = window.config?.isAdmin || false;

    const voteHtml = `
        <li class="votelist_dropdown" data-vote-id="${data.id}">
            <input type="checkbox" id="${data.id}" class="votelist_dropdown__checkbox">
            <label for="${data.id}" class="votelist_dropdown__label">
                <div class="votelist_label__header">${escapeHtml(data.header)}</div>
                ${isAdmin ? `
                    <div class="votelist_label__admin">
                        <button type="button" onclick="editVote(event)">
                            <img src="../assets/edit.ico" alt="Иконка редактирования" class="votelist_admin__edit">
                        </button>
                        <button type="button" class="votelist_admin__delete" onclick="deleteVote(event)">x</button>
                    </div>
                ` : ''}
            </label>
            <ul class="votelist_dropdown__content">
                ${variants}
                <li>
                    <button class="votelist_content__vote" ${hasVoted ? 'disabled' : ''}>
                        ${hasVoted ? 'Вы уже проголосовали' : 'Проголосовать'}
                    </button>
                </li>
            </ul>
        </li>
    `;

    const existingVote = document.querySelector(`.votelist_dropdown input[id="${data.id}"]`)?.closest('.votelist_dropdown');

    if (existingVote) {
        // Временно скрываем для пересчета стилей
        existingVote.style.opacity = '0';
        existingVote.outerHTML = voteHtml;

        // Принудительно пересчитываем стили для нового элемента
        requestAnimationFrame(() => {
            const newElement = document.querySelector(`.votelist_dropdown[data-vote-id="${data.id}"]`);
            if (newElement) {
                newElement.style.opacity = '1';
                // Принудительный reflow
                newElement.offsetHeight;
            }
        });
    } else {
        document.querySelector('.votelist').insertAdjacentHTML('beforeend', voteHtml);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

const socket = new WebSocket('ws://localhost:8000')
const votelist = document.querySelector('.votelist');

socket.addEventListener('open', (event) => {
    console.log('Подключение установлено!');
    socket.send('Привет, сервер!');
})

socket.addEventListener('message', (event) => {
    const data = JSON.parse(event.data);
    console.log(data);

    // 1. Полный список голосований (при логине или обновлении)
    if (Array.isArray(data)) {
        const votelist = document.querySelector('.votelist');
        votelist.innerHTML = '';
        data.forEach(vote => renderVote(vote));
        // Сохраняем данные локально
        window.votesData = data;
    }
    // 2. Новое созданное голосование (один объект)
    else if (data.id && data.header) {
        renderVote(data);
        // Добавляем в локальные данные
        if (window.votesData) {
            window.votesData.push(data);
        }
    }
    // 3. Успешное голосование
    else if (data.type === 'vote_success') {
        if (window.config?.authorized && data.topicId) {
            if (!window.config.votedTopics.includes(data.topicId)) {
                window.config.votedTopics.push(data.topicId);
            }

            // Обновляем UI только для этой темы
            const voteElement = document.querySelector(`.votelist_dropdown[data-vote-id="${data.topicId}"]`);
            if (voteElement) {
                const voteButton = voteElement.querySelector('.votelist_content__vote');
                if (voteButton) {
                    voteButton.disabled = true;
                    voteButton.textContent = 'Вы уже проголосовали';
                }
            }

            alert(data.message || 'Голос учтен!');
        }
    }
});

socket.addEventListener('close', (event) => {
    if (event.wasClean) 
        console.log('Соединение закрыто чисто!');
    else 
        console.log('Соединение сброшено');
})

votelist.addEventListener('click', (event) => {
    const button = event.target.closest('.votelist_content__vote');
    if (!button) return;

    event.preventDefault();

    if (button.disabled) {
        alert('Вы уже голосовали в этом опросе!');
        return;
    }

    const dropdown = button.closest('.votelist_dropdown');
    const voteId = Number(dropdown.querySelector('.votelist_dropdown__checkbox').id);
    const checkedInputs = dropdown.querySelectorAll('.votelist_content__checkbox:checked');

    const votes = Array.from(checkedInputs).map(input => {
        return Number(input.id.replace('votelist_content__checkbox', ''));
    });

    if (!votes.length) {
        alert('Выберите хотя бы один вариант!');
        return;
    }

    if (window.config?.authorized && window.config.votedTopics?.includes(voteId)) {
        alert('Вы уже голосовали в этом опросе!');
        return;
    }

    if (window.config?.authorized) {
        socket.send(JSON.stringify({
            name: name,
            id: voteId,
            votes: votes
        }));
        button.disabled = true;
        button.innerHTML = "Вы уже проголосовали"
    } else {
        alert('Необходимо авторизоваться!');
    }
});

window.socket = socket;
export default socket;
