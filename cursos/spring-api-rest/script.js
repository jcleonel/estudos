document.addEventListener('DOMContentLoaded', () => {
    const topicLinks = document.querySelectorAll('.sidebar-nav a');
    const contentArea = document.getElementById('topic-content');
    let currentActiveLink = null;

    // Função para carregar o conteúdo de um tópico
    async function loadTopic(page) {
        // Mostra um feedback visual enquanto carrega
        contentArea.innerHTML = '<h1>Carregando...</h1>';
        try {
            const response = await fetch(`./topicos/${page}.html`);
            if (!response.ok) {
                throw new Error(`Não foi possível encontrar o arquivo: ${page}.html`);
            }
            const content = await response.text();
            contentArea.innerHTML = content;
        } catch (error) {
            console.error('Erro ao carregar o tópico:', error);
            contentArea.innerHTML = `<h1>Erro 404</h1><p>O conteúdo não pôde ser carregado. Verifique se o arquivo <strong>${page}.html</strong> existe na pasta /topicos/.</p>`;
        }
    }

    // Adiciona o evento de clique a cada link do menu
    topicLinks.forEach(link => {
        link.addEventListener('click', (event) => {
            event.preventDefault(); // Impede a navegação padrão do link

            // Remove a classe 'active' do link anterior
            if(currentActiveLink) {
                currentActiveLink.classList.remove('active');
            }

            // Adiciona a classe 'active' ao link clicado
            link.classList.add('active');
            currentActiveLink = link;

            const page = link.getAttribute('data-page');
            loadTopic(page);
        });
    });

    // Carrega a página de boas-vindas ou o primeiro tópico por padrão
    const initialPage = 'boas-vindas'; // ou o nome do primeiro tópico, se preferir
    loadTopic(initialPage);

    // Opcional: Ativa o primeiro link da lista se não for o de boas vindas
    // const firstLink = document.querySelector('.sidebar-nav a[data-page="1-fundamentos-rest"]');
    // if(firstLink){
    //     firstLink.classList.add('active');
    //     currentActiveLink = firstLink;
    //     loadTopic(firstLink.getAttribute('data-page'));
    // }
});