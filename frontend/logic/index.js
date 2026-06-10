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
    count = 0;
    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="makeVote(event)" onclick="makeVariant(event)">
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
                <li>
                    <label for="variant" class="form_label"></label>
                    <input type="text" id="variant" class="form_input" name="variant" required>
                </li>
                
            </ul>
            <div class="form_bottomContainer">
                <div class="form_makeVariant"><button type="button" class="form_makeVariant__plus">+</button></div>
                <button type="submit" class="form_button">Создать голосование</button>
            </div>
        </form>
    `);
}

function makeVariant(event) {
    if (event.target.classList.contains('form_makeVariant__plus')) {
        document.querySelector('.form_variantsList').insertAdjacentHTML('beforeend', `
            <li>
                <label for="variant${count}" class="form_label"></label>
                
                <input type="text" id="variant${count}" class="form_input" name="variant${count++}" required>
            </li>
        `)
        if (count === 7) document.querySelector('.form_makeVariant__plus').remove();
    }
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

    const radioOne = document.querySelector('#form_radio__one');
    const many = radioOne ? !radioOne.checked : true;

    const oldVoteItem = document.querySelector(`.votelist_dropdown input[id="${voteId}"]`)?.closest('.votelist_dropdown');

    if (oldVoteItem) {
        const updatedVote = {
            id: Number(voteId),
            header: header,
            variants: variants.map((name, index) => ({
                id: index,
                name: name
            })),
            many: many
        };

        const temp = document.createElement('div');
        temp.innerHTML = renderVoteToString(updatedVote); // Нужна новая функция

        oldVoteItem.outerHTML = temp.firstElementChild.outerHTML;
    }

    socket.send(JSON.stringify({
        type: 'update',
        id: Number(voteId),
        header: header,
        variants: variants,
        many: many
    }));

    removeForm();
}

function renderVoteToString(data) {
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
                    <input type="radio" class="votelist_content__checkbox" id="votelist_content__checkbox${data.variants[i].id}" name=${data.id} />
                    <label class="votelist_content__checkboxDescription" for="votelist_content__checkbox${data.variants[i].id}">${data.variants[i].name}</label>
                </li>
            `
        }
    }

    return `
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
                    <li>
                        <input type="text" class="form_input" name="variant${index}" value="${variant.replace(/"/g, '&quot;')}" required>
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
        variantsList.insertAdjacentHTML('beforeend', `
            <li>
                <input type="text" class="form_input" name="variant${currentCount}" required>
            </li>
        `);
    }

    if (currentCount + 1 >= 10) {
        const plusButton = document.querySelector('.form_makeVariant__plus');
        if (plusButton) plusButton.remove();
    }
}


window.makeVoteForm = makeVoteForm;
window.makeVariant = makeVariant;
window.makeVote = makeVote;
window.deleteVote = deleteVote;
window.editVote = editVote;
window.updateVote = updateVote;
window.addVariantField = addVariantField;