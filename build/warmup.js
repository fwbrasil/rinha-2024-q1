import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
    stages: [
        { duration: '1s', target: 10 },
        { duration: '30s', target: 10 },
    ]
};

export default function () {

    const transactionUrl = 'http://nginx:9999/clientes/0/transacoes';
    const extratoUrl = 'http://nginx:9999/clientes/0/extrato';
    const headers = { 'Content-Type': 'application/json' };

    // Alternating transaction type for each iteration
    const transactionType = (__ITER % 2 === 0) ? "c" : "d";
    const payload = JSON.stringify({
        valor: 1,
        tipo: transactionType,
        descricao: "warmup"
    });

    // Set request timeout options
    const requestOptions = {
        headers: headers,
        timeout: '1s' // Set timeout to 1 second
    };

    // Perform a transaction with a timeout
    let transactionRes = http.post(transactionUrl, payload, requestOptions);
    check(transactionRes, { 'transaction status was 200': (r) => r.status == 200 });

    // Retrieve the account statement with a timeout
    let extratoRes = http.get(extratoUrl, { timeout: '1s' }); // Apply timeout to GET request
    check(extratoRes, { 'extrato status was 200': (r) => r.status == 200 });
}
