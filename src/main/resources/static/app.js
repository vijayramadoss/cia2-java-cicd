"use strict";

const api = "api/tasks";

function byId(id) {
    return document.getElementById(id);
}

async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = "Request failed (" + response.status + ")";
        try {
            const body = await response.json();
            if (body.error) {
                message = body.error;
            }
        } catch (ignored) {
            // response had no JSON body
        }
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}

function showError(message) {
    byId("error").textContent = message || "";
}

function renderTask(task) {
    const item = document.createElement("li");
    if (task.completed) {
        item.classList.add("done");
    }

    const title = document.createElement("span");
    title.textContent = task.title; // textContent avoids HTML injection
    item.appendChild(title);

    const actions = document.createElement("div");
    if (!task.completed) {
        const doneButton = document.createElement("button");
        doneButton.textContent = "Done";
        doneButton.addEventListener("click", () => changeTask(api + "/" + task.id + "/complete", "PUT"));
        actions.appendChild(doneButton);
    }
    const deleteButton = document.createElement("button");
    deleteButton.textContent = "Delete";
    deleteButton.className = "secondary";
    deleteButton.addEventListener("click", () => changeTask(api + "/" + task.id, "DELETE"));
    actions.appendChild(deleteButton);

    item.appendChild(actions);
    return item;
}

async function loadTasks() {
    const [tasks, stats] = await Promise.all([request(api), request(api + "/stats")]);
    const list = byId("task-list");
    list.replaceChildren(...tasks.map(renderTask));
    byId("empty").hidden = tasks.length > 0;
    byId("stat-total").textContent = stats.total;
    byId("stat-pending").textContent = stats.pending;
    byId("stat-completed").textContent = stats.completed;
}

async function changeTask(url, method) {
    try {
        showError("");
        await request(url, { method });
        await loadTasks();
    } catch (e) {
        showError(e.message);
    }
}

async function loadInfo() {
    const info = await request("api/info");
    byId("app-name").textContent = info.name;
    byId("app-message").textContent = info.message;
    byId("app-version").textContent = info.version;
    byId("app-host").textContent = info.host;
    document.title = info.name;
}

byId("task-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const input = byId("task-title");
    try {
        showError("");
        await request(api, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title: input.value })
        });
        input.value = "";
        await loadTasks();
    } catch (e) {
        showError(e.message);
    }
});

loadInfo().catch((e) => showError(e.message));
loadTasks().catch((e) => showError(e.message));
