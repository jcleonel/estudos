# Relatório de Handover: Projetos Custody (Frontend e BFF)

**Público-alvo:** Equipe de desenvolvimento Depositária.  
**Autores:** Jean Carlos Leonel Da Costa, com contribuições de Guilherme Henrique Sala Pais (equipe anterior).  
**Data:** 14 de agosto de 2025

## 1. Introdução e Objetivo

Este documento serve como o relatório oficial de handover (transferência de conhecimento) dos projetos que compõem a funcionalidade de **Consulta de Custódia**. O seu objetivo é fornecer à equipe Depositária todo o conhecimento necessário para entender a arquitetura, a estrutura e o processo de desenvolvimento local dos sistemas, permitindo a manutenção e evolução contínua dos mesmos.

Os sistemas em escopo são:
* **Backend-for-Frontend:** `blc-pblc-bff-custody`
* **Frontend (Microfrontend):** `blc-pblc-mfe-custody-ui`

## Links Rápidos e Repositórios

* **Repositório BFF:** [https://github.com/b3sa/blc-pblc-bff-custody](https://github.com/b3sa/blc-pblc-bff-custody)
* **Repositório Frontend:** [https://github.com/b3sa/blc-pblc-mfe-custody-ui](https://github.com/b3sa/blc-pblc-mfe-custody-ui)

---

## 2. Projeto BFF: blc-pblc-bff-custody

### 2.1. Visão Geral

O `blc-pblc-bff-custody` é um serviço Backend-for-Frontend construído em **Java 17** com o framework **Quarkus**. Sua principal responsabilidade é servir como uma camada intermediária entre o frontend (`custody-ui`) e os serviços de backend, principalmente o de `accounts` (Carteiras).

Ele atua como um orquestrador e tradutor, simplificando a lógica que o frontend precisa consumir e adaptando as requisições para os padrões dos microserviços de backend.

### 2.2. Arquitetura e Padrões

O projeto foi estruturado utilizando os princípios da **Arquitetura Hexagonal (Ports & Adapters)**. Este padrão visa isolar a lógica de negócio principal da aplicação (o "core") das dependências externas, como APIs, bancos de dados ou mensageria.

O fluxo de uma requisição segue este caminho:

1.  **Entrada (Adapter):** Um endpoint REST, chamado de `Resource` no Quarkus (ex: `CarteirasResource`), recebe a requisição HTTP. Ele está no pacote `adapter.input`.
2.  **Porta de Entrada (Port):** O `Resource` invoca um `UseCase` (ex: `GetCarteirasUseCase`), que é uma interface definindo o contrato da funcionalidade. Essa interface é uma "Porta de Entrada" e fica em `application.ports.input`.
3.  **Core da Aplicação (Service):** A implementação concreta do `UseCase` (ex: `GetCarteirasService`) contém a lógica de negócio. Ela fica em `application.service`.
4.  **Porta de Saída (Port):** Para se comunicar com o "mundo externo" (como outra API), o `Service` utiliza outra interface, a "Porta de Saída" (ex: `CarteirasOutputPort`). Ela define o contrato de como buscar os dados externos e fica em `application.ports.output`.
5.  **Saída (Adapter):** A implementação concreta da Porta de Saída (ex: `CarteirasAccountsBackendAdapter`) é o "adaptador" que efetivamente sabe como se comunicar com a tecnologia externa. Neste caso, ele usa um `RestClient` para chamar a API de `accounts`. Ele fica no pacote `adapter.output`.

#### O Papel do BFF como Proxy e Tradutor ("De-Para")

> **[Explicação Gemini] O que é um "Proxy para o Backend"?**
>
> Pense em um **Proxy** como um **intermediário inteligente**. Em vez de o frontend chamar o backend diretamente, ele chama o BFF (o proxy). O BFF então repassa essa chamada para o backend de destino.
>
> **Por que usar isso?**
> 1.  **Tradução (De-Para):** Como você notou, o BFF recebe requisições com campos em português e as "traduz" para o inglês antes de enviá-las ao backend de `accounts`. Ele também faz o caminho inverso na resposta. Isso é o "mapeamento" ou "de-para" que foi mencionado. O benefício é que o frontend pode ser desenvolvido em um idioma familiar ao negócio (português), enquanto os microserviços de backend mantêm um padrão global (inglês), sem que um precise conhecer a linguagem do outro.
> 2.  **Simplificação (Agregação):** Se o frontend precisasse de dados de 3 serviços diferentes para montar uma tela, ele teria que fazer 3 chamadas. Com um BFF, o frontend faz uma única chamada ao BFF, e o BFF se encarrega de chamar os 3 serviços e juntar (agregar) as respostas em um formato simples para a tela.
> 3.  **Segurança e Abstração:** O BFF pode adicionar uma camada de segurança e esconder a complexidade da rede de microserviços do frontend.

O `AccountsRestClient` é a peça final que executa a comunicação. É uma interface do MicroProfile Rest Client anotada com `@RegisterRestClient`. O Quarkus, em tempo de execução, gera automaticamente a implementação desta interface, usando as URLs configuradas no `application.yaml` para fazer as chamadas HTTP para o backend de `accounts`. Para injetá-lo, utiliza-se a anotação `@RestClient`.

### 2.3. Como Executar Localmente

**Pré-requisitos:**
* JDK 17 ou superior.
* Apache Maven 3.8.x ou superior.

**Passos:**

1.  **Clonar o Repositório:**
    ```bash
    git clone [https://github.com/b3sa/blc-pblc-bff-custody.git](https://github.com/b3sa/blc-pblc-bff-custody.git)
    cd blc-pblc-bff-custody
    ```

2.  **Configurar Variáveis de Ambiente:**
    Crie um arquivo `.env` na raiz do projeto com o seguinte conteúdo. Embora tenha havido dúvida na reunião se ele é estritamente necessário para rodar, é uma boa prática tê-lo configurado. O Quarkus carrega este arquivo automaticamente.

    ```properties
    KAFKA_SASL_USERNAME=_svcPBLCN
    KAFKA_SASL_PASSWORD=senha_do_kafka
    AUTH4B3_CLIENT_SECRET=secret_client
    B3_IDENTIFICADORES_PARTICIPANTES=6760,123,321
    ```

3.  **Iniciar a Aplicação:**
    Você pode iniciar em modo de desenvolvimento (com hot reload) usando um dos seguintes comandos no terminal:

    ```bash
    # Usando o Maven Wrapper (recomendado)
    ./mvnw quarkus:dev
    ```
    ou
    ```bash
    # Usando a CLI do Quarkus (se instalada)
    quarkus dev
    ```

4.  **Acesso:**
    A aplicação estará disponível em `http://localhost:8080`. A interface do Swagger para visualização dos endpoints pode ser acessada em `http://localhost:8080/q/swagger-ui`.

    **Observação Importante:** Para facilitar o desenvolvimento local, a validação de token de autenticação foi desabilitada nos ambientes `local` e `dev`.

### 2.4. Testes

A estratégia de testes do BFF segue os padrões do Quarkus, utilizando **JUnit 5** para a estrutura dos testes e **Mockito** para a criação de mocks (simulacros) de dependências externas, como os `OutputPorts`. A cobertura de código atual do projeto está em aproximadamente **75%**.

---

## 3. Projeto Frontend: blc-pblc-mfe-custody-ui

### 3.1. Visão Geral

O `blc-pblc-mfe-custody-ui` é uma aplicação **Angular** que implementa a interface de usuário para a funcionalidade de Consulta de Custódia. Ele foi desenvolvido seguindo uma arquitetura de **Microfrontend**, projetado para operar de forma semi-independente e ser integrado a uma aplicação "hospedeira" maior, conhecida como Shell.

> **[Explicação Gemini] O que é o template Ultron?**
>
> O "Ultron" muito provavelmente é uma **ferramenta interna de scaffolding** ou um **gerador de projetos** da empresa. Ele cria uma estrutura de projeto Angular inicial já com pacotes pré-definidos (`utils`, `shared`, etc.), configurações de build e padrões de código da companhia. Isso garante consistência entre os diversos projetos frontend e acelera o início do desenvolvimento. Não é um framework público, mas sim uma ferramenta customizada.

### 3.2. Arquitetura de Microfrontend

O projeto utiliza a biblioteca `@angular-architects/native-federation` para implementar o padrão de microfrontend.

> **[Explicação Gemini] O que é um "Componente Shell"?**
>
> O **Shell** (ou "casca") é a aplicação Angular principal que **hospeda** os microfrontends. Pense nele como uma moldura que contém o menu superior, a barra lateral e a área de conteúdo principal. O Shell é responsável por:
> 1.  **Carregar os Microfrontends:** Com base na rota acessada pelo usuário (URL), o Shell sabe qual microfrontend deve ser carregado na área de conteúdo.
> 2.  **Orquestrar a Navegação:** Ele gerencia as rotas principais da aplicação.
> 3.  **Fornecer Elementos Comuns:** Disponibiliza menus, cabeçalhos e rodapés que são consistentes em toda a aplicação.
>
> O problema mencionado é que atualmente **não existe um Shell único**. O microfrontend de Custódia está sendo carregado dentro do `blc-pblc-shell-movement-ui`, o que causa a rota indesejada `movements/lista-movimentos/consulta-de-custodia`. O ideal, no futuro, é ter um Shell de plataforma que carregue "Custódia" em uma rota raiz, como `/custodia`.

No arquivo `federation.config.ts`, o microfrontend declara o que ele expõe para o mundo exterior. No caso, ele expõe suas rotas: `expose: { './routes': './src/app/app.routes.ts' }`. É assim que o Shell sabe quais páginas este microfrontend contém.

> **[Explicação Gemini] O que significa "não fazer o bundle"?**
>
> "Bundle" (ou empacotamento) é o processo onde o Angular agrupa todo o seu código e o código das bibliotecas que você usa em poucos arquivos JavaScript para serem enviados ao navegador.
>
> Na configuração de `skip`, ao adicionar bibliotecas como `'@angular/cdk'`, você está dizendo ao Native Federation: "**Não inclua o código desta biblioteca no meu pacote.** Eu assumo que o **Shell** (ou outra dependência compartilhada) já vai carregar essa biblioteca."
>
> Isso é uma otimização crucial em microfrontends. Se cada um dos 5 microfrontends incluísse o Angular CDK em seu próprio bundle, o usuário teria que baixar o mesmo código 5 vezes. Ao "skipar", o código é baixado uma única vez, melhorando drasticamente o tempo de carregamento da aplicação.

### 3.3. Como Executar Localmente

**Pré-requisitos:**
* Node.js (recomenda-se a versão LTS mais recente).
* NPM (geralmente instalado junto com o Node.js).

**Passos:**

1.  **Clonar o Repositório:**
    ```bash
    git clone [https://github.com/b3sa/blc-pblc-mfe-custody-ui.git](https://github.com/b3sa/blc-pblc-mfe-custody-ui.git)
    cd blc-pblc-mfe-custody-ui
    ```

2.  **Instalar Dependências:**
    Este comando lê o arquivo `package.json` e baixa todas as bibliotecas necessárias para o projeto na pasta `node_modules`.

    ```bash
    npm install
    ```

3.  **Configurar o Proxy de Desenvolvimento:**
    Crie um arquivo chamado `proxy.conf.mjs` na raiz do projeto. Este arquivo **não deve ser versionado** no Git.

    ```javascript
    export default {
      '/menu/api/pblc/api/custody/v1': {
        // Para apontar para o BFF rodando em DEV
        target: 'http//pblc-dev-n.internalenv.oci/pblc/api/custody/v1',
        // Para apontar para o BFF rodando na sua máquina local
        // target: 'http://localhost:8080',
        secure: false,
        changeOrigin: true,
        pathRewrite: {
          '^/menu/api/pblc/api/custody/v1': ''
        }
      }
    };
    ```

> **[Explicação Gemini] Para que serve o `proxy.conf.mjs`?**
>
> O servidor de desenvolvimento do Angular (`ng serve`) cria um proxy para contornar problemas de **CORS (Cross-Origin Resource Sharing)**. O navegador, por segurança, impede que uma página em `localhost:4200` (seu front) faça requisições diretas para `pblc-dev...` (o BFF em dev) ou até mesmo `localhost:8080` (seu BFF local).
>
> O que o proxy faz:
> 1.  `'/menu/api/...'`: Ele intercepta todas as chamadas que o seu código Angular faz para este caminho.
> 2.  `target`: Ele **redireciona** essa chamada para o endereço de destino (o BFF). Como a chamada é feita do servidor (Node.js) para o BFF, e não do navegador, a restrição do CORS não se aplica.
> 3.  `changeOrigin: true`: Altera a origem da requisição para o `target`, ajudando a evitar problemas de segurança em alguns servidores de destino.
> 4.  `pathRewrite`: Remove o prefixo `/menu/api/...` antes de enviar a requisição para o `target`. Isso é útil porque a URL do código (`${this.apiUrl}`) é uma URL "fake" (`/menu/api/...`) necessária para a aplicação legada (`NoMe`), mas a API do BFF real não tem esse prefixo. O proxy faz essa "limpeza" da URL.

4.  **Iniciar a Aplicação:**
    No terminal, execute um dos seguintes comandos:

    ```bash
    # Usando o script definido no package.json (recomendado)
    npm start
    ```
    ou
    ```bash
    # Usando a CLI do Angular (se instalada globalmente)
    ng serve
    ```

5.  **Acesso:**
    A aplicação estará disponível em `http://localhost:4200`. Note que, ao rodar localmente desta forma, apenas o microfrontend será renderizado, sem o Shell (menus, cabeçalho, etc).

### 3.4. Testes

Os testes unitários são desenvolvidos com **Jest**. Os arquivos de teste possuem a extensão `.spec.ts` e ficam ao lado dos componentes e serviços que eles testam. O foco principal é testar a lógica de negócio contida nos arquivos TypeScript (`.ts`), e não a renderização de layout (HTML/CSS). A cobertura de código atual do projeto está em aproximadamente **88%**.










------------------------------------------------








# Relatório de Handover: Projetos Custody (Frontend e BFF)

**Público-alvo:** Equipe de desenvolvimento Depositária.  
**Autores:** Jean Carlos Leonel Da Costa, com contribuições de Guilherme Henrique Sala Pais (equipe anterior).  
**Data:** 14 de agosto de 2025

## 1. Introdução e Objetivo

Este documento serve como o relatório oficial de handover (transferência de conhecimento) dos projetos que compõem a funcionalidade de **Consulta de Custódia**. O seu objetivo é fornecer à equipe Depositária todo o conhecimento necessário para entender a arquitetura, a estrutura e o processo de desenvolvimento local dos sistemas, permitindo a manutenção e evolução contínua dos mesmos.

Os sistemas em escopo são:
* **Backend-for-Frontend:** `blc-pblc-bff-custody`
* **Frontend (Microfrontend):** `blc-pblc-mfe-custody-ui`

### 1.1. Diagrama de Arquitetura Simplificado

O diagrama abaixo ilustra o fluxo de comunicação entre os componentes envolvidos na solução:

+------------+     +---------------------------+     +-----------------+     +----------------------+
|   Usuário  |----->| Frontend (Angular MFE)    |----->| BFF (Quarkus)   |----->| Backend Accounts API |
|            |<-----|                           |<-----|                 |<-----|                      |
+------------+     +---------------------------+     +--------+--------+     +----------------------+


### 1.2. Links Rápidos e Repositórios

* **Repositório BFF:** [https://github.com/b3sa/blc-pblc-bff-custody](https://github.com/b3sa/blc-pblc-bff-custody)
* **Repositório Frontend:** [https://github.com/b3sa/blc-pblc-mfe-custody-ui](https://github.com/b3sa/blc-pblc-mfe-custody-ui)

---

## 2. Projeto BFF: blc-pblc-bff-custody

### 2.1. Visão Geral

O `blc-pblc-bff-custody` é um serviço Backend-for-Frontend construído em **Java 17** com o framework **Quarkus**. Sua principal responsabilidade é servir como uma camada intermediária entre o frontend (`custody-ui`) e os serviços de backend, principalmente o de `accounts` (Carteiras).

Ele atua como um orquestrador e tradutor, simplificando a lógica que o frontend precisa consumir e adaptando as requisições para os padrões dos microserviços de backend.

### 2.2. Arquitetura e Padrões

O projeto foi estruturado utilizando os princípios da **Arquitetura Hexagonal (Ports & Adapters)**. Este padrão visa isolar a lógica de negócio principal da aplicação (o "core") das dependências externas, como APIs, bancos de dados ou mensageria.

O fluxo de uma requisição segue este caminho:

1.  **Entrada (Adapter):** Um endpoint REST, chamado de `Resource` no Quarkus (ex: `CarteirasResource`), recebe a requisição HTTP. Ele está no pacote `adapter.input`.
2.  **Porta de Entrada (Port):** O `Resource` invoca um `UseCase` (ex: `GetCarteirasUseCase`), que é uma interface definindo o contrato da funcionalidade. Essa interface é uma "Porta de Entrada" e fica em `application.ports.input`.
3.  **Core da Aplicação (Service):** A implementação concreta do `UseCase` (ex: `GetCarteirasService`) contém a lógica de negócio. Ela fica em `application.service`.
4.  **Porta de Saída (Port):** Para se comunicar com o "mundo externo" (como outra API), o `Service` utiliza outra interface, a "Porta de Saída" (ex: `CarteirasOutputPort`). Ela define o contrato de como buscar os dados externos e fica em `application.ports.output`.
5.  **Saída (Adapter):** A implementação concreta da Porta de Saída (ex: `CarteirasAccountsBackendAdapter`) é o "adaptador" que efetivamente sabe como se comunicar com a tecnologia externa. Neste caso, ele usa um `RestClient` para chamar a API de `accounts`. Ele fica no pacote `adapter.output`.

#### O Papel do BFF como Proxy e Tradutor ("De-Para")

> **[Explicação Gemini] O que é um "Proxy para o Backend"?**
>
> Pense em um **Proxy** como um **intermediário inteligente**. Em vez de o frontend chamar o backend diretamente, ele chama o BFF (o proxy). O BFF então repassa essa chamada para o backend de destino.
>
> **Por que usar isso?**
> 1.  **Tradução (De-Para):** Como você notou, o BFF recebe requisições com campos em português e as "traduz" para o inglês antes de enviá-las ao backend de `accounts`. Ele também faz o caminho inverso na resposta. Isso é o "mapeamento" ou "de-para" que foi mencionado. O benefício é que o frontend pode ser desenvolvido em um idioma familiar ao negócio (português), enquanto os microserviços de backend mantêm um padrão global (inglês), sem que um precise conhecer a linguagem do outro.
> 2.  **Simplificação (Agregação):** Se o frontend precisasse de dados de 3 serviços diferentes para montar uma tela, ele teria que fazer 3 chamadas. Com um BFF, o frontend faz uma única chamada ao BFF, e o BFF se encarrega de chamar os 3 serviços e juntar (agregar) as respostas em um formato simples para a tela.
> 3.  **Segurança e Abstração:** O BFF pode adicionar uma camada de segurança e esconder a complexidade da rede de microserviços do frontend.

O `AccountsRestClient` é a peça final que executa a comunicação. É uma interface do MicroProfile Rest Client anotada com `@RegisterRestClient`. O Quarkus, em tempo de execução, gera automaticamente a implementação desta interface, usando as URLs configuradas no `application.yaml` para fazer as chamadas HTTP para o backend de `accounts`. Para injetá-lo, utiliza-se a anotação `@RestClient`.

### 2.3. Como Executar Localmente

**Pré-requisitos:**
* JDK 17 ou superior.
* Apache Maven 3.8.x ou superior.

**Passos:**

1.  **Clonar o Repositório:**
    ```bash
    git clone [https://github.com/b3sa/blc-pblc-bff-custody.git](https://github.com/b3sa/blc-pblc-bff-custody.git)
    cd blc-pblc-bff-custody
    ```

2.  **Configurar Variáveis de Ambiente:**
    Crie um arquivo `.env` na raiz do projeto. O Quarkus carrega este arquivo automaticamente.

| Variável | Descrição |
| :--- | :--- |
| `KAFKA_SASL_USERNAME` | Usuário para autenticação no serviço de Kafka. |
| `KAFKA_SASL_PASSWORD`| Senha para autenticação no serviço de Kafka. |
| `AUTH4B3_CLIENT_SECRET` | Chave de acesso ("secret") para o serviço de autorização (Motor Autorizador B3). |
| `B3_IDENTIFICADORES_PARTICIPANTES` | Lista de IDs de participantes utilizada para filtros na aplicação. |


3.  **Iniciar a Aplicação:**
    Você pode iniciar em modo de desenvolvimento (com hot reload) usando um dos seguintes comandos no terminal:

    ```bash
    # Usando o Maven Wrapper (recomendado)
    ./mvnw quarkus:dev
    ```
    ou
    ```bash
    # Usando a CLI do Quarkus (se instalada)
    quarkus dev
    ```

4.  **Acesso:**
    A aplicação estará disponível em `http://localhost:8080`. A interface do Swagger para visualização dos endpoints pode ser acessada em `http://localhost:8080/q/swagger-ui`.

    **Observação Importante:** Para facilitar o desenvolvimento local, a validação de token de autenticação foi desabilitada nos ambientes `local` e `dev`.

### 2.4. Testes

A estratégia de testes do BFF segue os padrões do Quarkus, utilizando **JUnit 5** para a estrutura dos testes e **Mockito** para a criação de mocks de dependências externas. A cobertura de código atual do projeto está em aproximadamente **75%**.

### 2.5. Dependências Críticas

* **Java:** Versão 17
* **Quarkus:** 3.x (verificar `pom.xml` para a versão exata)
* **Maven:** 3.8.x

---

## 3. Projeto Frontend: blc-pblc-mfe-custody-ui

### 3.1. Visão Geral

O `blc-pblc-mfe-custody-ui` é uma aplicação **Angular** que implementa a interface de usuário para a funcionalidade de Consulta de Custódia. Ele foi desenvolvido seguindo uma arquitetura de **Microfrontend**, projetado para operar de forma semi-independente e ser integrado a uma aplicação "hospedeira" maior, conhecida como Shell.

> **[Explicação Gemini] O que é o template Ultron?**
>
> O "Ultron" muito provavelmente é uma **ferramenta interna de scaffolding** ou um **gerador de projetos** da empresa. Ele cria uma estrutura de projeto Angular inicial já com pacotes pré-definidos (`utils`, `shared`, etc.), configurações de build e padrões de código da companhia. Isso garante consistência entre os diversos projetos frontend e acelera o início do desenvolvimento. Não é um framework público, mas sim uma ferramenta customizada.

### 3.2. Arquitetura de Microfrontend

O projeto utiliza a biblioteca `@angular-architects/native-federation` para implementar o padrão de microfrontend.

> **[Explicação Gemini] O que é um "Componente Shell"?**
>
> O **Shell** (ou "casca") é a aplicação Angular principal que **hospeda** os microfrontends. Pense nele como uma moldura que contém o menu superior, a barra lateral e a área de conteúdo principal. O Shell é responsável por:
> 1.  **Carregar os Microfrontends:** Com base na rota acessada pelo usuário (URL), o Shell sabe qual microfrontend deve ser carregado na área de conteúdo.
> 2.  **Orquestrar a Navegação:** Ele gerencia as rotas principais da aplicação.
> 3.  **Fornecer Elementos Comuns:** Disponibiliza menus, cabeçalhos e rodapés que são consistentes em toda a aplicação.
>
> O problema mencionado é que atualmente **não existe um Shell único**. O microfrontend de Custódia está sendo carregado dentro do `blc-pblc-shell-movement-ui`, o que causa a rota indesejada `movements/lista-movimentos/consulta-de-custodia`. O ideal, no futuro, é ter um Shell de plataforma que carregue "Custódia" em uma rota raiz, como `/custodia`.

No arquivo `federation.config.ts`, o microfrontend declara o que ele expõe para o mundo exterior. No caso, ele expõe suas rotas: `expose: { './routes': './src/app/app.routes.ts' }`. É assim que o Shell sabe quais páginas este microfrontend contém.

> **[Explicação Gemini] O que significa "não fazer o bundle"?**
>
> "Bundle" (ou empacotamento) é o processo onde o Angular agrupa todo o seu código e o código das bibliotecas que você usa em poucos arquivos JavaScript para serem enviados ao navegador.
>
> Na configuração de `skip`, ao adicionar bibliotecas como `'@angular/cdk'`, você está dizendo ao Native Federation: "**Não inclua o código desta biblioteca no meu pacote.** Eu assumo que o **Shell** (ou outra dependência compartilhada) já vai carregar essa biblioteca."
>
> Isso é uma otimização crucial em microfrontends. Se cada um dos 5 microfrontends incluísse o Angular CDK em seu próprio bundle, o usuário teria que baixar o mesmo código 5 vezes. Ao "skipar", o código é baixado uma única vez, melhorando drasticamente o tempo de carregamento da aplicação.

### 3.3. Como Executar Localmente

**Pré-requisitos:**
* Node.js (recomenda-se a versão LTS 18.x ou superior).
* NPM (geralmente instalado junto com o Node.js).

**Passos:**

1.  **Clonar o Repositório:**
    ```bash
    git clone [https://github.com/b3sa/blc-pblc-mfe-custody-ui.git](https://github.com/b3sa/blc-pblc-mfe-custody-ui.git)
    cd blc-pblc-mfe-custody-ui
    ```

2.  **Instalar Dependências:**
    Este comando lê o arquivo `package.json` e baixa todas as bibliotecas necessárias para o projeto na pasta `node_modules`.

    ```bash
    npm install
    ```

3.  **Configurar o Proxy de Desenvolvimento:**
    Crie um arquivo chamado `proxy.conf.mjs` na raiz do projeto. Este arquivo **não deve ser versionado** no Git.

    ```javascript
    export default {
      '/menu/api/pblc/api/custody/v1': {
        // Para apontar para o BFF rodando em DEV
        target: 'http//pblc-dev-n.internalenv.oci/pblc/api/custody/v1',
        // Para apontar para o BFF rodando na sua máquina local
        // target: 'http://localhost:8080',
        secure: false,
        changeOrigin: true,
        pathRewrite: {
          '^/menu/api/pblc/api/custody/v1': ''
        }
      }
    };
    ```

> **[Explicação Gemini] Para que serve o `proxy.conf.mjs`?**
>
> O servidor de desenvolvimento do Angular (`ng serve`) cria um proxy para contornar problemas de **CORS (Cross-Origin Resource Sharing)**. O navegador, por segurança, impede que uma página em `localhost:4200` (seu front) faça requisições diretas para `pblc-dev...` (o BFF em dev) ou até mesmo `localhost:8080` (seu BFF local).
>
> O que o proxy faz:
> 1.  `'/menu/api/...'`: Ele intercepta todas as chamadas que o seu código Angular faz para este caminho.
> 2.  `target`: Ele **redireciona** essa chamada para o endereço de destino (o BFF). Como a chamada é feita do servidor (Node.js) para o BFF, e não do navegador, a restrição do CORS não se aplica.
> 3.  `changeOrigin: true`: Altera a origem da requisição para o `target`, ajudando a evitar problemas de segurança em alguns servidores de destino.
> 4.  `pathRewrite`: Remove o prefixo `/menu/api/...` antes de enviar a requisição para o `target`. Isso é útil porque a URL do código (`${this.apiUrl}`) é uma URL "fake" (`/menu/api/...`) necessária para a aplicação legada (`NoMe`), mas a API do BFF real não tem esse prefixo. O proxy faz essa "limpeza" da URL.

4.  **Iniciar a Aplicação:**
    No terminal, execute um dos seguintes comandos:

    ```bash
    # Usando o script definido no package.json (recomendado)
    npm start
    ```
    ou
    ```bash
    # Usando a CLI do Angular (se instalada globalmente)
    ng serve
    ```

5.  **Acesso:**
    A aplicação estará disponível em `http://localhost:4200`. Note que, ao rodar localmente desta forma, apenas o microfrontend será renderizado, sem o Shell (menus, cabeçalho, etc).

### 3.4. Testes

Os testes unitários são desenvolvidos com **Jest**. Os arquivos de teste possuem a extensão `.spec.ts` e ficam ao lado dos componentes e serviços que eles testam. O foco principal é testar a lógica de negócio contida nos arquivos TypeScript (`.ts`), e não a renderização de layout (HTML/CSS). A cobertura de código atual do projeto está em aproximadamente **88%**.

### 3.5. Dependências Críticas

* **Node.js:** v18.x (LTS)
* **Angular:** v17.x (verificar `package.json` para a versão exata)
* **@angular-architects/native-federation:** v17.x

---

## 4. Pontos de Atenção e Próximos Passos

Esta seção consolida os débitos técnicos e as melhorias futuras que foram discutidas durante a passagem de conhecimento. São pontos importantes a serem considerados no planejamento de futuras Sprints.

* **Refatoração de Rota do Frontend:** A rota atual do microfrontend de custódia (`movements/lista-movimentos/consulta-de-custodia`) está acoplada ao shell de Movimentos. É um ponto de melhoria prioritário refatorar esta configuração para que a rota seja uma raiz própria (ex: `/custodia`).

* **Criação de um Shell Global:** Existe a intenção de criar um "Shell de Plataforma" unificado que hospedará todos os microfrontends, incluindo o de Custódia. Essa iniciativa irá resolver a questão do acoplamento de rotas e centralizar a identidade visual e navegação da aplicação.

* **Extração da Lib Core do BFF:** Atualmente, o código comum do BFF reside no pacote `core` dentro do próprio projeto. Foi mencionado que há um plano para extrair este código para uma biblioteca (`.jar`) compartilhada, para que possa ser reaproveitado por outros BFFs.
