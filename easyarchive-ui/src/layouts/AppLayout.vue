<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <div class="brand">EasyArchive</div>
      <nav class="nav">
        <RouterLink class="nav__item" :to="{ name: 'dashboard' }">Dashboard</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'datasources' }">Datasources</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'tasks' }">Tasks</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'users' }">Users</RouterLink>
        <RouterLink class="nav__item nav__item--ghost" :to="{ name: 'login' }">Login</RouterLink>
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
