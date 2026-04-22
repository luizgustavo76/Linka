function openTab(evt, tabName) {
  let i, tabContent, tabBtns;

  // Esconde todos os conteúdos
  tabContent = document.getElementsByClassName("tab-content");
  for (i = 0; i < tabContent.length; i++) {
    tabContent[i].classList.remove("active");
  }

  // Remove classe 'active' dos botões
  tabBtns = document.getElementsByClassName("tab-btn");
  for (i = 0; i < tabBtns.length; i++) {
    tabBtns[i].classList.remove("active");
  }

  // Mostra o conteúdo atual e adiciona classe 'active' ao botão
  document.getElementById(tabName).classList.add("active");
  evt.currentTarget.classList.add("active");
}
