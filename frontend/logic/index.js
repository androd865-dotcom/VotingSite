import removeForm from './removeForm.js';

async function makeVote(event) {
    event.preventDefault();
}

let count = 0;

export default function makeVoteForm(event) {
    event.preventDefault();
    removeForm();
    count = 0
    document.querySelector('.main').insertAdjacentHTML('beforeend', `
        <form class="form" onsubmit="makeVote(event)" onclick="makeVariant(event)">
            <button type="button" class="form_close" onclick="removeForm()">x</button>
            <label for="voteName" class="form_label form_header"><div>Тема опроса<div/><input type="radio" name=""></input></label>
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
    event.preventDefault();
    if (event.target.classList.contains('form_makeVariant__plus')) {
        document.querySelector('.form_variantsList').insertAdjacentHTML('beforeend', `
            <li>
                <label for="variant${count}" class="form_label"></label>
                
                <input type="text" id="variant${count}" class="form_input" name="variant${count++}" required>
            </li>
        `)
        if (count == 7) document.querySelector('.form_makeVariant__plus').remove();
    }
}



window.makeVoteForm = makeVoteForm;
window.makeVariant = makeVariant;
window.makeVote = makeVote;