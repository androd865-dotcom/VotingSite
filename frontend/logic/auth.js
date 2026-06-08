async function isAuthorized() {

}

async function login(name, password) {
    const json = await fetch('http://localhost:3000/api/login', {
        method: 'POST',
        body: JSON.stringify({name, password})
    })
    console.log(json)
}

async function register(name, password) {
    const json = await fetch('http://localhost:3000/api/register', {
        method: 'POST',
        body: JSON.stringify({name, password})
    })
    console.log(json)
}