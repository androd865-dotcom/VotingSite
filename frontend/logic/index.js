import removeForm from './removeForm.js';
const {default: socket, renderVote} = await import ('./websocket.js')
async function makeVote(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const header = formData.get('voteName');

    const variants = [];
    const variantInputs = form.querySelectorAll('input[name^="variant"]');
    variantInputs.forEach(input => {
        const value = input.value.trim();
        if (value) {
            variants.push(value);
        }
    });

    const radioOne = document.querySelector('#form_radio__one');
    const many = radioOne ? !radioOne.checked : true;

    const data = {
        type: 'create',
        header: header,
        variants: variants,
        many: many
    };

    socket.send(JSON.stringify(data))
    removeForm();
}

let count = 0;

export default function makeVoteForm(event) {
    event.preventDefault();
    removeForm();

    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="makeVote(event)">
            <button type="button" class="form_close" onclick="removeForm()">x</button>
            <div class="form_label form_header">
                <div>Тема опроса</div>
                <div class="form_radioContainer">
                    <label for="form_radio__one" class="form_radio__label">Один</label>
                    <input type="radio" name="form_radio" id="form_radio__one" class="form_radio">
                    <label for="form_radio__many" class="form_radio__label">Много</label>
                    <input type="radio" name="form_radio" id="form_radio__many" class="form_radio">
                </div>
            </div>
            <input type="text" id="voteName" class="form_input" name="voteName" required>
            <div class="form_label">
                Варианты ответов
            </div>
            <ul class="form_variantsList">
                <li class="form_variantItem">
                    <input type="text" id="variant0" class="form_input" name="variant0" required>
                </li>
            </ul>
            <div class="form_bottomContainer">
                <div class="form_makeVariant">
                    <button type="button" class="form_makeVariant__plus" onclick="addVariantField(event)">+</button>
                </div>
                <button type="submit" class="form_button">Создать голосование</button>
            </div>
        </form>
    `);
}

export async function deleteVote(event) {
    event.preventDefault();
    event.stopPropagation();
    console.log('deleteVote')
    if (event.target.classList.contains('votelist_admin__delete')) {
        const voteItem = event.target.closest('.votelist_dropdown');

        const checkbox = voteItem.querySelector('.votelist_dropdown__checkbox');
        const voteId = checkbox.id;
        console.log('voteId: ', voteId)
        const isConfirmed = confirm("Вы уверены, что хотите удалить голосование?");

        if (!isConfirmed) {
            return;
        }

        socket.send(JSON.stringify({
            type: 'delete',
            id: Number(voteId)
        }))
        voteItem.remove();
    }
}

export async function updateVote(event) {
    event.preventDefault();
    event.stopPropagation();

    const form = event.target;
    const formData = new FormData(form);

    const voteId = form.dataset.voteId;
    const header = formData.get('voteName');

    const variants = [];
    for (let i = 0; i < form.elements.length; i++) {
        const element = form.elements[i];
        if (element.name && element.name.startsWith('variant')) {
            const value = element.value.trim();
            if (value) {
                variants.push(value);
            }
        }
    }

    // Правильно определяем many
    const radioMany = document.querySelector('#form_radio__many');
    const many = radioMany ? radioMany.checked : false;

    // Сохраняем состояние голосования пользователя
    const oldVoteElement = document.querySelector(`.votelist_dropdown[data-vote-id="${voteId}"]`);
    let hasVoted = false;
    if (oldVoteElement) {
        const oldButton = oldVoteElement.querySelector('.votelist_content__vote');
        hasVoted = oldButton && oldButton.disabled;
    }

    // Обновляем данные с сохранением состояния голосования
    const updatedVote = {
        id: Number(voteId),
        header: header,
        variants: variants.map((name, index) => ({
            id: index + 1,
            name: name
        })),
        many: many,
    };

    // Перерисовываем
    renderVote(updatedVote);

    // Восстанавливаем состояние кнопки
    if (hasVoted && window.config?.votedTopics) {
        if (!window.config.votedTopics.includes(Number(voteId))) {
            window.config.votedTopics.push(Number(voteId));
        }

        setTimeout(() => {
            const newVoteElement = document.querySelector(`.votelist_dropdown[data-vote-id="${voteId}"]`);
            if (newVoteElement) {
                const newButton = newVoteElement.querySelector('.votelist_content__vote');
                if (newButton) {
                    newButton.disabled = true;
                    newButton.textContent = 'Вы уже проголосовали';
                }
            }
        }, 20);
    }

    // Отправляем запрос на сервер
    socket.send(JSON.stringify({
        type: 'update',
        id: Number(voteId),
        header: header,
        variants: variants,
        many: many
    }));

    removeForm();
}

export async function editVote(event) {
    event.preventDefault();
    event.stopPropagation();

    const editButton = event.target.closest('.votelist_admin__edit');
    if (!editButton) return;

    const voteItem = editButton.closest('.votelist_dropdown');
    if (!voteItem) return;

    const checkbox = voteItem.querySelector('.votelist_dropdown__checkbox');
    const voteId = checkbox.id;

    const header = voteItem.querySelector('.votelist_label__header').innerText;

    const variantItems = voteItem.querySelectorAll('.votelist_content__list');
    const variants = Array.from(variantItems).map(item => {
        const label = item.querySelector('.votelist_content__checkboxDescription');
        return label ? label.innerText : '';
    }).filter(text => text);

    const firstCheckbox = voteItem.querySelector('.votelist_content__checkbox');
    const many = firstCheckbox ? firstCheckbox.type === 'checkbox' : false;

    removeForm();

    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="updateVote(event)" data-vote-id="${voteId}">
            <button type="button" class="form_close" onclick="removeForm()">x</button>
            <div class="form_label form_header">
                <div>Редактирование опроса</div>
                <div class="form_radioContainer">
                    <label for="form_radio__one" class="form_radio__label">Один</label>
                    <input type="radio" name="form_radio" id="form_radio__one" class="form_radio" ${!many ? 'checked' : ''}>
                    <label for="form_radio__many" class="form_radio__label">Много</label>
                    <input type="radio" name="form_radio" id="form_radio__many" class="form_radio" ${many ? 'checked' : ''}>
                </div>
            </div>
            <input type="text" id="voteName" class="form_input" name="voteName" value="${header.replace(/"/g, '&quot;')}" required>
            <div class="form_label">
                Варианты ответов
            </div>
            <ul class="form_variantsList">
                ${variants.map((variant, index) => `
                    <li class="form_variantItem">
                        <input type="text" class="form_input" name="variant${index}" value="${variant.replace(/"/g, '&quot;')}" required>
                        ${index > 0 ? '<button type="button" class="form_variantRemove" onclick="removeVariantField(event)" title="Удалить вариант">×</button>' : ''}
                    </li>
                `).join('')}
            </ul>
            <div class="form_bottomContainer">
                <div class="form_makeVariant"><button type="button" class="form_makeVariant__plus" onclick="addVariantField(event)">+</button></div>
                <button type="submit" class="form_button">Обновить голосование</button>
            </div>
        </form>
    `);

    window.currentVariantsCount = variants.length;
}

