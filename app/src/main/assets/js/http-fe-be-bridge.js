'use strict';

function createBeFunction(funcName) {
    return async dto => {
        const res = await fetch(`/be/${funcName}`, {
            method: 'POST',
            body: JSON.stringify(dto??{}),
        })
        const body = await res.json()
        console.log('body', body)
        return body
    }
}

