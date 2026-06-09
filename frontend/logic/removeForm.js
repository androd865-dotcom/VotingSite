export default function removeForm() {
    const oldForm = document.querySelector('.form');
    if (oldForm) {
        oldForm.remove();
    }
}