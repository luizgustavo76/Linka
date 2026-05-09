async function login_request() {
    const username = document.getElementById("user").value;
    const password = document.getElementById("pass").value;

    const resposta = await fetch("http://127.0.0.1:5000/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            username,
            password
        })
    });

    const data = await resposta.json();

    if (resposta.status == 200) {
        
        alert("Login feito com sucesso");
    } else {
        alert("Falha no login");
    }
}
async function register_request() {
    const username = document.getElementById("reg-user").value;
    const password = document.getElementById("reg-pass").value;
    const email = document.getElementById("reg-email").value;
    const resposta = await fetch("http://127.0.0.1:5000/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            username,
            password,
            email
        })
    });

    const data = await resposta.json();

    if (resposta.status in (200, 201)) {
        alert("conta feita com sucesso");
    } else {
        alert("Falha no login");
    }
}