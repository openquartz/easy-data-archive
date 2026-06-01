<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <div class="brand">EasyArchive</div>
      <nav class="nav">
        <RouterLink class="nav__item" to="/">Dashboard</RouterLink>
        <RouterLink class="nav__item" to="/datasources">Datasources</RouterLink>
        <RouterLink class="nav__item" to="/users">Users</RouterLink>
        <RouterLink class="nav__item" to="/tasks">Tasks</RouterLink>
      </nav>
    </aside>
    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <strong>Operations Console</strong>
      </header>
      <main class="app-shell__content">
        <router-view />
      </main>
    </div>
  </div>
</template>
