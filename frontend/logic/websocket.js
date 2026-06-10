export function renderVote(data) {
    let variants = ''
    for (let i = 0; i < data.variants.length; i++) {
        if (data.many) {
            variants = variants + `
                <li class="votelist_content__list">
                    <input type="checkbox" class="votelist_content__checkbox" id="votelist_content__checkbox${data.variants[i].id}"/>
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${data.variants[i].id}">${escapeHtml(data.variants[i].name)}</label>
                </li>
            `
        } else {
            variants = variants + `
                <li class="votelist_content__list">
                    <input type="radio" class="votelist_content__checkbox" id="votelist_content__checkbox${data.variants[i].id}" name="${data.id}" />
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${data.variants[i].id}">${escapeHtml(data.variants[i].name)}</label>
                </li>
            `
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
        existingVote.outerHTML = voteHtml;
    } else {
        document.querySelector('.votelist').insertAdjacentHTML('beforeend', voteHtml);
    }
}

export function renderResults(voteId, results) {
    const voteElement = document.querySelector(`.votelist_dropdown[data-vote-id="${voteId}"]`);
    if (!voteElement) return;

    const content = voteElement.querySelector('.votelist_dropdown__content');
    if (!content) return;

    // Находим максимальный процент для масштабирования
    const maxPercent = Math.max(...results.map(r => r.percent), 1);

    let resultsHtml = '';
    results.forEach(result => {
        const barWidth = (result.percent / maxPercent) * 100;
        resultsHtml += `
            <li class="votelist_result__item">
                <div class="votelist_result__label">${escapeHtml(result.name)}</div>
                <div class="votelist_result__barContainer">
                    <div class="votelist_result__bar" style="width: ${barWidth}%"></div>
                </div>
                <div class="votelist_result__percent">${result.percent}%</div>
            </li>
        `;
    });

    // Добавляем общее количество голосов
    const totalVotes = results.reduce((sum, r) => sum + (r.count || 0), 0);

    content.innerHTML = `
        <ul class="votelist_results">
            ${resultsHtml}
        </ul>
        <div class="votelist_results__total">
            Всего голосов: ${totalVotes}
        </div>
    `;

    // Добавляем класс для стилизации
    voteElement.classList.add('votelist_dropdown--results');
}

// Вспомогательная функция для экранирования HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

const socket = new WebSocket('ws://localhost:8000')
const votelist = document.querySelector('.votelist');

socket.addEventListener('open', (event) => {
    console.log('Подключение установлено!');
    socket.send('Привет, сервер!');
})

socket.addEventListener('message', (event) => {
    const data = JSON.parse(event.data);
    console.log('Получены данные:', data);

    // 1. Полный список голосований (при логине или обновлении)
    if (Array.isArray(data)) {
        const votelist = document.querySelector('.votelist');
        votelist.innerHTML = '';
        data.forEach(vote => {
            // Если есть статистика и пользователь голосовал - показываем результаты
            if (vote.hasVoted && vote.statistics && vote.statistics.votes) {
                // Сначала рендерим голосование
                renderVote(vote);
                // Затем заменяем на результаты
                renderResults(vote.id, vote.statistics.votes);
            } else {
                renderVote(vote);
            }
        });
        window.votesData = data;
    }
    // 2. Новое созданное голосование (один объект)
    else if (data.id && data.header && !data.type) {
        renderVote(data);
        if (window.votesData) {
            window.votesData.push(data);
        }
    }
    // 3. Результаты голосования
    else if (data.type === 'results') {
        renderResults(data.id, data.votes);

        // Обновляем кнопку
        const voteElement = document.querySelector(`.votelist_dropdown[data-vote-id="${data.id}"]`);
        if (voteElement) {
            const voteButton = voteElement.querySelector('.votelist_content__vote');
            if (voteButton) {
                voteButton.disabled = true;
                voteButton.textContent = 'Вы уже проголосовали';
            }
        }
    }
    // 4. Успешное голосование (старый формат)
    else if (data.type === 'VOTE_SUCCESS' || data.type === 'vote_success') {
        if (window.config?.authorized && data.topicId) {
            if (!window.config.votedTopics.includes(data.topicId)) {
                window.config.votedTopics.push(data.topicId);
            }

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
    // 5. Голосование обновлено админом
    else if (data.type === 'UPDATE_SUCCESS' || data.type === 'vote_updated') {
        if (window.config?.votedTopics) {
            const index = window.config.votedTopics.indexOf(Number(data.topicId || data.id));
            if (index > -1) {
                window.config.votedTopics.splice(index, 1);
            }
        }

        if (data.topic) {
            renderVote(data.topic);
        }

        if (window.votesData && data.topic) {
            const index = window.votesData.findIndex(v => v.id === data.topic.id);
            if (index > -1) {
                window.votesData[index] = data.topic;
            }
        }
    }
    // 6. Удаление голосования
    else if (data.type === 'DELETE_SUCCESS') {
        const voteElement = document.querySelector(`.votelist_dropdown input[id="${data.topicId}"]`)?.closest('.votelist_dropdown');
        if (voteElement) {
            voteElement.remove();
        }

        if (window.votesData) {
            window.votesData = window.votesData.filter(v => v.id !== data.topicId);
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
        // Отправляем голос
        socket.send(JSON.stringify({
            name: window.config.username,
            id: voteId,
            votes: votes
        }));

        // Сразу меняем кнопку
        button.disabled = true;
        button.textContent = 'Вы уже проголосовали';

        // Добавляем в votedTopics
        if (!window.config.votedTopics) {
            window.config.votedTopics = [];
        }
        if (!window.config.votedTopics.includes(voteId)) {
            window.config.votedTopics.push(voteId);
        }

        // Показываем индикатор загрузки
        const content = dropdown.querySelector('.votelist_dropdown__content');
        if (content) {
            content.innerHTML = '<div class="votelist_loading">Загрузка результатов...</div>';
        }
    } else {
        alert('Необходимо авторизоваться!');
    }
});
window.renderResults = renderResults;
window.socket = socket;
export default socket;