export function addVariantField(event) {
    event.preventDefault();
    const variantsList = document.querySelector('.form_variantsList');
    const currentCount = variantsList.children.length;

    if (currentCount < 10) {
        const newIndex = currentCount;
        variantsList.insertAdjacentHTML('beforeend', `
            <li class="form_variantItem">
                <input type="text" class="form_input" name="variant${newIndex}" required>
                <button type="button" class="form_variantRemove" onclick="removeVariantField(event)" title="Удалить вариант">×</button>
            </li>
        `);
    }

    if (currentCount + 1 >= 10) {
        const plusButton = document.querySelector('.form_makeVariant__plus');
        if (plusButton) plusButton.remove();
    }
}

// Функция для удаления варианта
export function removeVariantField(event) {
    event.preventDefault();
    const variantItem = event.target.closest('.form_variantItem');
    if (!variantItem) return;

    const variantsList = document.querySelector('.form_variantsList');
    variantItem.remove();

    // Показываем кнопку "+", если она была скрыта
    const plusButton = document.querySelector('.form_makeVariant__plus');
    if (!plusButton && variantsList.children.length < 10) {
        const makeVariantDiv = document.querySelector('.form_makeVariant');
        if (makeVariantDiv) {
            makeVariantDiv.innerHTML = '<button type="button" class="form_makeVariant__plus" onclick="addVariantField(event)">+</button>';
        }
    }

    // Обновляем имена и ID полей
    updateVariantFields();
}

// Функция для обновления индексов полей после удаления
function updateVariantFields() {
    const variantItems = document.querySelectorAll('.form_variantsList .form_variantItem');
    variantItems.forEach((item, index) => {
        const input = item.querySelector('.form_input');
        if (input) {
            input.name = `variant${index}`;
            input.id = `variant${index}`;
        }
    });
}

window.removeVariantField = removeVariantField
window.makeVoteForm = makeVoteForm;
window.makeVote = makeVote;
window.deleteVote = deleteVote;
window.editVote = editVote;
window.updateVote = updateVote;
window.addVariantField = addVariantField;